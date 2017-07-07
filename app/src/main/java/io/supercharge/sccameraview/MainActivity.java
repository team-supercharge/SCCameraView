package io.supercharge.sccameraview;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.example.sccameraview.BaseCameraView;
import com.example.sccameraview.Camera1View;
import com.example.sccameraview.Camera2View;
import com.example.sccameraview.OnImageSavedListener;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;

    private FrameLayout videoContainer;
    private BaseCameraView cameraView;
    private Button switchButton;
    private Button recordButton;
    private Button takePictureButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();

        if (isEveryPermissionGranted()) {
            cameraView = createCameraView();
            initApplication();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private boolean isEveryPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void bindViews() {
        videoContainer = (FrameLayout) findViewById(R.id.video_container);
        switchButton = (Button) findViewById(R.id.camera_switch_btn);
        recordButton = (Button) findViewById(R.id.camera_record_btn);
        takePictureButton = (Button) findViewById(R.id.camera_take_picture_btn);
    }

    private void initApplication() {
        videoContainer.addView(cameraView);
        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraView.switchCamera();
            }
        });
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cameraView.isRecordingVideo()) {
                    cameraView.stopRecordingVideo();
                    cameraView.startPreview();
                } else {
                    cameraView.startRecordingVideo();
                }
            }
        });
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraView.takePicture();
            }
        });
        cameraView.setImageSavedListener(new OnImageSavedListener() {
            @Override
            public void onImageSaved() {
                cameraView.startPreview();
            }
        });
        
        cameraView.startPreview();
    }

    private BaseCameraView createCameraView() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Camera1View camera1View = new Camera1View(this);
            camera1View.setCameraFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera1View.setCameraFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

            return camera1View;
        } else {
            Camera2View camera2View = new Camera2View(this);
            camera2View.setCameraAutoFocusMode(CaptureRequest.CONTROL_AF_MODE_AUTO);
            camera2View.setCameraFlashMode(CaptureRequest.FLASH_MODE_OFF);

            return camera2View;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraView = createCameraView();
                    initApplication();
                }
            }
        }
    }
}
