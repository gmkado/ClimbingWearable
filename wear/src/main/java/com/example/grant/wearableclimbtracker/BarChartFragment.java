package com.example.grant.wearableclimbtracker;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Grant on 8/1/2016.
 */
public class BarChartFragment extends android.app.Fragment {
    public static final String ARG_CLIMB_TYPE = "climbTypeArg";
    private MainActivity.ClimbType mClimbType;
    private final String TAG = "BarChartFragment";
    private TextView typeTextView;

    public BarChartFragment(){}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_barchart, container, false);

        typeTextView = (TextView)rootView.findViewById(R.id.title_textView);
        mClimbType = MainActivity.ClimbType.values()[getArguments().getInt(ARG_CLIMB_TYPE)];

        typeTextView.setText(mClimbType.title);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        updateChartView();
    }

    public void updateChartView() {
        Log.d(TAG, "updateChartView()");
        // run a query for this weeks points


    }
}
