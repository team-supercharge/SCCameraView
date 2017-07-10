package com.example.sccameraview;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.CamcorderProfile;
import android.view.TextureView;

import java.io.File;

public abstract class BaseCameraView extends TextureView {

    static final int ORIENTATION_90 = 90;
    static final int ORIENTATION_270 = 270;
    static final int BITRATE = 2500000;
    static final float ASPECT_RATIO = 4f / 3f;
    static final int MAX_RECORDING_WIDTH = 720;
    static final String LOG_TAG = "SCCameraView";

    int cameraId;
    boolean recordingVideo;
    File videoFile;
    int calculatedWidth;
    int calculatedHeight;
    protected boolean frontFacingCameraActive;

    final SurfaceTextureListener surfaceTextureListener = new SurfaceTextureListener();

    public BaseCameraView(Context context) {
        super(context);
        frontFacingCameraActive = true;
    }

    public abstract void startPreview();

    public abstract void stopPreview();

    public abstract void startRecordingVideo();

    public abstract void stopRecordingVideo();

    public boolean isRecordingVideo() {
        return recordingVideo;
    }

    public void setVideoFile(File videoFile) {
        this.videoFile = videoFile;
    }

    public void setCalculatedWidth(int calculatedWidth) {
        this.calculatedWidth = calculatedWidth;
    }

    public void setCalculatedHeight(int calculatedHeight) {
        this.calculatedHeight = calculatedHeight;
    }

    public void setFrontFacingCameraActive(boolean frontFacingCameraActive) {
        this.frontFacingCameraActive = frontFacingCameraActive;
    }

    CamcorderProfile getCamcorderProfile() {
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_HIGH)) {
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
        }
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P);
        }
        return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
    }

    abstract void openCamera();

    public abstract void switchCamera();

    private class SurfaceTextureListener implements TextureView.SurfaceTextureListener {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            startPreview();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // nothing to do
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // nothing to do
        }
    }
}
