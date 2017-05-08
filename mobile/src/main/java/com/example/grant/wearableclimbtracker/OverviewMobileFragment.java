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
import com.example.mysynclibrary.realm.Climb;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.threeten.bp.temporal.ChronoUnit;

import io.realm.RealmResults;

/**
 * Created by Grant on 10/17/2016.
 */
public class OverviewMobileFragment extends Fragment implements OnChartGestureListener {
    private TextView pointsTextView;
    private final String TAG = "OverviewMobileFragment";
    private TextView typeTextView;
    private TextView countTextView;
    private TextView maxTextView;
    private RealmResults<Climb> mResult;
    private Shared.ClimbType mClimbType;
    private TextView ppsAvgTextView;
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

        pointsTextView = (TextView) rootView.findViewById(R.id.points_textview);
        ppsAvgTextView = (TextView) rootView.findViewById(R.id.ppsavg_textview);
        countTextView = (TextView) rootView.findViewById(R.id.count_textview);
        maxTextView = (TextView) rootView.findViewById(R.id.max_textview);
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

        // add a selection listener
        chart.setOnChartGestureListener(this);
    }




    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }


    @Subscribe(sticky = true)
    public void onRealmResultEvent(RealmResultsEvent event) {
        mClimbStats = event.climbstats;
        mDateRange = event.dateRange;
        mClimbType = event.climbType;
        updatePointsView();
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
        /* TODO: look into changing this fragment into a javascript page with concentric rings:
        - http://pablomolnar.github.io/radial-progress-chart/
        - https://developer.android.com/guide/webapps/webview.html
        - https://d3js.org/
         */
        Log.d(TAG, "updatePointsView()");
        typeTextView.setText(mClimbType.title);

        if(mDateRange != ChronoUnit.FOREVER) {
            mSessionLayout.setVisibility(View.VISIBLE);
            mNonSessionLayout.setVisibility(View.GONE);


            // Setup data for the outer chart -- POINTS
            PieData data = mClimbStats.getPieData(ClimbStats.StatType.POINTS);
            data.setDrawValues(false);
            mPieChartOuter.setData(data);
            mPieChartOuter.highlightValue(1, 0, false); // highlight the current value

            // Setup data for the middle chart -- NUMBER OF CLIMBS
            data = mClimbStats.getPieData(ClimbStats.StatType.CLIMBS);
            data.setDrawValues(false);
            mPieChartMiddle.setData(data);
            mPieChartMiddle.highlightValue(1, 0, false);

            // Setup data for the inner chart -- MAX GRADE
            data = mClimbStats.getPieData(ClimbStats.StatType.GRADE);
            data.setDrawValues(false);
            mPieChartInner.setData(data);
            mPieChartInner.highlightValue(1, 0, false);

            mPieChartInner.setCenterText(mClimbStats.getCenterText(mCurrentStat));

            // animate the charts
            mPieChartInner.animateY(500, Easing.EasingOption.EaseInOutQuad);
            mPieChartOuter.animateY(500, Easing.EasingOption.EaseInOutQuad);
            mPieChartMiddle.animateY(500, Easing.EasingOption.EaseInOutQuad);

            //mPieChartInner.invalidate(); // refresh
        } else {
            mSessionLayout.setVisibility(View.GONE);
            mNonSessionLayout.setVisibility(View.VISIBLE);

            //TODO: implement this part
            /*float avgPPS = 0;
            if (mNumClimbs != 0) {
                // loop through to get the number of unique days in the result
                HashSet<Date> dates = new HashSet<>(); // use set to get only unique dates
                for (Climb climb : mResult) {
                    dates.add(Shared.getStartofDate(climb.getDate()));
                }
                avgPPS = mNumPoints / dates.size();
            }

            if (mMaxGrade == null) {
                maxTextView.setVisibility(View.GONE);
            } else {
                maxTextView.setVisibility(View.VISIBLE);
                List<String> gradeList = mClimbType.grades;

                maxTextView.setText(String.format("MAX: %s", gradeList.get(mMaxGrade.intValue())));
            }

            pointsTextView.setText(String.format("POINTS: %d", mNumPoints));
            countTextView.setText(String.format("CLIMBS: %d", mNumClimbs));
            ppsAvgTextView.setText(String.format("PPS AVG: %.1f", avgPPS));*/
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
