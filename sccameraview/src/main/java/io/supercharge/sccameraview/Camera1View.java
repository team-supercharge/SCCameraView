package io.supercharge.sccameraview;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Based on https://github.com/googlesamples/android-MediaRecorder
 */
public class Camera1View extends BaseCameraView {

    protected Camera camera;
    private MediaRecorder mediaRecorder;
    private Camera.Size videoSize;
    private String cameraFlashMode;
    private String cameraFocusMode;

    public Camera1View(Context context) {
        super(context);
    }

    @Override
    public void startPreview() {
        if (isAvailable()) {
            openCamera();
        } else {
            setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void stopPreview() {
        releaseMediaRecorder();
        releaseCamera();
    }

    @Override
    void openCamera() {
        cameraId = getDefaultCameraId();
        camera = Camera.open(cameraId);

        Camera.Parameters parameters = camera.getParameters();
        Camera.Size previewSize = ratioSizeList.get(selectedRatioIdx).getSize(camera);
        videoSize = ratioSizeList.get(selectedRatioIdx).getSize(camera);
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        parameters.setPictureSize(previewSize.width, previewSize.height);

        if (parameters.getSupportedFlashModes() != null &&
                parameters.getSupportedFlashModes().contains(cameraFlashMode)) {
            parameters.setFlashMode(cameraFlashMode);
        }
        if (parameters.getSupportedFocusModes() != null &&
                parameters.getSupportedFocusModes().contains(cameraFocusMode)) {
            parameters.setFocusMode(cameraFocusMode);
        }

        parameters.setRotation(ORIENTATION_270);
        camera.setDisplayOrientation(ORIENTATION_90);
        camera.setParameters(parameters);
        try {
            camera.setPreviewTexture(getSurfaceTexture());
            camera.startPreview();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    @Override
    public void takePicture() {
        camera.takePicture(null, null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] imageData, Camera camera) {
                releaseCamera();
                saveImage(imageData);
            }
        });
    }

    @Override
    public void changeAspectRatio(int position) {
        if(!ratioSizeList.isEmpty()) {
            ASPECT_RATIO = ratioSizeList.get(position).getRatio();
        }
    }

    public void switchCamera() {
        stopPreview();
        frontFacingCameraActive = !frontFacingCameraActive;
        ratioSizeList = new ArrayList<>();
        loadAspectRatios();
    }

    protected void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            // clear recorder configuration
            mediaRecorder.reset();
            // release the recorder object
            mediaRecorder.release();
            mediaRecorder = null;

            if (camera != null) {
                // Lock camera for later use i.e taking it back from MediaRecorder.
                // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
                camera.lock();
            }
        }
    }

    @Override
    public void startRecordingVideo() {
        if (prepareVideoRecorder()) {
            mediaRecorder.start();
            recordingVideo = true;
        } else {
            releaseMediaRecorder();
        }
    }

    private boolean prepareVideoRecorder() {
        mediaRecorder = new MediaRecorder();

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        if (info.orientation == SENSOR_ORIENTATION_DEFAULT_DEGREES) {
            mediaRecorder.setOrientationHint(ORIENTATION_90);
        } else {
            mediaRecorder.setOrientationHint(ORIENTATION_270);
        }

        // Step 1: Unlock and set camera to MediaRecorder
        camera.unlock();
        mediaRecorder.setCamera(camera);

        // Step 2: Set sources
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        CamcorderProfile profile = getCamcorderProfile();
        profile.videoFrameWidth = videoSize.width;
        profile.videoFrameHeight = videoSize.height;

        mediaRecorder.setProfile(profile);
        mediaRecorder.setVideoEncodingBitRate(BITRATE);

        // Step 4: Set output file
        mediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

        // Step 5: Prepare configured MediaRecorder
        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.e(LOG_TAG, e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    @SuppressWarnings("checkstyle:illegalcatch")
    @Override
    public void stopRecordingVideo() {
        try {
            mediaRecorder.stop();  // stop the recording
        } catch (RuntimeException e) {
            // RuntimeException is thrown when stop() is called immediately after start().
            // In this case the output file is not properly constructed ans should be deleted.
            Log.e(LOG_TAG, e.getMessage());
        }
        releaseMediaRecorder(); // release the MediaRecorder object
        camera.lock();         // take camera access back from MediaRecorder
        releaseCamera();
        recordingVideo = false;
        startPreview();
    }

    private int getDefaultCameraId() {
        int position = frontFacingCameraActive ?  Camera.CameraInfo.CAMERA_FACING_FRONT
                : Camera.CameraInfo.CAMERA_FACING_BACK;
        // Find the total number of cameras available
        int numberOfCameras = Camera.getNumberOfCameras();

        // Find the ID of the back-facing ("default") camera
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == position) {
                return position;
            }
        }

        return -1;
    }

    private Camera.Size chooseSize(List<Camera.Size> choices) {
        final double acceptTolerance = 0.1;

        for (Camera.Size option : choices) {
            if (Math.abs(ASPECT_RATIO - (double) option.width / (double) option.height) < acceptTolerance) {
                return option;
            }
        }
        return choices.get(0);
    }

    @Override
    public void collectRatioSizes() {
        ratioSizeList.clear();
        camera = Camera.open(getDefaultCameraId());
        List<Camera.Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();

        if (previewSizes != null) {
            List<Double> ratioList = new ArrayList<>();
            for (Camera.Size size : previewSizes) {
                double ratio = (double) size.width / (double) size.height;
                if (!ratioList.contains(ratio)) {
                    ratioList.add(ratio);
                    ratioSizeList.add(new AspectRatio(ratio, size.width, size.height));
                }
            }
        }
    }

    public void setCameraFlashMode(String cameraFlashMode) {
        this.cameraFlashMode = cameraFlashMode;
    }

    public void setCameraFocusMode(String cameraFocusMode) {
        this.cameraFocusMode = cameraFocusMode;
    }
}
