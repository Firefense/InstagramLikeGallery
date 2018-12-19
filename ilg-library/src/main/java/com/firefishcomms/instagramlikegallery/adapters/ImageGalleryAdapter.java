package com.firefishcomms.instagramlikegallery.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.firefishcomms.instagramlikegallery.R;
import com.firefishcomms.instagramlikegallery.activities.BaseActivity;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Anton Salim on 9/26/2017.
 */

public class ImageGalleryAdapter extends RecyclerView.Adapter<ImageGalleryAdapter.MyViewHolder>{

    private List<File> mGalleryPhotos;
    private ArrayList<File> selectedFiles = new ArrayList<>();
    private BaseActivity mActivity;
    private OnItemClickListener onItemClickListener;
    private boolean isMultiSelectMode;
    private int maxSelectedImages;
    private MyViewHolder currSelectedItemHolder;

    public ImageGalleryAdapter(BaseActivity activity, List<File> galleryPhotos, OnItemClickListener onItemClickListener) {
        this.mActivity = activity;
        this.mGalleryPhotos = galleryPhotos;
        this.onItemClickListener = onItemClickListener;
        this.isMultiSelectMode = false;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View photoView = inflater.inflate(R.layout.item_gallery_image, parent, false);

        MyViewHolder viewHolder = new MyViewHolder(photoView);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        holder.galleryPhoto = mGalleryPhotos.get(position);

        if(currSelectedItemHolder == null && position == 0) {
            currSelectedItemHolder = holder;
            onItemClickListener.onItemClick(position);
            if(selectedFiles.isEmpty())
                selectedFiles.add(currSelectedItemHolder.galleryPhoto);
        }

        Picasso.get().load(holder.galleryPhoto).resize(200,0).centerCrop().into(holder.iv_gallery_image_photo);

        if(holder.equals(currSelectedItemHolder)){
            holder.view_gallery_image_curr_selected_indicator.setVisibility(View.VISIBLE);
        } else{
            holder.view_gallery_image_curr_selected_indicator.setVisibility(View.GONE);
        }

        if(isMultiSelectMode) {
            if (selectedFiles.contains(holder.galleryPhoto)) {
                holder.view_gallery_image_selected_indicator.setVisibility(View.VISIBLE);
            } else {
                holder.view_gallery_image_selected_indicator.setVisibility(View.GONE);
            }
        } else{
            holder.view_gallery_image_selected_indicator.setVisibility(View.GONE);
        }
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public ImageView iv_gallery_image_photo;
        public View view_gallery_image_curr_selected_indicator;
        public View view_gallery_image_selected_indicator;
        public File galleryPhoto;

        public MyViewHolder(View itemView) {

            super(itemView);
            iv_gallery_image_photo = itemView.findViewById(R.id.iv_gallery_image_photo);
            view_gallery_image_curr_selected_indicator = itemView.findViewById(R.id.view_gallery_image_curr_selected_indicator);
            view_gallery_image_selected_indicator = itemView.findViewById(R.id.view_gallery_image_selected_indicator);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if(!isMultiSelectMode) { // if single image mode
                this.view_gallery_image_selected_indicator.setVisibility(View.GONE);
                selectedFiles.clear();
                selectedFiles.add(galleryPhoto);
            } else if(selectedFiles.contains(galleryPhoto)) { // To deselect
                selectedFiles.remove(galleryPhoto);
                view_gallery_image_selected_indicator.setVisibility(View.GONE);
            } else if(selectedFiles.size() < maxSelectedImages) { // To select
                view_gallery_image_selected_indicator.setVisibility(View.VISIBLE);
                selectedFiles.add(galleryPhoto);
            } else{
                // TODO: show snackbar instead
                Toast.makeText(mActivity, String.format("The limit is %d photos.", maxSelectedImages), Toast.LENGTH_SHORT).show();
            }

            if(onItemClickListener != null)
                onItemClickListener.onItemClick(getAdapterPosition());

            if(this.equals(currSelectedItemHolder)) return;

            this.view_gallery_image_curr_selected_indicator.setVisibility(View.VISIBLE);
            currSelectedItemHolder.view_gallery_image_curr_selected_indicator.setVisibility(View.GONE);
            currSelectedItemHolder = this;
        }
    }

    public interface OnItemClickListener{
        /**
         * This method will be called if ImageGalleryAdapter.isMultiSelectMode is false
         * @param position
         */
        void onItemClick(int position);
    }

    public ArrayList<File> getSelectedFile(){
        return selectedFiles;
    }

    /**
     * To change gallery selection mode to either single or multi select. maxSelectedImage If isMultiSelectMode is false,
     * @param isMultiSelectMode
     * @param maxSelectedImages
     */
    public void setMultiSelectMode(boolean isMultiSelectMode, int maxSelectedImages){
        this.selectedFiles.clear();
        this.isMultiSelectMode = isMultiSelectMode;

        if(isMultiSelectMode){
            this.maxSelectedImages = maxSelectedImages;
        }

        if(currSelectedItemHolder != null)
            selectedFiles.add(currSelectedItemHolder.galleryPhoto);

        notifyDataSetChanged();
    }

    public void customNotifyDataSetChanged(){
        currSelectedItemHolder = null;
        if(!isMultiSelectMode) selectedFiles.clear();
        notifyDataSetChanged();
    }

    public File getItem(int position){
        return mGalleryPhotos.get(position);
    }

    @Override
    public int getItemCount() {
        return (mGalleryPhotos.size());
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}
