package com.firefishcomms.instagramlikegallery.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firefishcomms.instagramlikegallery.R;
import com.firefishcomms.instagramlikegallery.activities.BaseActivity;
import com.firefishcomms.instagramlikegallery.adapters.model.ThumbnailItem;

import java.util.List;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

/**
 * @author Varun on 01/07/15.
 */
public class FilterThumbnailsAdapter extends RecyclerView.Adapter<FilterThumbnailsAdapter.ThumbnailsViewHolder> {

    private BaseActivity activity;
    private FilteredThumbnailListener filteredThumbnailListener;
    private List<ThumbnailItem> data;
    private GPUImageFilter selectedFilter;

    public FilterThumbnailsAdapter(BaseActivity a, List<ThumbnailItem> d, FilteredThumbnailListener filteredThumbnailListener) {
        this.activity = a;
        this.data = d;
        this.filteredThumbnailListener = filteredThumbnailListener;
    }

    @Override
    public ThumbnailsViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.item_filtered_thumbnail, viewGroup, false);
        return new ThumbnailsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ThumbnailsViewHolder holder, final int position) {
        final ThumbnailItem thumbnailItem = data.get(position);

        holder.thumbnail.setImageBitmap(thumbnailItem.filteredImage);

        holder.thumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filteredThumbnailListener.onSelected(thumbnailItem);
                selectedFilter = data.get(position).filter;
            }

        });

        /** Get filter name by the filter class simple name **/
        String camelCaseRegex = "(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])";
        String filterName = thumbnailItem.filter.getClass().getSimpleName().replace("GPUImage", "").replace("Filter", "");
        String filterNaming = "";
        for(String x : filterName.split(camelCaseRegex)){
            filterNaming += x + " ";
        }
        filterNaming = filterNaming.trim();
        if(filterNaming.length() <= 0){
            filterNaming = "Normal";
        }

        holder.thumbnailText.setText(filterNaming);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public GPUImageFilter getSelectedFilter(){
        return (selectedFilter == null)? data.get(0).filter : selectedFilter;
    }

    public class ThumbnailsViewHolder extends RecyclerView.ViewHolder {
        public ImageView thumbnail;
        public TextView thumbnailText;

        public ThumbnailsViewHolder(View v) {
            super(v);
            this.thumbnail = (ImageView) v.findViewById(R.id.item_filtered_thumbnail_image);
            this.thumbnailText = v.findViewById(R.id.item_filtered_thumbnail_text);
        }
    }

    public interface FilteredThumbnailListener{
        void onSelected(ThumbnailItem thumbnailItem);
    }
}
