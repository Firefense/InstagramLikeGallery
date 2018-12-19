package com.firefishcomms.instagramlikegallery;

import android.content.Context;
import android.content.Intent;

import com.firefishcomms.instagramlikegallery.activities.GalleryActivity;

public class InstagramLikeGallery {
    public static Intent getGalleryIntent(Context context){
        return new Intent(context, GalleryActivity.class);
    }
}
