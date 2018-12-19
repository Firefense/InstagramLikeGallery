package com.firefishcomms.library.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.firefishcomms.library.R;
import com.firefishcomms.library.activities.GalleryActivity;
import com.firefishcomms.library.adapters.ImageGalleryAdapter;
import com.firefishcomms.library.views.GridSpacingItemDecoration;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GalleryFragment extends BaseFragment {

    private final int REQUEST_EXT_STORAGE_PERMISSIONS = 1;

    private RecyclerView rv_gallery_images;
    private ImageView iv_gallery_selected_photo;
    private ImageView iv_gallery_multi_select_mode;

    private ImageGalleryAdapter imageGalleryAdapter;
    private ArrayList<File> galleryImages;

    private boolean isMultiSelectEnabled = false;
    private boolean isMultiSelectMode = false;
    private int maxSelectedImages = 5;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        initialiseUIElements(view);

        galleryImages = new ArrayList<>();


        imageGalleryAdapter = new ImageGalleryAdapter(activity, galleryImages, new ImageGalleryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Picasso.get().load(imageGalleryAdapter.getItem(position)).resize(Resources.getSystem().getDisplayMetrics().widthPixels, 0).into(iv_gallery_selected_photo);
            }
        });
        rv_gallery_images.setAdapter(imageGalleryAdapter);

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_EXT_STORAGE_PERMISSIONS);
        }

        if(isMultiSelectEnabled){
            iv_gallery_multi_select_mode.setVisibility(View.VISIBLE);
            iv_gallery_multi_select_mode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(isMultiSelectMode){
                        isMultiSelectMode = false;
                        iv_gallery_multi_select_mode.setBackground(ContextCompat.getDrawable(activity, R.drawable.circle_background_black_translucent));
                    } else{
                        isMultiSelectMode = true;
                        iv_gallery_multi_select_mode.setBackground(ContextCompat.getDrawable(activity, R.drawable.circle_background_accent));
                    }
                    imageGalleryAdapter.setMultiSelectMode(isMultiSelectMode, maxSelectedImages);
                }
            });
        } else{
            iv_gallery_multi_select_mode.setVisibility(View.GONE);
            iv_gallery_multi_select_mode.setOnClickListener(null);
        }
        return view;
    }

    /**
     * Find and assign the correct UI elements to the correct variables from current activity layout
     */
    private void initialiseUIElements(View view){
        iv_gallery_multi_select_mode = view.findViewById(R.id.iv_gallery_multi_select_mode);
        rv_gallery_images = view.findViewById(R.id.rv_gallery_images);
        iv_gallery_selected_photo = view.findViewById(R.id.iv_gallery_selected_photo);

        // Create GridLayoutManager to show images in rv_images in GridView layout
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getContext(), 4){
            @Override
            public boolean isAutoMeasureEnabled(){
                return true;
            }
        }; // set 3 items shown per row
        rv_gallery_images.setHasFixedSize(true);
        rv_gallery_images.setLayoutManager(layoutManager);

        rv_gallery_images.addItemDecoration(new GridSpacingItemDecoration(4, 5, true));
    }

    public void loadImages(List<File> images){
        if(activity == null) return;

        galleryImages.clear();
        galleryImages.addAll(images);

        if(imageGalleryAdapter == null){
            imageGalleryAdapter = new ImageGalleryAdapter(activity, galleryImages, new ImageGalleryAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(int position) {
                    Picasso.get().load(imageGalleryAdapter.getItem(position)).resize(Resources.getSystem().getDisplayMetrics().widthPixels, 0).into(iv_gallery_selected_photo);
                }
            });
            rv_gallery_images.setAdapter(imageGalleryAdapter);
        } else{
            imageGalleryAdapter.customNotifyDataSetChanged();
        }
    }

    public void setMaxSelectedImages(int maxImages){
        this.maxSelectedImages = maxImages;
    }

    public void setMultiSelectEnabled(boolean enabled){
        this.isMultiSelectEnabled = enabled;
    }

    public ArrayList<File> getSelectedImages(){
        return imageGalleryAdapter.getSelectedFile();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && activity != null) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_EXT_STORAGE_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_EXT_STORAGE_PERMISSIONS) {
            List<String> permissionList = Arrays.asList(permissions);
            boolean permissionsGranted = true;

            if(!permissionList.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE) || grantResults[permissionList.indexOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)] != PackageManager.PERMISSION_GRANTED){
                permissionsGranted &= false;
            }

            if(!permissionList.contains(Manifest.permission.READ_EXTERNAL_STORAGE) || grantResults[permissionList.indexOf(Manifest.permission.READ_EXTERNAL_STORAGE)] != PackageManager.PERMISSION_GRANTED){
                permissionsGranted &= false;
            }

            if(permissionsGranted && activity instanceof GalleryActivity){
                ((GalleryActivity) activity).reloadGalleryFolderDirectories();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}