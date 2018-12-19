package com.firefishcomms.instagramlikegallery;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.view.View;
import android.widget.Button;

import com.firefishcomms.library.activities.GalleryActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    private final static int MAX_IMAGES = 5;

    private RecyclerView rv_selected_images;

    // Toolbar Widgets
    private Button show_gallery;

    private List<SelectedImagesItem> selectedImages;
    private SelectedImagesAdapter selectedImagesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialiseUIElements();

        selectedImages = new ArrayList<>();

        selectedImagesAdapter = new SelectedImagesAdapter(this, selectedImages);
        rv_selected_images.setAdapter(selectedImagesAdapter);

        show_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
                intent.putExtra(Constants.EXTRA_GALLERY_MAX_IMAGES, MAX_IMAGES);
                intent.putExtra(Constants.EXTRA_ENABLE_MULTI_SELECT_MODE, true);
                startActivityForResult(intent, RequestCode.GALLERY_GET_IMAGES);
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
        show_gallery = findViewById(R.id.show_gallery);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case RequestCode.GALLERY_GET_IMAGES:
                if(resultCode == ResultCode.RESULT_OK) {
                    ArrayList<File> images = (ArrayList<File>) data.getSerializableExtra(Constants.EXTRA_GALLERY_IMAGE_FILES);
                    if(images == null) images = new ArrayList<>();

                    for(File file : images){
                        if(selectedImages.size() == MAX_IMAGES) break;
                        selectedImages.add(new SelectedImagesItem(file));
                    }

                    selectedImagesAdapter.notifyDataSetChanged();
                }
                break;
        }
    }
}
