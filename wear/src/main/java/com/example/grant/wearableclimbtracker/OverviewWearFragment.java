package com.example.grant.wearableclimbtracker;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.mysynclibrary.ClimbResultsProvider;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.model.Climb;
import com.example.mysynclibrary.model.RealmResultsEvent;

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
    private ClimbResultsProvider mClimbResultsProvider;

    public OverviewWearFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_session_overview, container, false);
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

        Shared.ClimbType climbType = mClimbResultsProvider.getType();
        typeTextView.setText(climbType.title);

        int sum = (mResult.sum("grade")).intValue();
        int count = mResult.size();
        Number max = mResult.max("grade");

        if (max == null) {
            maxTextView.setVisibility(View.GONE);
        } else {
            maxTextView.setVisibility(View.VISIBLE);
            List<String> gradeList = climbType.grades;

            maxTextView.setText(String.format("MAX: %s", gradeList.get(max.intValue())));
        }

        pointsTextView.setText(String.format("POINTS: %d", sum));
        countTextView.setText(String.format("CLIMBS: %d", count));
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