package com.firefishcomms.library.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.firefishcomms.library.R;
import com.firefishcomms.library.adapters.FragmentSectionsPageAdapter;
import com.firefishcomms.library.adapters.GalleryDirectorySpinnerAdapter;
import com.firefishcomms.library.commons.Constants;
import com.firefishcomms.library.commons.RequestCode;
import com.firefishcomms.library.commons.ResultCode;
import com.firefishcomms.library.fragments.BaseFragment;
import com.firefishcomms.library.fragments.CameraFragment;
import com.firefishcomms.library.fragments.GalleryFragment;
import com.firefishcomms.library.services.ImageGalleryService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends BaseActivity {

    private ViewPager vp_camera_gallery_container;
    private TabLayout tl_camera_gallery_tab_layout;

    // Toolbar Widgets
    private TextView tv_toolbar_title;
    private Spinner spinner_toolbar_camera_gallery_select_directory;
    private Button btn_toolbar_camera_gallery_next;

    private GalleryDirectorySpinnerAdapter galleryDirectorySpinnerAdapter;
    private FragmentSectionsPageAdapter fragmentSectionsPageAdapter;
    private GalleryFragment galleryFragment;
    private CameraFragment cameraFragment;
    private List<ImageGalleryService.Folder> folders;
    private boolean enableMultiSelectMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(R.anim.slide_in_up, R.anim.stationary);

        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_gallery);
        initialiseUIElements();

        enableMultiSelectMode = getIntent().getBooleanExtra(Constants.EXTRA_ENABLE_MULTI_SELECT_MODE, false);

        galleryFragment = new GalleryFragment();
        galleryFragment.setMultiSelectEnabled(enableMultiSelectMode);
        galleryFragment.setMaxSelectedImages(getIntent().getIntExtra(Constants.EXTRA_GALLERY_MAX_IMAGES, 5));

        cameraFragment = new CameraFragment();

        fragmentSectionsPageAdapter = new FragmentSectionsPageAdapter(getSupportFragmentManager());
        fragmentSectionsPageAdapter.addFragment(galleryFragment, "Gallery");
        fragmentSectionsPageAdapter.addFragment(cameraFragment, "Photo");
        vp_camera_gallery_container.setAdapter(fragmentSectionsPageAdapter);

        tl_camera_gallery_tab_layout.setupWithViewPager(vp_camera_gallery_container);
        updateToolbar(galleryFragment);

        vp_camera_gallery_container.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                updateToolbar(fragmentSectionsPageAdapter.getItem(i));
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.layout_ic_action_close);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        folders = new ArrayList<>();
        folders.addAll(ImageGalleryService.getInstance(this).getAllImageFolderDirectories());
        galleryDirectorySpinnerAdapter = new GalleryDirectorySpinnerAdapter(this, android.R.layout.simple_spinner_dropdown_item, folders);
        spinner_toolbar_camera_gallery_select_directory.setAdapter(galleryDirectorySpinnerAdapter);

        spinner_toolbar_camera_gallery_select_directory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                galleryFragment.loadImages(ImageGalleryService.getInstance(GalleryActivity.this).retrieveImages(galleryDirectorySpinnerAdapter.getItem(position)));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        btn_toolbar_camera_gallery_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btn_toolbar_camera_gallery_next.setEnabled(false);
                ArrayList<File> selectedImages = galleryFragment.getSelectedImages();

                if(selectedImages.isEmpty()) {
                    btn_toolbar_camera_gallery_next.setEnabled(true);
                    return;
                }

                Intent intent;

                if(selectedImages.size() == 1) {
                    intent = new Intent(GalleryActivity.this, ImageEditingActivity.class);
                    intent.putExtra(Constants.EXTRA_SELECTED_IMAGE_FILE, selectedImages.get(0));

                    startActivityForResult(intent, RequestCode.IMAGE_EDIT_ADD_FILTER);
                } else{
                    // TODO: start another activity to show all selected images
                    intent = new Intent(GalleryActivity.this, SelectedImagesActivity.class);
                    intent.putExtra(Constants.EXTRA_SELECTED_IMAGE_FILES, selectedImages);

                    startActivityForResult(intent, RequestCode.IMAGES_EDIT_ADD_FILTER);
                }

                btn_toolbar_camera_gallery_next.setEnabled(true);
            }
        });
    }

    /**
     * Find and assign the correct UI elements to the correct variables from current activity layout
     */
    private void initialiseUIElements(){
        vp_camera_gallery_container = findViewById(R.id.vp_camera_gallery_container);
        tl_camera_gallery_tab_layout = findViewById(R.id.tl_camera_gallery_tab_layout);

        // Widgets from toolbar
        tv_toolbar_title = toolbar.findViewById(R.id.tv_toolbar_title);
        spinner_toolbar_camera_gallery_select_directory = toolbar.findViewById(R.id.spinner_toolbar_camera_gallery_select_directory);
        btn_toolbar_camera_gallery_next = toolbar.findViewById(R.id.btn_toolbar_camera_gallery_next);

        /** Set the position of DropDown of spinner_toolbar_camera_gallery_select_directory to be below spinner_toolbar_camera_gallery_select_directory **/
        spinner_toolbar_camera_gallery_select_directory.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                spinner_toolbar_camera_gallery_select_directory.getViewTreeObserver().removeOnPreDrawListener(this);
                spinner_toolbar_camera_gallery_select_directory.setDropDownVerticalOffset(spinner_toolbar_camera_gallery_select_directory.getMeasuredHeight());
                return false;
            }
        });
        /** ------------------------------------------------------------------------- **/
    }

    private void updateToolbar(BaseFragment baseFragment){
        if(baseFragment instanceof CameraFragment){
            tv_toolbar_title.setVisibility(View.VISIBLE);
            spinner_toolbar_camera_gallery_select_directory.setVisibility(View.GONE);
            btn_toolbar_camera_gallery_next.setVisibility(View.GONE);
        } else {
            tv_toolbar_title.setVisibility(View.GONE);
            spinner_toolbar_camera_gallery_select_directory.setVisibility(View.VISIBLE);
            btn_toolbar_camera_gallery_next.setVisibility(View.VISIBLE);
        }
    }

    public void reloadGalleryFolderDirectories(){
        folders.clear();
        folders.addAll(ImageGalleryService.getInstance(this).getAllImageFolderDirectories());
        galleryDirectorySpinnerAdapter.notifyDataSetChanged();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        ArrayList<File> selectedImages = new ArrayList<>();

        switch (requestCode){
            case RequestCode.IMAGE_EDIT_ADD_FILTER:
                if(resultCode == ResultCode.RESULT_OK) {
                    File image = (File) data.getSerializableExtra(Constants.EXTRA_FILTERED_IMAGE_FILE);
                    if(image != null){
                        selectedImages.add(image);
                    }
                    getIntent().putExtra(Constants.EXTRA_GALLERY_IMAGE_FILES, selectedImages);
                    setResult(ResultCode.RESULT_OK, getIntent());
                    finish();
                    return;
                }
                break;
            case RequestCode.IMAGES_EDIT_ADD_FILTER:
                if(resultCode == ResultCode.RESULT_OK) {
                    ArrayList<File> images = (ArrayList<File>) data.getSerializableExtra(Constants.EXTRA_FILTERED_IMAGE_FILES);
                    if(images != null){
                        selectedImages.addAll(images);
                    }
                    getIntent().putExtra(Constants.EXTRA_GALLERY_IMAGE_FILES, selectedImages);
                    setResult(ResultCode.RESULT_OK, getIntent());
                    finish();
                    return;
                }
                break;
        }
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
    public void finish(){
        super.finish();

        overridePendingTransition(R.anim.stationary, R.anim.slide_out_down);
    }
}
