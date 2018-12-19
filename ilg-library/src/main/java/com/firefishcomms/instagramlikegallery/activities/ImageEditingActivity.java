package com.firefishcomms.instagramlikegallery.activities;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.firefishcomms.instagramlikegallery.R;
import com.firefishcomms.instagramlikegallery.adapters.FilterThumbnailsAdapter;
import com.firefishcomms.instagramlikegallery.adapters.model.ThumbnailItem;
import com.firefishcomms.instagramlikegallery.commons.ILGConstants;
import com.firefishcomms.instagramlikegallery.commons.ILGRequestCode;
import com.firefishcomms.instagramlikegallery.commons.ILGResultCode;
import com.firefishcomms.instagramlikegallery.fragments.CameraFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.filter.*;

public class ImageEditingActivity extends BaseActivity {

    public final static List<GPUImageFilter> IMAGE_FILTERS = new ArrayList<>();
    private static final List<ThumbnailItem> filterList = new ArrayList<>();
    static{
        IMAGE_FILTERS.add(new GPUImageFilter());
        IMAGE_FILTERS.add(new GPUImageContrastFilter());
        IMAGE_FILTERS.add(new GPUImageColorInvertFilter());
        IMAGE_FILTERS.add(new GPUImageHueFilter());
        IMAGE_FILTERS.add(new GPUImageGammaFilter());
        IMAGE_FILTERS.add(new GPUImageBrightnessFilter(0.3f));
        IMAGE_FILTERS.add(new GPUImageSepiaToneFilter());
        IMAGE_FILTERS.add(new GPUImageGrayscaleFilter());
        IMAGE_FILTERS.add(new GPUImageSharpenFilter(0.8f));
        IMAGE_FILTERS.add(new GPUImageSobelEdgeDetectionFilter());
        IMAGE_FILTERS.add(new GPUImageEmbossFilter());
        IMAGE_FILTERS.add(new GPUImagePosterizeFilter());
        IMAGE_FILTERS.add(new GPUImageSaturationFilter(1.5f));
        IMAGE_FILTERS.add(new GPUImageExposureFilter());
        IMAGE_FILTERS.add(new GPUImageHighlightShadowFilter(0.3f, 0.8f));
        IMAGE_FILTERS.add(new GPUImageMonochromeFilter());
        IMAGE_FILTERS.add(new GPUImageWhiteBalanceFilter(8000f, 0.5f));
        IMAGE_FILTERS.add(new GPUImageVibranceFilter(0.5f));
        IMAGE_FILTERS.add(new GPUImageGaussianBlurFilter());
        IMAGE_FILTERS.add(new GPUImageCrosshatchFilter());
        IMAGE_FILTERS.add(new GPUImageBoxBlurFilter());
        IMAGE_FILTERS.add(new GPUImageCGAColorspaceFilter());
        IMAGE_FILTERS.add(new GPUImageDilationFilter());
        IMAGE_FILTERS.add(new GPUImageKuwaharaFilter());
        IMAGE_FILTERS.add(new GPUImageRGBDilationFilter());
        IMAGE_FILTERS.add(new GPUImageSketchFilter());
        IMAGE_FILTERS.add(new GPUImageToonFilter());
        IMAGE_FILTERS.add(new GPUImageSmoothToonFilter());
        IMAGE_FILTERS.add(new GPUImageHalftoneFilter());
        IMAGE_FILTERS.add(new GPUImageBulgeDistortionFilter());
        IMAGE_FILTERS.add(new GPUImageGlassSphereFilter());
        IMAGE_FILTERS.add(new GPUImageHazeFilter());
        IMAGE_FILTERS.add(new GPUImageFalseColorFilter());

        for(GPUImageFilter filter : IMAGE_FILTERS){
            filterList.add(new ThumbnailItem(filter));
        }
    }

    private View layout_top_section;
    private ImageView iv_image_editing_original_picture;
    private ImageView iv_image_editing_picture;
    private RecyclerView rv_image_editing_filters;
    private Button btn_toolbar_camera_gallery_next;

    private File selectedImageFile;

    private Bitmap selectedImageOriginal;
    private Bitmap selectedImagePreview;
    private Bitmap selectedImageThumbnail;

    private FilterThumbnailsAdapter filterThumbnailsAdapter;
    private GPUImage gpuImage;

    @SuppressLint({"StaticFieldLeak", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_image_editing);
        initialiseUIElements();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        gpuImage = new GPUImage(this);

        selectedImageFile = (File) getIntent().getSerializableExtra(ILGConstants.EXTRA_SELECTED_IMAGE_FILE); //new File(getIntent().getStringExtra(EXTRA_SELECTED_IMAGE_FILE));

        if(selectedImageFile == null || !selectedImageFile.isFile() || selectedImageFile.length() <= 0) {
            //TODO: use snackbar instead
            Toast.makeText(getApplicationContext(), "Invalid image file to edit.", Toast.LENGTH_LONG).show();

            finish();
            return;
        }

        showLoadingSpinner(Gravity.CENTER);

        btn_toolbar_camera_gallery_next.setEnabled(false);
        new AsyncTask<File, Void, Void>(){

            @Override
            protected Void doInBackground(File... files) {

                FileInputStream fileInputStream = null;
                try {
                    fileInputStream = new FileInputStream(files[0]);

                    selectedImageOriginal = BitmapFactory.decodeStream(fileInputStream);

                    float scaledWidthForPreview = Resources.getSystem().getDisplayMetrics().widthPixels;
                    float scaledHeightForPreview = scaledWidthForPreview / ((float) selectedImageOriginal.getWidth()/(float) selectedImageOriginal.getHeight());
                    float scaledWidthForThumbnail = scaledWidthForPreview / 5;
                    scaledWidthForThumbnail = (scaledWidthForThumbnail <= 150)? scaledWidthForThumbnail : 150;
                    float scaledHeightForThumbnail = scaledWidthForThumbnail / ((float) selectedImageOriginal.getWidth()/(float) selectedImageOriginal.getHeight());

                    selectedImagePreview = Bitmap.createScaledBitmap(selectedImageOriginal, (int) scaledWidthForPreview, (int) scaledHeightForPreview, false);
                    selectedImageThumbnail = Bitmap.createScaledBitmap(selectedImageOriginal, (int) scaledWidthForThumbnail, (int) scaledHeightForThumbnail, false);

                    gpuImage.setImage(selectedImagePreview);
                    gpuImage.setFilter(new GPUImageFilter());

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally{
                    try {
                        fileInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                bindDataToAdapter();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        iv_image_editing_original_picture.setImageBitmap(gpuImage.getBitmapWithFilterApplied());
                        iv_image_editing_picture.setImageBitmap(gpuImage.getBitmapWithFilterApplied());

                        btn_toolbar_camera_gallery_next.setEnabled(true);
                        dismissLoadingSpinner();
                    }
                });

                return null;
            }
        }.execute(selectedImageFile);

        iv_image_editing_picture.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:{
                        iv_image_editing_picture.setAlpha(0f);
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                    {
                        iv_image_editing_picture.setAlpha(1f);
                        return true;
                    }
                }
                return false;
            }
        });

        btn_toolbar_camera_gallery_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btn_toolbar_camera_gallery_next.setEnabled(false);
                showLoadingSpinner(Gravity.CENTER);

                GPUImageFilter selectedFilter = filterThumbnailsAdapter.getSelectedFilter();

                int requestCode = getIntent().getIntExtra(ILGConstants.EXTRA_ACTIVITY_REQUEST_CODE, -1);

                if(requestCode == ILGRequestCode.IMAGE_EDIT_GET_FILTER) {
                    int selectedFilterIndex = IMAGE_FILTERS.indexOf(selectedFilter);
                    getIntent().putExtra(ILGConstants.EXTRA_SELECTED_IMAGE_FILTER_INDEX, (selectedFilterIndex == -1)? 0 : selectedFilterIndex);
                    setResult(ILGResultCode.RESULT_OK, getIntent());
                    finish();
                    return;
                } else{
                    String selectedFilterClassName = selectedFilter.getClass().getSimpleName();
                    if (selectedFilterClassName.equals(GPUImageFilter.class.getSimpleName()) && !selectedImageFile.getName().equals(CameraFragment.TEMP_IMAGE_FILE_NAME)) {
                        getIntent().putExtra(ILGConstants.EXTRA_FILTERED_IMAGE_FILE, selectedImageFile);
                        setResult(ILGResultCode.RESULT_OK, getIntent());
                        finish();
                        return;
                    } else {
                        final String fileName = String.format("%d.jpg", System.currentTimeMillis());
                        gpuImage.setFilter(selectedFilter);
                        gpuImage.setImage(selectedImageFile);
                        gpuImage.saveToPictures("", fileName,
                                new GPUImage.OnPictureSavedListener() {
                                    @Override
                                    public void onPictureSaved(Uri uri) {
                                        getIntent().putExtra(ILGConstants.EXTRA_FILTERED_IMAGE_FILE,
                                                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fileName));
                                        setResult(ILGResultCode.RESULT_OK, getIntent());
                                        finish();
                                    }
                                });
                        return;
                    }
                }

            }
        });
    }

    /**
     * Find and assign the correct UI elements to the correct variables from current activity layout
     */
    private void initialiseUIElements(){
        layout_top_section = findViewById(R.id.layout_top_section);
        iv_image_editing_original_picture = findViewById(R.id.iv_image_editing_original_picture);
        iv_image_editing_picture = findViewById(R.id.iv_image_editing_picture);
        rv_image_editing_filters = findViewById(R.id.rv_image_editing_filters);

        ViewGroup.LayoutParams lpm = layout_top_section.getLayoutParams();
        lpm.width = Resources.getSystem().getDisplayMetrics().widthPixels;
        lpm.height = Resources.getSystem().getDisplayMetrics().widthPixels;

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        layoutManager.scrollToPosition(0);
        rv_image_editing_filters.setLayoutManager(layoutManager);
        rv_image_editing_filters.setHasFixedSize(true);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL);
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider_vertical_image_editing));
        rv_image_editing_filters.addItemDecoration(dividerItemDecoration);

        // Widgets from toolbar
        btn_toolbar_camera_gallery_next = toolbar.findViewById(R.id.btn_toolbar_camera_gallery_next);
    }

    private void bindDataToAdapter() {
        GPUImage.getBitmapForMultipleFilters(selectedImageThumbnail, IMAGE_FILTERS, new GPUImage.ResponseListener<Bitmap>() {
            int count = 0;
            @Override
            public void response(Bitmap item) {
                if(count < filterList.size()){
                    filterList.get(count).filteredImage = item;
                    count++;
                }
            }
        });

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                filterThumbnailsAdapter = new FilterThumbnailsAdapter(ImageEditingActivity.this, filterList, new FilterThumbnailsAdapter.FilteredThumbnailListener() {
                    @Override
                    public void onSelected(ThumbnailItem thumbnailItem) {
                        gpuImage.setFilter(thumbnailItem.filter);
                        iv_image_editing_picture.setImageBitmap(gpuImage.getBitmapWithFilterApplied());
                    }
                });
                rv_image_editing_filters.setAdapter(filterThumbnailsAdapter);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();  return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
