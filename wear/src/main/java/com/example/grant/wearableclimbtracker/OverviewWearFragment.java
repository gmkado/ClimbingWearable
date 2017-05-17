package com.example.grant.wearableclimbtracker;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.mysynclibrary.ClimbStats;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.eventbus.RealmResultsEvent;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Created by Grant on 10/17/2016.
 */
public class OverviewWearFragment extends Fragment {
    private TextView overallStatsTextView;
    private final String TAG = "OverviewWearFragment";
    private TextView typeTextView;
    private Shared.ClimbType mClimbType;
    private View mSessionLayout;


    /* Chart handles */
    private PieChart mPieChartInner;
    private PieChart mPieChartOuter;
    private PieChart mPieChartMiddle;

    /* Stats being tracked */
    private ClimbStats mClimbStats;


    public OverviewWearFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_overview_wear, container, false);
        mSessionLayout = rootView.findViewById(R.id.sessionLayout);

        mPieChartInner = (PieChart)rootView.findViewById(R.id.chart_inner);
        mPieChartMiddle = (PieChart)rootView.findViewById(R.id.chart_middle);
        mPieChartOuter = (PieChart)rootView.findViewById(R.id.chart_outer);

        // get the screen width
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        // set height of framelayout to be slightly larger than view
        mSessionLayout.setMinimumHeight(metrics.heightPixels+1);
        // set text size to be some percentage of the width
        int textPixels = (int) (0.7 * metrics.widthPixels);
        int graphPixels = metrics.widthPixels-textPixels;
        int widthPixels = graphPixels/2/3;
        int bufferPixels = (int)(.05*widthPixels);

        setupPieChart(mPieChartOuter, metrics.widthPixels, widthPixels-bufferPixels);
        setupPieChart(mPieChartMiddle, metrics.widthPixels - 2*widthPixels, widthPixels-bufferPixels);
        setupPieChart(mPieChartInner, metrics.widthPixels - 4*widthPixels, widthPixels-bufferPixels);

        typeTextView = (TextView) rootView.findViewById(R.id.title_textview);

        return rootView;
    }

    private void setupPieChart(PieChart chart, int diameter, int width) {
        chart.getLayoutParams().width = diameter;
        chart.getLayoutParams().height = diameter;

        chart.setUsePercentValues(false);
        chart.setExtraOffsets(5, 10, 5, 5);

        chart.setDragDecelerationFrictionCoef(0.95f);

        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.BLACK);

        chart.setTransparentCircleColor(Color.BLACK);
        chart.setTransparentCircleAlpha(110);

        float radiusPercent = (diameter-2f*width)/diameter * 100;
        chart.setHoleRadius(radiusPercent);
        chart.setTransparentCircleRadius(61f);

        chart.setDrawEntryLabels(false);
        chart.setDrawCenterText(true);
        chart.setRotationAngle(270);
        chart.setRotationEnabled(false);
        chart.setHighlightPerTapEnabled(false);
        chart.setDescription("");
        chart.getLegend().setEnabled(false);


    }




    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }


    @Subscribe(sticky = true)
    public void onRealmResultEvent(RealmResultsEvent event) {
        mClimbStats = event.climbstats;
        mClimbType = event.climbType;
        updatePointsView();
    }

    @Subscribe
    public void onEnterAmbientEvent(AmbientEvent event) {
        // change font color
        mPieChartInner.setCenterText(mClimbStats.getWearCenterText(event.isAmbient));
        mPieChartInner.invalidate();
    }

    @Override
    public void onStop() {
        super.onStop();
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


        // Setup data for the outer chart -- POINTS
        PieData data = mClimbStats.getPieData(ClimbStats.StatType.POINTS, true);
        data.setDrawValues(false);
        mPieChartOuter.setData(data);
        mPieChartOuter.highlightValue(1, 0, false); // highlight the current value

        // Setup data for the middle chart -- NUMBER OF CLIMBS
        data = mClimbStats.getPieData(ClimbStats.StatType.CLIMBS, true);
        data.setDrawValues(false);
        mPieChartMiddle.setData(data);
        mPieChartMiddle.highlightValue(1, 0, false);

        // Setup data for the inner chart -- MAX GRADE
        data = mClimbStats.getPieData(ClimbStats.StatType.GRADE, true);
        data.setDrawValues(false);
        mPieChartInner.setData(data);
        mPieChartInner.highlightValue(1, 0, false);


        mPieChartInner.setCenterText(mClimbStats.getWearCenterText(false));

        // animate the charts
        mPieChartInner.animateY(500, Easing.EasingOption.EaseInOutQuad);
        mPieChartOuter.animateY(500, Easing.EasingOption.EaseInOutQuad);
        mPieChartMiddle.animateY(500, Easing.EasingOption.EaseInOutQuad);
    }
}
