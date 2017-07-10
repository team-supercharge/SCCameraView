package io.supercharge.sccameraview;


import android.content.Context;
import android.os.AsyncTask;

import java.util.Collections;
import java.util.Comparator;

public class LoadAspectRatiosTask extends AsyncTask {

    Context context;
    BaseCameraView cameraView;

    LoadAspectRatiosTask(Context context, BaseCameraView cameraView) {
        this.context = context;
        this.cameraView = cameraView;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        cameraView.collectRatioSizes();

        if (!cameraView.getRatioSizeList().isEmpty()) {
            Collections.sort(cameraView.ratioSizeList, new Comparator<AspectRatio>() {
                @Override
                public int compare(AspectRatio p1, AspectRatio p2) {
                    return p1.getRatio() < p2.getRatio() ? -1 : 1;
                }
            });
        }

        for (int i = 0; i < cameraView.getRatioSizeList().size(); i++) {
            if (cameraView.getRatioSizeList().get(i).getRatio() == BaseCameraView.ASPECT_RATIO)
                cameraView.setSelectedRatioIdx(i);
        }

        return cameraView.getRatioSizeList();
    }

    @Override
    protected void onPostExecute(Object list) {
        cameraView.changeAspectRatio(cameraView.getSelectedRatioIdx());
        cameraView.requestParentLayout();
    }
}
