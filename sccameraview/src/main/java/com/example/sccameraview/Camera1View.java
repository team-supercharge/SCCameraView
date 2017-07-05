package com.example.sccameraview;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.util.List;

/**
 * Based on https://github.com/googlesamples/android-MediaRecorder
 */
public class Camera1View extends BaseCameraView {

    private Camera camera;

    private MediaRecorder mediaRecorder;

    private Camera.Size videoSize;

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
        // if we are using MediaRecorder, release it first
        releaseMediaRecorder();
        // release the camera immediately on pause event
        releaseCamera();
    }

    @Override
    void openCamera() {
        cameraId = getDefaultFrontFacingCameraId();
        camera = Camera.open(cameraId);

        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> supportedVideoSizes = parameters.getSupportedVideoSizes();
        Camera.Size previewSize = getOptimalPreviewSize(supportedVideoSizes, supportedPreviewSizes,
                calculatedWidth, calculatedHeight);
        videoSize = chooseVideoSize(supportedVideoSizes);

        parameters.setPreviewSize(previewSize.width, previewSize.height);

        camera.setDisplayOrientation(ORIENTATION_90);

        camera.setParameters(parameters);

        try {
            camera.setPreviewTexture(getSurfaceTexture());
            camera.startPreview();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    public void switchCamera() {
        frontFacingCameraActive = !frontFacingCameraActive;
        releaseCamera();
        openCamera();
    }

    private void releaseCamera() {
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
        mediaRecorder.setOrientationHint(ORIENTATION_270);

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
        mediaRecorder.setOutputFile(videoFile.getAbsolutePath());

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
    }

    private static int getDefaultFrontFacingCameraId() {
        return getDefaultCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);
    }

    private static int getDefaultCameraId(int position) {
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

    private static Camera.Size getOptimalPreviewSize(List<Camera.Size> supportedVideoSizes,
                                                     List<Camera.Size> previewSizes, int w, int h) {
        // Use a very small tolerance because we want an exact match.
        final double acceptTolerance = 0.1;

        // Supported video sizes list might be null, it means that we are allowed to use the preview
        // sizes
        List<Camera.Size> videoSizes;
        if (supportedVideoSizes != null) {
            videoSizes = supportedVideoSizes;
        } else {
            videoSizes = previewSizes;
        }
        Camera.Size optimalSize = null;

        // Start with max value and refine as we iterate over available video sizes. This is the
        // minimum difference between view and camera height.
        double minDiff = Double.MAX_VALUE;

        // Target view height
        int targetHeight = h;

        // Try to find a video size that matches aspect ratio and the target view size.
        // Iterate over all available sizes and pick the largest size that can fit in the view and
        // still maintain the aspect ratio.
        for (Camera.Size size : videoSizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - ASPECT_RATIO) > acceptTolerance) {
                continue;
            }

            if (Math.abs(size.height - targetHeight) < minDiff && previewSizes.contains(size)) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find video size that matches the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : videoSizes) {
                if (Math.abs(size.height - targetHeight) < minDiff && previewSizes.contains(size)) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    private static Camera.Size chooseVideoSize(List<Camera.Size> supportedVideoSizes) {
        final double acceptTolerance = 0.1;

        for (Camera.Size size : supportedVideoSizes) {
            if (Math.abs(size.width - size.height * ASPECT_RATIO) < acceptTolerance
                    && size.width <= MAX_RECORDING_WIDTH) {
                return size;
            }
        }

        return supportedVideoSizes.get(supportedVideoSizes.size() - 1);
    }
}
