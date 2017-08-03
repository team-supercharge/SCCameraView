package io.supercharge.sccameraviewdemo;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import io.supercharge.sccameraview.AspectRatio;
import io.supercharge.sccameraview.BaseCameraView;

public class AspectRatioAdapter extends BaseAdapter {

    private final Context context;
    private final BaseCameraView cameraView;
    private final int layoutResource;

    public AspectRatioAdapter(Context context, @LayoutRes int layoutResource, BaseCameraView cameraView) {
        this.context = context;
        this.layoutResource = layoutResource;
        this.cameraView = cameraView;
    }

    public String getFormattedRatioString(AspectRatio aspectRatio) {
        String ratioString = aspectRatio.getWidth() + "x" + aspectRatio.getHeight();
        int greatestCommonDivisor = getGreatestCommonDivisor(aspectRatio.getWidth(), aspectRatio.getHeight());
        ratioString += "\t\t\t" + aspectRatio.getWidth() / greatestCommonDivisor + " : " + aspectRatio.getHeight() / greatestCommonDivisor;
        return ratioString;
    }

    private int getGreatestCommonDivisor(int a, int b) {
        if (b == 0) {
            return Math.abs(a);
        }
        return getGreatestCommonDivisor(b, a % b);
    }

    public void showDialog() {
        final int checkedItem = cameraView.getSelectedRatioIdx();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.dialog_title));

        builder.setSingleChoiceItems(this, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cameraView.changeAspectRatio(which);
                if (which != checkedItem) {
                    cameraView.setSelectedRatioIdx(which);
                    cameraView.requestParentLayout();
                }
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        dialog.show();
    }

    @Override
    public int getCount() {
        return cameraView.getRatioSizeList().size();
    }

    @Override
    public AspectRatio getItem(int i) {
        return cameraView.getRatioSizeList().get(i);
    }

    @Override
    public long getItemId(int i) {
        return getItem(i).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AspectRatioAdapter.ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(layoutResource, parent, false);
            viewHolder = new AspectRatioAdapter.ViewHolder();
            viewHolder.text = (TextView) convertView.findViewById(android.R.id.text1);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (AspectRatioAdapter.ViewHolder) convertView.getTag();
        }

        AspectRatio item = getItem(position);
        viewHolder.text.setText(getFormattedRatioString(item));
        return convertView;
    }

    private static class ViewHolder {
        TextView text;
    }
}
