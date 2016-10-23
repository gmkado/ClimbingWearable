package com.example.grant.wearableclimbtracker;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mysynclibrary.ClimbResultsProvider;
import com.example.mysynclibrary.Shared;

/**
 * Created by Grant on 10/17/2016.
 */
public class OverviewMobileFragment extends Fragment{
    private MainActivity mMainActivity;
    private ClimbResultsProvider mClimbResultsProvider;

    public OverviewMobileFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_overview_mobile, container, false);
        mMainActivity = (MainActivity)getActivity();

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mClimbResultsProvider = (ClimbResultsProvider) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement ClimbResultsProvider");
        }
    }

}
