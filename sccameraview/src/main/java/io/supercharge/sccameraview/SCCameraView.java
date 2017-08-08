package io.supercharge.sccameraview;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class SCCameraView extends FrameLayout {

    private BaseCameraView cameraView;
    private AspectRatio aspectRatio;
    private int width = getMeasuredWidth();
    private int height = getMeasuredHeight();

    public SCCameraView(Context context) {
        this(context, null);
    }

    public SCCameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SCCameraView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (isInEditMode()){
            cameraView = null;
            return;
        }

        cameraView = BaseCameraView.createCameraView(context);
        cameraView.loadAspectRatios();
        this.addView(cameraView);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isInEditMode() || cameraView.getRatioSizeList().isEmpty()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            if (width == 0 && height == 0) {
                width = getMeasuredWidth();
                height = getMeasuredHeight();
                cameraView.setAspectRatioByScreenSize((double)height/width);
            }
            aspectRatio = cameraView.getRatioSizeList().get(cameraView.getSelectedRatioIdx());
            final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            int measureWidth = Math.min(width, (int) (height / aspectRatio.getRatio()));
            int measureHeight = Math.min(height, (int) (width * aspectRatio.getRatio()));
            double viewRatio = (double)measureHeight / (double)measureWidth;

            if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(measureHeight, MeasureSpec.EXACTLY));
            } else if (widthMode != MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
                super.onMeasure(MeasureSpec.makeMeasureSpec(measureWidth, MeasureSpec.EXACTLY), heightMeasureSpec);
            } else if (widthMode != MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
                super.onMeasure(MeasureSpec.makeMeasureSpec(measureWidth, MeasureSpec.EXACTLY),
                                MeasureSpec.makeMeasureSpec(measureHeight, MeasureSpec.EXACTLY));
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }

            if (aspectRatio.getRatio() < viewRatio) {
                cameraView.measure(
                        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec((int) (width * aspectRatio.getRatio()), MeasureSpec.EXACTLY));
            } else {
                cameraView.measure(
                        MeasureSpec.makeMeasureSpec((int) (height / aspectRatio.getRatio()), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            }
        }
    }

    public BaseCameraView getCameraView() {
        return cameraView;
    }
}
