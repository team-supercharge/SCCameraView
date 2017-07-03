package com.example.sccameraview;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Size;
import android.view.Surface;

import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Based on https://github.com/googlesamples/android-Camera2Video
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2View extends BaseCameraView {

    private static final String[] VIDEO_PERMISSIONS = {
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
    };

    private static final long LOCK_TIMEOUT = 2500;

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;

    CameraManager cameraManager;

    private Size previewSize;
    private Size videoSize;

    private int sensorOrientation;

    private final StateCallback stateCallback = new StateCallback();

    private CameraDevice cameraDevice;
    private CameraCaptureSession previewSession;

    private CaptureRequest.Builder previewBuilder;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private final Semaphore cameraOpenCloseLock = new Semaphore(1);

    private MediaRecorder mediaRecorder;

    public Camera2View(Context context) {
        super(context);
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    public void startPreview() {
        startBackgroundThread();

        if (isAvailable()) {
            openCamera();
        } else {
            setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void stopPreview() {
        closeCamera();
        stopBackgroundThread();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread == null) {
            return;
        }

        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            Timber.e(e);
        }
    }

    @SuppressWarnings("MissingPermission")
    @Override
    public void openCamera() {
        final Activity activity = (Activity) getContext();

        if (null == activity || activity.isFinishing()) {
            return;
        }

        new RxPermissions(activity)
                .request(VIDEO_PERMISSIONS)
                .subscribe();

        try {

            if (!cameraOpenCloseLock.tryAcquire(LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            String cameraIdString = getFrontFacingCameraId();
            cameraId = Integer.parseInt(cameraIdString);

            // Choose the sizes for camera preview and video recording
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraIdString);
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            if (map == null) {
                throw new RuntimeException("Cannot get available preview/video sizes");
            }

            videoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    calculatedWidth, calculatedHeight, videoSize);

            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

            mediaRecorder = new MediaRecorder();

            cameraManager.openCamera(cameraIdString, stateCallback, null);
        } catch (CameraAccessException e) {
            Timber.e(e);
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            Timber.e(e);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }

    private String getFrontFacingCameraId() throws CameraAccessException {
        for (String cameraId : cameraManager.getCameraIdList()) {
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);

            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                    == CameraCharacteristics.LENS_FACING_FRONT) {
                return cameraId;
            }
        }

        return null;
    }

    private void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();
            closePreviewSession();
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != mediaRecorder) {
                mediaRecorder.release();
                mediaRecorder = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            cameraOpenCloseLock.release();
        }
    }

    private void startPreviewSession() {
        if (null == cameraDevice || !isAvailable() || null == previewSize) {
            return;
        }
        try {
            closePreviewSession();
            SurfaceTexture texture = getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            Surface previewSurface = new Surface(texture);
            previewBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            previewSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            // failed
                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            Timber.e(e);
        }
    }

    private void updatePreview() {
        if (null == cameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(previewBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            previewSession.setRepeatingRequest(previewBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            Timber.e(e);
        }
    }

    private void closePreviewSession() {
        if (previewSession != null) {
            previewSession.close();
            previewSession = null;
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    @Override
    public void startRecordingVideo() {
        if (null == cameraDevice || !isAvailable() || null == previewSize) {
            return;
        }
        try {
            closePreviewSession();
            setUpMediaRecorder();
            SurfaceTexture texture = getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            // Set up Surface for the camera preview
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            previewBuilder.addTarget(previewSurface);

            // Set up Surface for the MediaRecorder
            Surface recorderSurface = mediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            previewBuilder.addTarget(recorderSurface);

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    previewSession = cameraCaptureSession;
                    updatePreview();
                    post(() -> {
                        // Start recording
                        mediaRecorder.start();
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    // failed
                }
            }, backgroundHandler);

            recordingVideo = true;
        } catch (CameraAccessException | IOException e) {
            Timber.e(e);
        }

    }

    private void setUpMediaRecorder() throws IOException {
        final Activity activity = (Activity) getContext();
        if (null == activity) {
            return;
        }
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFile(videoFile.getAbsolutePath());

        if (sensorOrientation == SENSOR_ORIENTATION_DEFAULT_DEGREES) {
            mediaRecorder.setOrientationHint(ORIENTATION_90);
        } else {
            mediaRecorder.setOrientationHint(ORIENTATION_270);
        }

        CamcorderProfile profile = getCamcorderProfile();
        profile.videoFrameWidth = videoSize.getWidth();
        profile.videoFrameHeight = videoSize.getHeight();

        mediaRecorder.setProfile(profile);

        mediaRecorder.setVideoEncodingBitRate(BITRATE);

        mediaRecorder.prepare();
    }

    @Override
    public void stopRecordingVideo() {
        // Stop recording
        mediaRecorder.stop();
        mediaRecorder.reset();

        recordingVideo = false;
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w
                    && option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            return choices[0];
        }
    }

    private static Size chooseVideoSize(Size[] choices) {
        final double acceptTolerance = 0.1;

        for (Size size : choices) {
            if (Math.abs(size.getWidth() - size.getHeight() * ASPECT_RATIO) < acceptTolerance
                    && size.getWidth() <= MAX_RECORDING_WIDTH) {
                return size;
            }
        }

        return choices[choices.length - 1];
    }

    private static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight()
                    - (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    private class StateCallback extends CameraDevice.StateCallback {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            startPreviewSession();
            cameraOpenCloseLock.release();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraOpenCloseLock.release();
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraOpenCloseLock.release();
            camera.close();
            cameraDevice = null;
        }
    }

}
