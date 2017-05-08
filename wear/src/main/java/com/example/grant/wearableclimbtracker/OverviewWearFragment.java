package com.example.grant.wearableclimbtracker;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.realm.Climb;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

import io.realm.RealmChangeListener;
import io.realm.RealmResults;

/**
 * Created by Grant on 8/1/2016.
 */
public class OverviewWearFragment extends Fragment {

    private TextView pointsTextView;
    private final String TAG = "OverviewWearFragment";
    private TextView typeTextView;
    private TextView countTextView;
    private TextView maxTextView;
    private RealmResults<Climb> mResult;
    private Shared.ClimbType mClimbType;

    public OverviewWearFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_session_overview, container, false);

        Shared.matchDeviceSizeProgrammatically(getContext(), rootView);

        pointsTextView = (TextView) rootView.findViewById(R.id.points_textview);
        countTextView = (TextView) rootView.findViewById(R.id.count_textview);
        maxTextView = (TextView) rootView.findViewById(R.id.max_textView);

        typeTextView = (TextView) rootView.findViewById(R.id.title_textView);

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
            mResult.removeAllChangeListeners();
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

        if (max == null) {
            maxTextView.setVisibility(View.GONE);
        } else {
            maxTextView.setVisibility(View.VISIBLE);
            List<String> gradeList = mClimbType.grades;

            maxTextView.setText(String.format("MAX: %s", gradeList.get(max.intValue())));
        }

        pointsTextView.setText(String.format("POINTS: %d", sum));
        countTextView.setText(String.format("CLIMBS: %d", count));
    }

}