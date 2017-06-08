package com.example.grant.wearableclimbtracker;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.mysynclibrary.ClimbStats;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.eventbus.RealmResultsEvent;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.threeten.bp.temporal.ChronoUnit;

/**
 * Created by Grant on 10/17/2016.
 */
public class OverviewMobileFragment extends Fragment implements OnChartGestureListener {
    private TextView overallStatsTextView;
    private final String TAG = "OverviewMobileFragment";
    private TextView typeTextView;
    private Shared.ClimbType mClimbType;
    private ChronoUnit mDateRange;
    private View mSessionLayout;
    private View mNonSessionLayout;



    /* Chart handles */
    private PieChart mPieChartInner;
    private PieChart mPieChartOuter;
    private PieChart mPieChartMiddle;

    /* Stats being tracked */
    private ClimbStats.StatType mCurrentStat;
    private ClimbStats mClimbStats;


    public OverviewMobileFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_overview_mobile, container, false);
        mSessionLayout = rootView.findViewById(R.id.sessionLayout);
        mNonSessionLayout = rootView.findViewById(R.id.nonSessionLayout);

        mPieChartInner = (PieChart)rootView.findViewById(R.id.chart_inner);
        mPieChartMiddle = (PieChart)rootView.findViewById(R.id.chart_middle);
        mPieChartOuter = (PieChart)rootView.findViewById(R.id.chart_outer);
        mCurrentStat = ClimbStats.StatType.values()[0];

        // get the screen width
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        // set text size to be some percentage of the width
        int textPixels = (int) (0.6 * metrics.widthPixels);
        int graphPixels = metrics.widthPixels-textPixels;
        int widthPixels = graphPixels/2/3;
        int bufferPixels = (int)(.1*widthPixels);

        setupPieChart(mPieChartOuter, metrics.widthPixels, widthPixels-bufferPixels);
        setupPieChart(mPieChartMiddle, metrics.widthPixels - 2*widthPixels, widthPixels-bufferPixels);
        setupPieChart(mPieChartInner, metrics.widthPixels - 4*widthPixels, widthPixels-bufferPixels);

        overallStatsTextView = (TextView) rootView.findViewById(R.id.overallStatsTextView);
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
        chart.setHoleColor(Color.WHITE);

        chart.setTransparentCircleColor(Color.WHITE);
        chart.setTransparentCircleAlpha(110);
        //chart.setDrawEntryLabels(false);
        chart.setDrawEntryLabels(true);
        float radiusPercent = (diameter-2f*width)/diameter * 100;
        chart.setHoleRadius(radiusPercent);
        chart.setTransparentCircleRadius(61f);

        chart.setDrawCenterText(true);
        chart.setRotationAngle(270);
        chart.setRotationEnabled(false);
        chart.setHighlightPerTapEnabled(false);
        chart.setDescription("");
        chart.getLegend().setEnabled(false);

        // add a selection listener
        chart.setOnChartGestureListener(this);
    }




    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }


    /*TODO: fix this
    @Subscribe(sticky = true)
    public void onRealmResultEvent(RealmResultsEvent event) {
        mClimbStats = event.climbstats;
        mDateRange = event.dateRange;
        mClimbType = event.climbType;
        updatePointsView();
    }*/

    @Subscribe(sticky = true)
    public void onShowCaseEvent(ShowcaseEvent event) {
        if(event.type == ShowcaseEvent.ShowcaseEventType.goals) {
            event.view.setContentTitle("Your climbing goals");
            event.view.setContentText("Tap to cycle through goals");
            event.view.setShowcase(new ViewTarget(R.id.chart_outer ,getActivity()), true);
        }
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

        if(mDateRange != ChronoUnit.FOREVER) {
            mSessionLayout.setVisibility(View.VISIBLE);
            mNonSessionLayout.setVisibility(View.GONE);


            // Setup data for the outer chart -- POINTS
            PieData data = mClimbStats.getPieData(ClimbStats.StatType.POINTS, false);
            data.setDrawValues(false);
            mPieChartOuter.setData(data);
            mPieChartOuter.highlightValue(1, 0, false); // highlight the current value
            //mPieChartOuter.setEntryLabelColor(ClimbStats.StatType.POINTS.basecolor.Dark);

            // Setup data for the middle chart -- NUMBER OF CLIMBS
            data = mClimbStats.getPieData(ClimbStats.StatType.CLIMBS, false);
            data.setDrawValues(false);
            mPieChartMiddle.setData(data);
            mPieChartMiddle.highlightValue(1, 0, false);
            //mPieChartMiddle.setEntryLabelColor(ClimbStats.StatType.CLIMBS.basecolor.Dark);

            // Setup data for the inner chart -- MAX GRADE
            data = mClimbStats.getPieData(ClimbStats.StatType.GRADE, false);
            data.setDrawValues(false);
            mPieChartInner.setData(data);
            mPieChartInner.highlightValue(1, 0, false);
            //mPieChartInner.setEntryLabelColor(ClimbStats.StatType.GRADE.basecolor.Dark);

            mPieChartInner.setCenterText(mClimbStats.getCenterText(mCurrentStat));

            // animate the charts
            mPieChartInner.animateY(500, Easing.EasingOption.EaseInOutQuad);
            mPieChartOuter.animateY(500, Easing.EasingOption.EaseInOutQuad);
            mPieChartMiddle.animateY(500, Easing.EasingOption.EaseInOutQuad);

            //mPieChartInner.invalidate(); // refresh
        } else {
            mSessionLayout.setVisibility(View.GONE);
            mNonSessionLayout.setVisibility(View.VISIBLE);

            overallStatsTextView.setText(mClimbStats.getCenterText(mCurrentStat));
        }
    }


    /***************************** CHART GESTURE CALLBACKS ********************************************/
    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartLongPressed(MotionEvent me) {

    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {

    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {
        if(mCurrentStat.ordinal() == ClimbStats.StatType.values().length-1) {
            mCurrentStat = ClimbStats.StatType.values()[0];
        }else {
            mCurrentStat = ClimbStats.StatType.values()[mCurrentStat.ordinal() + 1];
        }
        mPieChartInner.setCenterText(mClimbStats.getCenterText(mCurrentStat));
        mPieChartInner.invalidate();
    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {

    }
}
