package io.supercharge.sccameraview;

import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Size;

import java.io.Serializable;


public class AspectRatio implements Serializable {

    private double ratio;
    private int width;
    private int height;

    AspectRatio(double ratio, int width, int height) {
        this.ratio = ratio;
        this.width = width;
        this.height = height;
    }

    public double getRatio() {
        return ratio;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Size getSize() {
        return new Size(width, height);
    }

    public Camera.Size getSize(Camera camera) {
        return camera.new Size(width, height);
    }
}
