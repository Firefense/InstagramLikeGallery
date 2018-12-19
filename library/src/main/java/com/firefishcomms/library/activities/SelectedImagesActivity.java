package com.firefishcomms.library.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.firefishcomms.library.R;
import com.firefishcomms.library.adapters.SelectedImagesAdapter;
import com.firefishcomms.library.adapters.model.SelectedImagesItem;
import com.firefishcomms.library.commons.Constants;
import com.firefishcomms.library.commons.RequestCode;
import com.firefishcomms.library.commons.ResultCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

public class SelectedImagesActivity extends BaseActivity {

    private RecyclerView rv_selected_images;

    // Toolbar Widgets
    private Button btn_toolbar_camera_gallery_next;

    private List<SelectedImagesItem> selectedImages;
    private SelectedImagesAdapter selectedImagesAdapter;
    private int totalSavedImages = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_selected_images);
        initialiseUIElements();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        selectedImages = new ArrayList<>();

        List<File> selectedImageFiles = (ArrayList<File>) getIntent().getSerializableExtra(Constants.EXTRA_SELECTED_IMAGE_FILES);
        if(selectedImageFiles != null) {
            for(File file : selectedImageFiles){
                selectedImages.add(new SelectedImagesItem(file));
            }
        }

        selectedImagesAdapter = new SelectedImagesAdapter(this, selectedImages);
        rv_selected_images.setAdapter(selectedImagesAdapter);

        btn_toolbar_camera_gallery_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btn_toolbar_camera_gallery_next.setEnabled(false);
                showLoadingSpinner(Gravity.CENTER);

                final ArrayList<File> savedImages = new ArrayList<>();

                Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        for(SelectedImagesItem selectedImagesItem : selectedImages) {
                            String selectedFilterClassName = selectedImagesItem.filter.getClass().getSimpleName();
                            if (selectedFilterClassName.equals(GPUImageFilter.class.getSimpleName())) {
                                savedImages.add(selectedImagesItem.imageFile);
                                totalSavedImages++;
                            } else {
                                FileInputStream fileInputStream = null;

                                try {
                                    fileInputStream = new FileInputStream(selectedImagesItem.imageFile);
                                    Bitmap selectedImageBitmap = BitmapFactory.decodeStream(fileInputStream);

                                    final String fileName = String.format("%d.jpg", System.currentTimeMillis());

                                    GPUImage gpuImage = new GPUImage(SelectedImagesActivity.this);
                                    gpuImage.setFilter(selectedImagesItem.filter);
                                    gpuImage.setImage(selectedImageBitmap);
                                    gpuImage.saveToPictures("", fileName,
                                            new GPUImage.OnPictureSavedListener() {
                                                @Override
                                                public void onPictureSaved(Uri uri) {
                                                    synchronized (savedImages) {
                                                        totalSavedImages++;
                                                        savedImages.add(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fileName));
                                                        if (totalSavedImages == selectedImages.size()) {
                                                            getIntent().putExtra(Constants.EXTRA_FILTERED_IMAGE_FILES, savedImages);
                                                            setResult(ResultCode.RESULT_OK, getIntent());
                                                            finish();
                                                        }
                                                    }
                                                }
                                            });
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                                finally{
                                    if(fileInputStream != null){
                                        try {
                                            fileInputStream.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }

                            if(totalSavedImages == selectedImages.size()){
                                getIntent().putExtra(Constants.EXTRA_FILTERED_IMAGE_FILES, savedImages);
                                setResult(ResultCode.RESULT_OK, getIntent());
                                finish();
                            }
                        }
                    }
                });
            }
        });
    }

    /**
     * Find and assign the correct UI elements to the correct variables from current activity layout
     */
    private void initialiseUIElements(){
        rv_selected_images = findViewById(R.id.rv_selected_images);

        rv_selected_images.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL , false);
        rv_selected_images.setLayoutManager(llm);

        SnapHelper helper = new LinearSnapHelper();
        helper.attachToRecyclerView(rv_selected_images);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL);
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider_vertical_selected_images));
        rv_selected_images.addItemDecoration(dividerItemDecoration);

        // Widgets from toolbar
        btn_toolbar_camera_gallery_next = toolbar.findViewById(R.id.btn_toolbar_camera_gallery_next);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();  return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case RequestCode.IMAGE_EDIT_GET_FILTER:
                if(resultCode == ResultCode.RESULT_OK) {
                    int selectedImageFilterIndex = data.getIntExtra(Constants.EXTRA_SELECTED_IMAGE_FILTER_INDEX, 0);
                    if(selectedImageFilterIndex >= ImageEditingActivity.IMAGE_FILTERS.size()) selectedImageFilterIndex = 0;

                    selectedImagesAdapter.applyImageFilterToSelectedImage(ImageEditingActivity.IMAGE_FILTERS.get(selectedImageFilterIndex));
                }
                break;
        }
    }
}
