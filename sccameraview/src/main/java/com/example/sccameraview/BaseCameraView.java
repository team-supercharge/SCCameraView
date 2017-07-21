package com.example.sccameraview;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.CamcorderProfile;
import android.os.Environment;
import android.util.Log;
import android.view.TextureView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class BaseCameraView extends TextureView {

    static final int MEDIA_TYPE_IMAGE = 1;
    static final int MEDIA_TYPE_VIDEO = 2;
    static final int ORIENTATION_90 = 90;
    static final int ORIENTATION_270 = 270;
    static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
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
    OnImageSavedListener imageSavedListener;

    final SurfaceTextureListener surfaceTextureListener = new SurfaceTextureListener();

    public BaseCameraView(Context context) {
        super(context);
        frontFacingCameraActive = true;
    }

    public abstract void startPreview();

    public abstract void stopPreview();

    public abstract void startRecordingVideo();

    public abstract void stopRecordingVideo();

    abstract void openCamera();

    public abstract void switchCamera();

    public abstract void takePicture();

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

    public void setImageSavedListener(OnImageSavedListener imageSavedListener) {
        this.imageSavedListener = imageSavedListener;
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

    File getOutputMediaFile(int type){
        if (type == MEDIA_TYPE_IMAGE) {
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES).toString());

            if (createDirectoryForFile(mediaStorageDir)) {
                return new File(mediaStorageDir.getPath() + File.separator +
                        "IMG_"+ getTimeStamp() + ".jpg");
            }
        } else if(type == MEDIA_TYPE_VIDEO) {
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MOVIES).toString());

            if (createDirectoryForFile(mediaStorageDir)) {
                return new File(mediaStorageDir.getPath() + File.separator +
                        "VID_"+ getTimeStamp() + ".mp4");
            }
        }
        return null;
    }

    boolean createDirectoryForFile(File mediaStorageDir) {
        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                Log.e(LOG_TAG, "failed to create directory");
                return false;
            }
        }
        return true;
    }

    String getTimeStamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    }

    void saveImage(byte[] imageData) {
        //TODO: refactor filePath method
        SaveImageTask saveImageTask = new SaveImageTask(imageSavedListener, imageData, getOutputMediaFile(MEDIA_TYPE_IMAGE));
        saveImageTask.execute();
    }

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
            stopPreview();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // nothing to do
        }
    }
}
