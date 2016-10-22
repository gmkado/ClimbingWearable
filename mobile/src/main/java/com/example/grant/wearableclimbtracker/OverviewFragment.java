package com.example.grant.wearableclimbtracker;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mysynclibrary.Shared;

/**
 * Created by Grant on 10/17/2016.
 */
public class OverviewFragment extends Fragment{
    private MainActivity mMainActivity;

    public OverviewFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_overview_mobile, container, false);
        mMainActivity = (MainActivity)getActivity();

        return rootView;
    }

}
