package com.firefishcomms.instagramlikegallery.adapters.model;

import java.io.File;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

public class SelectedImagesItem {

    public File imageFile;
    public GPUImageFilter filter;

    public SelectedImagesItem(File imageFile) {
        this.imageFile = imageFile;
        this.filter = new GPUImageFilter();
    }
}
