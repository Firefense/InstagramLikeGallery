package com.firefishcomms.instagramlikegallery.fragments;

import android.content.Context;
import android.support.v4.app.Fragment;

import com.firefishcomms.instagramlikegallery.activities.BaseActivity;

/**
 * Created by Anton on 6/7/2018.
 */

public class BaseFragment extends Fragment {

    protected BaseActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if(context instanceof BaseActivity) {
            activity = (BaseActivity) context;
        }

        setMenuVisibility(isVisible());
    }
}
