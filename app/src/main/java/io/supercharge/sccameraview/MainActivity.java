package io.supercharge.sccameraview;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.example.sccameraview.BaseCameraView;
import com.example.sccameraview.Camera1View;
import com.example.sccameraview.Camera2View;

public class MainActivity extends AppCompatActivity {

    private FrameLayout videoContainer;
    private BaseCameraView cameraView;
    private Button switchButton;
    private Button recordButton;
    private Button stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();

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
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraView.stopPreview();
            }
        });
        cameraView.startPreview();
    }

    private void bindViews() {
        videoContainer = (FrameLayout) findViewById(R.id.video_container);
        cameraView = createCameraView();
        switchButton = (Button) findViewById(R.id.camera_switch_btn);
        recordButton = (Button) findViewById(R.id.camera_record_btn);
        stopButton = (Button) findViewById(R.id.camera_stop_btn);
    }

    private BaseCameraView createCameraView() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return new Camera1View(this);
        }
        return new Camera2View(this);
    }
}
