package com.firefishcomms.instagramlikegallery.adapters;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.firefishcomms.instagramlikegallery.activities.BaseActivity;
import com.firefishcomms.instagramlikegallery.services.ImageGalleryService;

import java.util.List;

/**
 * Created by anton on 8/12/2017.
 */

public class GalleryDirectorySpinnerAdapter extends ArrayAdapter<ImageGalleryService.Folder> {

    // Your sent context
    private BaseActivity activity;
    // Your custom values for the spinner (User)
    private List<ImageGalleryService.Folder> data;

    public GalleryDirectorySpinnerAdapter(BaseActivity a, int textViewResourceId,
                       List<ImageGalleryService.Folder> d) {
        super(a, textViewResourceId, d);
        this.activity = a;
        this.data = d;
    }

    @Override
    public int getCount(){
        return data.size();
    }

    @Override
    public ImageGalleryService.Folder getItem(int position){
        return data.get(position);
    }

    @Override
    public long getItemId(int position){
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView label = (TextView) super.getView(position, convertView, parent);
        label.setTextColor(Color.BLACK);
        label.setTypeface(null, Typeface.BOLD);
        label.setText(data.get(position).name);
        return label;
    }

    @Override
    public View getDropDownView(int position, View convertView,
                                ViewGroup parent) {
        TextView label = (TextView) super.getDropDownView(position, convertView, parent);
        label.setTextColor(Color.BLACK);
        label.setText(data.get(position).name);
        label.setBackgroundColor(Color.WHITE);
        return label;
    }
}