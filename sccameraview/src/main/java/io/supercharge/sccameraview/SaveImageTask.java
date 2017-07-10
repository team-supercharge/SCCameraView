package io.supercharge.sccameraview;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static io.supercharge.sccameraview.BaseCameraView.LOG_TAG;

class SaveImageTask extends AsyncTask {

    private OnImageSavedListener imageSavedListener;
    private final byte[] imageData;
    private final File imageFile;

    SaveImageTask(OnImageSavedListener imageSavedListener, byte[] imageData, File imageFile) {
        this.imageSavedListener = imageSavedListener;
        this.imageData = imageData;
        this.imageFile = imageFile;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        FileOutputStream fos = null;
        try {
            if (imageFile == null) {
                Log.d(LOG_TAG, "Error creating media file, check storage permissions");
                return null;
            }
            if (imageData == null) {
                Log.d(LOG_TAG, "No image data");
            }

            try {
                //TODO: test rotation and orientation on multiple devices
                fos = new FileOutputStream(imageFile);
                fos.write(imageData);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(LOG_TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(LOG_TAG, "Error accessing file: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, "Error executing background task: " + e.getMessage());
        }
        return fos;
    }

    @Override
    protected void onPostExecute(Object o) {
        if (imageSavedListener != null) {
            imageSavedListener.onImageSaved();
        }
    }
}
