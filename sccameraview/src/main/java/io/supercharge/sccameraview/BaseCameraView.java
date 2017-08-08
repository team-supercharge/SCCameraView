package io.supercharge.sccameraview;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CaptureRequest;
import android.media.CamcorderProfile;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.TextureView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class BaseCameraView extends TextureView {

    static final int MEDIA_TYPE_IMAGE = 1;
    static final int MEDIA_TYPE_VIDEO = 2;
    static final int ORIENTATION_90 = 90;
    static final int ORIENTATION_270 = 270;
    static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    static final int BITRATE = 2500000;
    public static double ASPECT_RATIO = 1.0;
    static final String LOG_TAG = "SCCameraView";

    protected int selectedRatioIdx;
    int cameraId;
    boolean recordingVideo;
    File videoFile;
    boolean frontFacingCameraActive;
    OnImageSavedListener imageSavedListener;
    List<AspectRatio> ratioSizeList;

    final SurfaceTextureListener surfaceTextureListener = new SurfaceTextureListener();

    BaseCameraView(Context context) {
        super(context);
        frontFacingCameraActive = true;
        ratioSizeList = new ArrayList<>();
    }

    public static BaseCameraView createCameraView(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Camera1View camera1View = new Camera1View(context);
            camera1View.setCameraFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera1View.setCameraFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

            return camera1View;
        } else {
            Camera2View camera2View = new Camera2View(context);
            camera2View.setCameraFlashMode(CaptureRequest.FLASH_MODE_OFF);
            camera2View.setCameraAutoFocusMode(CaptureRequest.CONTROL_AF_MODE_AUTO);

            return camera2View;
        }
    }

    public abstract void startPreview();

    public abstract void stopPreview();

    public abstract void startRecordingVideo();

    public abstract void stopRecordingVideo();

    abstract void openCamera();

    public abstract void switchCamera();

    public abstract void takePicture();

    public abstract void changeAspectRatio(int position);

    public abstract void collectRatioSizes();

    public boolean isRecordingVideo() {
        return recordingVideo;
    }

    public void setVideoFile(File videoFile) {
        this.videoFile = videoFile;
    }

    public void setImageSavedListener(OnImageSavedListener imageSavedListener) {
        this.imageSavedListener = imageSavedListener;
    }

    public List<AspectRatio> getRatioSizeList() {
        return ratioSizeList;
    }

    public int getSelectedRatioIdx() {
        return selectedRatioIdx;
    }

    public void setSelectedRatioIdx(int selectedRatioIdx) {
        this.selectedRatioIdx = selectedRatioIdx;
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

    public void loadAspectRatios() {
        LoadAspectRatiosTask loadAspectRatiosTask = new LoadAspectRatiosTask(getContext(), this);
        loadAspectRatiosTask.execute();
    }

    public void requestParentLayout() {
        stopPreview();
        getParent().requestLayout();
        startPreview();
    }

    public void setAspectRatioByScreenSize(double screenRatio) {
        double minDifference = Double.MAX_VALUE;
        int minIdx = 0;
        for (int i = 0; i < ratioSizeList.size(); i++) {
            double difference = Math.abs(ratioSizeList.get(i).getRatio() - screenRatio);
            if (difference < minDifference) {
                minDifference = difference;
                minIdx = i;
            }
        }
        ASPECT_RATIO = ratioSizeList.get(minIdx).getRatio();
        setSelectedRatioIdx(minIdx);
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
