package com.firefishcomms.instagramlikegallery.adapters.model;

import android.graphics.Bitmap;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

public class ThumbnailItem {

    public Bitmap filteredImage;
    public GPUImageFilter filter;

    public ThumbnailItem(GPUImageFilter filter) {
        this.filter = filter;
    }
}
