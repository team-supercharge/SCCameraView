package com.example.sccameraview;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.media.CamcorderProfile;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.TextureView;

import java.io.File;

public abstract class BaseCameraView extends TextureView {

    static final int ORIENTATION_90 = 90;
    static final int ORIENTATION_270 = 270;
    static final int BITRATE = 2500000;
    static final float ASPECT_RATIO = 4f / 3f;
    static final int MAX_RECORDING_WIDTH = 720;
    static final String LOG_TAG = "SCCameraView";
    private static final int PERMISSION_REQUEST_CODE = 1;

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

    private class SurfaceTextureListener implements TextureView.SurfaceTextureListener, ActivityCompat.OnRequestPermissionsResultCallback {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            if (!(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(((Activity) getContext()),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                        PERMISSION_REQUEST_CODE);
            } else {
                startPreview();
            }
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

        @Override
        public void onRequestPermissionsResult(int requestCode,
                                               String permissions[], int[] grantResults) {
            switch (requestCode) {
                case PERMISSION_REQUEST_CODE: {
                    if (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        startPreview();
                    }
                }
            }
        }
    }
}
