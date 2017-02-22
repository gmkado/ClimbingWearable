package com.example.grant.wearableclimbtracker;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.eventbus.RealmResultsEvent;
import com.example.mysynclibrary.realm.Climb;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

import io.realm.RealmChangeListener;
import io.realm.RealmResults;

/**
 * Created by Grant on 10/17/2016.
 */
public class OverviewMobileFragment extends Fragment{
    private TextView pointsTextView;
    private final String TAG = "OverviewMobileFragment";
    private TextView typeTextView;
    private TextView countTextView;
    private TextView maxTextView;
    private RealmResults<Climb> mResult;
    private Shared.ClimbType mClimbType;
    private TextView ppsAvgTextView;

    public OverviewMobileFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_overview_mobile, container, false);
        pointsTextView = (TextView) rootView.findViewById(R.id.points_textview);
        ppsAvgTextView = (TextView) rootView.findViewById(R.id.ppsavg_textview);

        countTextView = (TextView) rootView.findViewById(R.id.count_textview);
        maxTextView = (TextView) rootView.findViewById(R.id.max_textview);

        typeTextView = (TextView) rootView.findViewById(R.id.title_textview);

        return rootView;
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }

    @Subscribe(sticky = true)
    public void onRealmResult(RealmResultsEvent event) {
        mResult = event.realmResults;
        mClimbType = event.climbType;
        mResult.addChangeListener(new RealmChangeListener<RealmResults<Climb>>() {
            @Override
            public void onChange(RealmResults<Climb> element) {
                updatePointsView();
            }
        });

        updatePointsView();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mResult!=null) {
            mResult.removeChangeListeners();
        }
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }


    public void updatePointsView() {
        Log.d(TAG, "updatePointsView()");

        typeTextView.setText(mClimbType.title);

        int sum = (mResult.sum("grade")).intValue();
        int count = mResult.size();
        Number max = mResult.max("grade");

        float avgPtsPerSession = 0;
        if(count!=0) {
            // loop through to get the number of unique days in the result
            HashSet<Date> dates = new HashSet<>(); // use set to get only unique dates
            for (Climb climb : mResult) {
                dates.add(Shared.getStartofDate(climb.getDate()));
            }
            avgPtsPerSession = sum / dates.size();
        }

        if (max == null) {
            maxTextView.setVisibility(View.GONE);
        } else {
            maxTextView.setVisibility(View.VISIBLE);
            List<String> gradeList = mClimbType.grades;

            maxTextView.setText(String.format("MAX: %s", gradeList.get(max.intValue())));
        }

        pointsTextView.setText(String.format("POINTS: %d", sum));
        countTextView.setText(String.format("CLIMBS: %d", count));
        ppsAvgTextView.setText(String.format("PPS AVG: %.1f", avgPtsPerSession));
    }

}
