package io.supercharge.sccameraviewdemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import io.supercharge.sccameraview.BaseCameraView;
import io.supercharge.sccameraview.OnImageSavedListener;
import io.supercharge.sccameraview.SCCameraView;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;

    private SCCameraView scCameraView;
    private BaseCameraView cameraView;
    private ImageView switchButton;
    private ImageView recordButton;
    private ImageView takePictureButton;
    private ImageView aspectRatioButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();

        if (isEveryPermissionGranted()) {
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
        scCameraView = (SCCameraView) findViewById(R.id.video_container);
        switchButton = (ImageView) findViewById(R.id.camera_switch_btn);
        recordButton = (ImageView) findViewById(R.id.camera_record_btn);
        takePictureButton = (ImageView) findViewById(R.id.camera_take_picture_btn);
        aspectRatioButton = (ImageView) findViewById(R.id.camera_aspect_ratio_btn);
    }

    private void initApplication() {
        cameraView = scCameraView.getCameraView();
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
        aspectRatioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AspectRatioAdapter adapter = new AspectRatioAdapter(MainActivity.this, android.R.layout.simple_list_item_single_choice, cameraView);
                adapter.showDialog();
            }
        });

        cameraView.setImageSavedListener(new OnImageSavedListener() {
            @Override
            public void onImageSaved() {
                cameraView.startPreview();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initApplication();
                }
            }
        }
    }
}
