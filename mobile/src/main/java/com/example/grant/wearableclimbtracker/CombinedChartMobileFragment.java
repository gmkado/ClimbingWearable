package com.example.grant.wearableclimbtracker;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.mysynclibrary.ClimbStats;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.eventbus.ChartEntrySelected;
import com.example.mysynclibrary.eventbus.RealmResultsEvent;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.threeten.bp.temporal.ChronoUnit;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * Created by Grant on 10/17/2016.
 */
public class CombinedChartMobileFragment extends Fragment  {
    private static final String TAG = "HistChrtMobFragment";
    private CombinedChart mCombinedChart;
    private TextView mYAxisLeftLabel;
    private TextView mYAxisRightLabel;
    private ClimbStats mClimbStat;
    private Shared.ClimbType mClimbType;
    private ChronoUnit mDateRange;
    private int mDateOffset;

    public CombinedChartMobileFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Log.d(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_combined_mobile, container, false);
        mCombinedChart = (CombinedChart)rootView.findViewById(R.id.chart);
        mYAxisLeftLabel = (TextView) rootView.findViewById(R.id.yAxisLeft);
        mYAxisRightLabel = (TextView) rootView.findViewById(R.id.yAxisRight);

        // create a custom MarkerView (extend MarkerView) and specify the layout to use for it
        //CustomMarkerView mv = new CustomMarkerView(getContext(), R.layout.custom_marker_view);
        mCombinedChart.setDrawMarkerViews(true);
        //mCombinedChart.setMarkerView(mv); // Set the marker to the chart
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        // subscribe to ClimbRealmResults
        Log.d(TAG, "registering this");
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        // unsubscribe from ClimbRealmResults
        Log.d(TAG, "unregistering this");
        EventBus.getDefault().unregister(this);

    }

    @Subscribe (sticky = true)
    public void onRealmResultsEvent(RealmResultsEvent event) {
        mClimbStat = event.climbstats;
        mClimbType = event.climbType;
        mDateRange = event.dateRange;
        mDateOffset = event.dateOffset;
        setupCombinedChart();
    }

    private void setupCombinedChart() {
        // TODO: a lot of these should be done only once
        Log.d(TAG, "setupCombinedChart");

        if (mClimbStat.getRealmResult().isEmpty()) {
            mCombinedChart.setData(null);
            mYAxisLeftLabel.setVisibility(View.GONE);
            mYAxisRightLabel.setVisibility(View.GONE);
        } else {
            mYAxisLeftLabel.setVisibility(View.VISIBLE);
            mYAxisRightLabel.setVisibility(View.VISIBLE);

            CombinedData data = new CombinedData();

            XAxis xAxis = mCombinedChart.getXAxis();
            xAxis.resetAxisMaxValue(); // reset the axis first so it can be calculated from the data
            xAxis.resetAxisMinValue();
            xAxis.setCenterAxisLabels(true);

            // ---------  Add data -------------------
            mYAxisLeftLabel.setText(getYLeftLabelText());
            mYAxisRightLabel.setText(getYRightLabelText());
            final DateFormat df;
            data.setData(mClimbStat.getScatterData()); // Add this before
            if(mDateRange == ChronoUnit.DAYS) {
                data.setData(mClimbStat.getLineData());
                df = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
            }else {
                data.setData(mClimbStat.getBarData()); // add the bar data only when looking at range of dates

                if(mDateRange == ChronoUnit.WEEKS) {
                    // format with days of week
                    df = new SimpleDateFormat("EEEEE", Locale.getDefault());
                }else {
                    df = SimpleDateFormat.getDateInstance(DateFormat.SHORT);
                }

                // add chartlistener to zoom to day if clicked
                mCombinedChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                    @Override
                    public void onValueSelected(Entry e, Highlight h) {
                        if(mDateRange!=ChronoUnit.DAYS && e.getY()!=0) {
                            ChronoUnit newDateRange = null;
                        /*switch(mDateRange) {
                            case DAYS:
                                return;
                            case WEEKS:
                                newDateRange = ChronoUnit.DAYS;
                                break;
                            case MONTHS:
                                newDateRange = ChronoUnit.WEEKS;
                                break;
                            case YEARS:
                                newDateRange = ChronoUnit.MONTHS;
                                break;
                            case FOREVER:
                                newDateRange = ChronoUnit.YEARS;
                                break;
                        }*/
                            newDateRange = ChronoUnit.DAYS;

                            // get the date
                            Date selectedDate = mClimbStat.XValueToDate(e.getX());
                            EventBus.getDefault().post(new ChartEntrySelected(selectedDate, newDateRange));
                        }
                    }

                    @Override
                    public void onNothingSelected() {

                    }
                });
            }
            AxisValueFormatter formatter = new AxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    Date date;
                    if(mDateRange!= ChronoUnit.DAYS) {
                         date = mClimbStat.XValueToDate(value + 0.5f); // Fix for rounding error, get label for center of day
                    }else {
                        date = mClimbStat.XValueToDate(value);
                    }
                    return df.format(date);
                }

                @Override
                public int getDecimalDigits() {
                    return 0;
                }
            };

            // setup axis
            xAxis.setValueFormatter(formatter);
            xAxis.setGranularity(1f);
            xAxis.setDrawGridLines(true);
            xAxis.setTextColor(Color.BLACK);
            if(mDateRange == ChronoUnit.DAYS) {
                xAxis.setLabelCount(6, true); // force max label count
            }else {
                xAxis.setLabelCount(6, false);  // return it to default
            }

            YAxis yAxis = mCombinedChart.getAxisLeft();
            yAxis.setTextColor(ClimbStats.StatType.GRADE.color);
            yAxis.setGranularity(1f);
            yAxis.setAxisMinValue(0);
            yAxis.setDrawGridLines(false);
            yAxis.setValueFormatter(new AxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    return mClimbType.grades.get((int) value);
                }

                @Override
                public int getDecimalDigits() {
                    return 0;
                }
            });

            yAxis.removeAllLimitLines(); // TODO: adding limit lines should only be done once
            LimitLine ll = new LimitLine(mClimbStat.getmPrefTargetGrade(), "Target Grade");
            ll.setLineColor(ClimbStats.StatType.GRADE.color);
            //ll.setLineWidth(4f);
            ll.setTextColor(ClimbStats.StatType.GRADE.color);
            ll.setTextSize(12f);
            ll.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_TOP);
            yAxis.addLimitLine(ll);

            yAxis = mCombinedChart.getAxisRight();
            yAxis.setTextColor(Color.BLACK);
            yAxis.setGranularity(1f);
            yAxis.setAxisMinValue(0);
            yAxis.setDrawGridLines(false);

            yAxis.removeAllLimitLines(); // TODO: adding limit lines should only be done once
            ll = new LimitLine(mClimbStat.getmPrefNumClimbs(), "Target # of climbs");
            ll.setLineColor(ClimbStats.StatType.CLIMBS.color);
            //ll.setLineWidth(4f);
            ll.setTextColor(ClimbStats.StatType.CLIMBS.color);
            ll.setTextSize(12f);
            ll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
            yAxis.addLimitLine(ll);

            ll = new LimitLine(mClimbStat.getmPrefNumpoints(), "Target # of v-points");
            ll.setLineColor(ClimbStats.StatType.POINTS.color);
            //ll.setLineWidth(4f);
            ll.setTextColor(ClimbStats.StatType.POINTS.color);
            ll.setTextSize(12f);
            ll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
            yAxis.addLimitLine(ll);

            mCombinedChart.setDescription("");
            mCombinedChart.getLegend().setEnabled(false);
            mCombinedChart.setData(data);

            /*************** FIX UP AXIS LIMITS *****************/
            if(mDateRange == ChronoUnit.DAYS) {
                float buffer = 0.1f*xAxis.mAxisRange;
                xAxis.setAxisMinValue(xAxis.getAxisMinimum() - buffer);
                xAxis.setAxisMaxValue(xAxis.getAxisMaximum()+ buffer);
            }else if(mDateRange!= ChronoUnit.FOREVER) {
                // show the full range of week/month/year
                Pair<Date, Date> xrange = Shared.getDatesFromRange(mDateRange, mDateOffset);
                xAxis.setAxisMinValue(mClimbStat.dateToXValue(xrange.first));
                xAxis.setAxisMaxValue(mClimbStat.dateToXValue(xrange.second));
            }

            mCombinedChart.fitScreen(); // zoom out
            mCombinedChart.getOnTouchListener().setLastHighlighted(null);  // unselect everything
            mCombinedChart.highlightValues(null); // clear highlight
        }
        if(mDateRange == ChronoUnit.DAYS) {
            mCombinedChart.animateY(500);
        }else {
            mCombinedChart.animateXY(1000, 1000); //invalidate(); // refresh
        }
    }

    private SpannableString getYRightLabelText() {
        SpannableString s1, s2;
        if(mDateRange == ChronoUnit.DAYS) {
            s1 = new SpannableString("Cumulative V-Points");
            s2 = new SpannableString("Cumulative # of climbs");
        }else {
            s1 = new SpannableString("Session V-Points");
            s2 = new SpannableString("Session # of climbs");
        }
        s1.setSpan(new ForegroundColorSpan(ClimbStats.StatType.POINTS.color), 0, s1.length(), 0);
        s2.setSpan(new ForegroundColorSpan(ClimbStats.StatType.CLIMBS.color), 0, s2.length(), 0);
        return new SpannableString(TextUtils.concat(s1," ", s2));
    }

    private SpannableString getYLeftLabelText() {
        SpannableString s;
        if(mDateRange == ChronoUnit.DAYS) {
            s = new SpannableString("Grades");
        }else {
            s = new SpannableString("Max Grade per session");
        }
        s.setSpan(new ForegroundColorSpan(ClimbStats.StatType.GRADE.color), 0, s.length(), 0);
        return s;
    }




}
