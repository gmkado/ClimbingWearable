package com.example.grant.wearableclimbtracker;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.eventbus.RealmResultsEvent;
import com.example.mysynclibrary.realm.Climb;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.EntryXComparator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.temporal.ChronoUnit;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


import io.realm.RealmChangeListener;
import io.realm.RealmResults;

/**
 * Created by Grant on 10/17/2016.
 */
public class HistoryChartMobileFragment extends Fragment  {
    private static final String TAG = "HistChrtMobFragment";
    private BarChart mBarChart;
    private RealmResults<Climb> mResult;
    private Shared.ClimbType mClimbType;
    private ChronoUnit mDateRange;
    private SimpleDateFormat mDateFormat;
    private float mBarWidth;
    private int mDateOffset;

    public HistoryChartMobileFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Log.d(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_barchart_mobile, container, false);
        mBarChart = (BarChart)rootView.findViewById(R.id.chart);

        // create a custom MarkerView (extend MarkerView) and specify the layout to use for it
        CustomMarkerView mv = new CustomMarkerView(getContext(), R.layout.custom_marker_view);
        mBarChart.setDrawMarkerViews(true);
        mBarChart.setMarkerView(mv); // Set the marker to the chart
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

        if (mResult != null) {
            mResult.removeChangeListeners();
        }
    }

    @Subscribe (sticky = true)
    public void onRealmResult(RealmResultsEvent event) {
        mResult = event.realmResults;
        mClimbType = event.climbType;
        mDateRange = event.dateRange;
        mDateOffset = event.dateOffset;

        mResult.addChangeListener(new RealmChangeListener<RealmResults<Climb>>() {
            @Override
            public void onChange(RealmResults<Climb> element) {
                setUpBarChart();
            }
        });

        setUpBarChart();
    }


    private void setUpBarChart() {
        Log.d(TAG, "setUpBarChart");

        if (mResult.isEmpty()) {
            mBarChart.setData(null);
        } else {

            // setup bins based on daterange
            final ArrayList<Date> bins = getBins();


            //loop through each bin, query the results for a count or sum of each difficulty, and add to barEntries
            List<BarEntry> barEntries = new ArrayList<>();
            // fix so it shows actual easy/med/hard routes, add definition to enum
            for (int i=0; i<bins.size(); i++) {
                Date fromDate = bins.get(i);
                if(i+1 == bins.size()) {
                    // this is the last element, so get all results after this
                    barEntries.add(new BarEntry(i,
                            new float[] {
                                    mResult.where().greaterThan("date",fromDate).lessThanOrEqualTo("grade", mClimbType.getMaxGradeInd(Shared.ClimbLevel.beginner)).count(),
                                    mResult.where().greaterThan("date",fromDate).between("grade", mClimbType.getMaxGradeInd(Shared.ClimbLevel.beginner)+1, mClimbType.getMaxGradeInd(Shared.ClimbLevel.intermediate)).count(),
                                    mResult.where().greaterThan("date",fromDate).between("grade", mClimbType.getMaxGradeInd(Shared.ClimbLevel.intermediate)+1, mClimbType.getMaxGradeInd(Shared.ClimbLevel.advanced)).count(),
                                    mResult.where().greaterThan("date",fromDate).greaterThan("grade", mClimbType.getMaxGradeInd(Shared.ClimbLevel.advanced)).count()}));
                }else {
                    Date toDate = bins.get(i+1);
                    barEntries.add(new BarEntry(i,
                            new float[] {
                                    mResult.where().between("date",fromDate, toDate).lessThanOrEqualTo("grade", mClimbType.getMaxGradeInd(Shared.ClimbLevel.beginner)).count(),
                                    mResult.where().between("date",fromDate, toDate).between("grade", mClimbType.getMaxGradeInd(Shared.ClimbLevel.beginner)+1, mClimbType.getMaxGradeInd(Shared.ClimbLevel.intermediate)).count(),
                                    mResult.where().between("date",fromDate, toDate).between("grade", mClimbType.getMaxGradeInd(Shared.ClimbLevel.intermediate)+1, mClimbType.getMaxGradeInd(Shared.ClimbLevel.advanced)).count(),
                                    mResult.where().between("date",fromDate, toDate).greaterThan("grade", mClimbType.getMaxGradeInd(Shared.ClimbLevel.advanced)).count()}));
                }


            }

            // ensure that they are sorted
            Collections.sort(barEntries, new EntryXComparator());

            BarDataSet dataSet = new BarDataSet(barEntries, "climbs");
            dataSet.setColors(getColors());
            dataSet.setDrawValues(false);

            ArrayList<IBarDataSet> dataSetList = new ArrayList<>();
            dataSetList.add(dataSet); // add the dataset

            // format the chart
            final DateFormat finalDf = mDateFormat;
            AxisValueFormatter formatter = new AxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    if((int)value<0 || (int)value > bins.size()-1)
                        return "WTF";
                    return finalDf.format(bins.get((int)value));
                }

                @Override
                public int getDecimalDigits() {
                    return 0;
                }
            };

            // set data
            XAxis xAxis = mBarChart.getXAxis();
            xAxis.setValueFormatter(formatter);
            xAxis.setGranularity(1f);
            xAxis.setLabelCount(bins.size());
            xAxis.setTextColor(Color.BLACK);
            xAxis.setDrawGridLines(false);

            YAxis yAxis = mBarChart.getAxisLeft();
            yAxis.setTextColor(Color.BLACK);
            yAxis.setGranularity(1f);
            yAxis.setAxisMinValue(0);
            yAxis.setDrawGridLines(false);

            mBarChart.getAxisRight().setEnabled(false);

            mBarChart.setDescription("");
            mBarChart.getLegend().setEnabled(false);

            BarData data = new BarData(dataSetList);

            data.setBarWidth(0.9f);//mBarWidth);
            //data.setHighlightEnabled(true);
            mBarChart.setFitBars(true);
            mBarChart.setData(data);
        }
        mBarChart.invalidate(); // refresh
    }

    private ArrayList<Date> getBins() {
        ZonedDateTime fromZDT, toZDT;
        ChronoUnit displayUnit;
        if(mDateRange == ChronoUnit.DAYS) {
            fromZDT =  DateTimeUtils.toInstant(mResult.first().getDate()).atZone(ZoneOffset.UTC);
            toZDT = DateTimeUtils.toInstant(mResult.last().getDate()).atZone(ZoneOffset.UTC);
            displayUnit = ChronoUnit.DAYS;
        }else if(mDateRange == ChronoUnit.FOREVER) {
            fromZDT =  DateTimeUtils.toInstant(mResult.first().getDate()).atZone(ZoneOffset.UTC);
            toZDT = DateTimeUtils.toInstant(mResult.last().getDate()).atZone(ZoneOffset.UTC);

            // get the bins based on the oldest date
            ZonedDateTime toZDTCopy;
            if(fromZDT.isBefore(toZDT)){
                toZDTCopy = toZDT.minus(1, ChronoUnit.WEEKS);
                if(fromZDT.isBefore(toZDTCopy)) {
                    // first date was older than this week
                    toZDTCopy = toZDT.minus(1, ChronoUnit.MONTHS);
                    if(fromZDT.isBefore(toZDTCopy)) {
                        // first date was older than this month
                        toZDTCopy = toZDT.minus(1, ChronoUnit.YEARS);
                        if(fromZDT.isBefore(toZDTCopy)) {
                            // first date was older than this year
                            displayUnit = ChronoUnit.FOREVER;

                        }else{
                            // first date was this year
                            displayUnit = ChronoUnit.YEARS;

                        }
                    }else{
                        // first date was this month
                        displayUnit = ChronoUnit.MONTHS;
                    }
                }else{
                    displayUnit = ChronoUnit.WEEKS;
                }
            }else {
                // first date was today
                displayUnit = ChronoUnit.DAYS;
            }
        }else{
            // if forever, get
            Pair<Date,Date> datePairs = Shared.getDatesFromRange(mDateRange, mDateOffset);
            fromZDT = DateTimeUtils.toInstant(datePairs.first).atZone(ZoneOffset.UTC);
            toZDT = DateTimeUtils.toInstant(datePairs.second).atZone(ZoneOffset.UTC);
            displayUnit = mDateRange;
        }
        // condition fromCal and toCal, then add Date objects to the resultLIst until fromCal>toCal
        ArrayList<Date> dateList = new ArrayList<>();
        switch(displayUnit) {
            case DAYS:
                // use hours as the bins, with the earliest dataentry as the starting hour
                fromZDT = fromZDT.truncatedTo(ChronoUnit.HOURS);
                while (fromZDT.isBefore(toZDT)) {
                    dateList.add(DateTimeUtils.toDate(fromZDT.toInstant()));
                    fromZDT = fromZDT.plus(30, ChronoUnit.MINUTES);
                }
                mDateFormat = new SimpleDateFormat("hh:mm a");
                mBarWidth = ChronoUnit.MINUTES.getDuration().toMillis()*30*0.9f;
                break;
            case WEEKS:
                while (fromZDT.isBefore(toZDT)) {
                    dateList.add(DateTimeUtils.toDate(fromZDT.toInstant()));
                    fromZDT = fromZDT.plus(1, ChronoUnit.DAYS);
                }
                mDateFormat = new SimpleDateFormat("EEE");
                mBarWidth = ChronoUnit.DAYS.getDuration().toMillis()*0.9f;
                break;
            case MONTHS:
                // each week, ending with this week
                while (fromZDT.isBefore(toZDT)) {
                    dateList.add(DateTimeUtils.toDate(fromZDT.toInstant()));
                    fromZDT = fromZDT.plus(1, ChronoUnit.WEEKS);
                }
                mDateFormat = new SimpleDateFormat("MM/dd");
                mBarWidth = ChronoUnit.WEEKS.getDuration().toMillis()*0.9f;
                break;
            case YEARS:
                // each month, ending with this month
                while (fromZDT.isBefore(toZDT)) {
                    dateList.add(DateTimeUtils.toDate(fromZDT.toInstant()));
                    fromZDT = fromZDT.plus(1, ChronoUnit.MONTHS);
                }
                mDateFormat = new SimpleDateFormat("MMM");
                mBarWidth = ChronoUnit.MONTHS.getDuration().toMillis()*0.9f;
                break;
            case FOREVER:
                // if we've gotten here, that means the data is older than a year.  use each year, ending with this year
                while (fromZDT.isBefore(toZDT)) {
                    dateList.add(DateTimeUtils.toDate(fromZDT.toInstant()));
                    fromZDT = fromZDT.plus(1, ChronoUnit.YEARS);
                }
                mDateFormat = new SimpleDateFormat("yyyy");
                mBarWidth = ChronoUnit.YEARS.getDuration().toMillis()*0.9f;
                break;
            default:
                break;
        }
        return dateList;
    }

    private int[] getColors() {

        int stacksize = 4;

        // have as many colors as stack-values per entry
        int[] colors = new int[stacksize];

        for (int i = 0; i < colors.length; i++) {
            colors[i] = ColorTemplate.MATERIAL_COLORS[i];
        }

        return colors;

    }

}
