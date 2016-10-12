package com.example.grant.wearableclimbtracker;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.grant.wearableclimbtracker.model.Climb;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.realm.implementation.RealmBarDataSet;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

/**
 * Created by Grant on 8/1/2016.
 */
public class BarChartFragment extends android.app.Fragment implements RealmChangeListener<Realm> {
    public static final String ARG_CLIMB_TYPE = "climbTypeArg";
    private MainActivity.ClimbType mClimbType;
    private final String TAG = "BarChartFragment";
    private TextView typeTextView;
    private BarChart mBarChart;
    private Realm mRealm;

    @Override
    public void onStop() {
        super.onStop();
        mRealm.removeChangeListener(this);
        mRealm.close();
    }



    public BarChartFragment(){}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_barchart, container, false);

        mBarChart = (BarChart)rootView.findViewById(R.id.gradeBarChart);
        mClimbType = MainActivity.ClimbType.values()[getArguments().getInt(ARG_CLIMB_TYPE)];


        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        mRealm = Realm.getDefaultInstance();
        mRealm.addChangeListener(this);

        setUpBarChart();

    }

    private void setUpBarChart() {

        final List<String> gradeList = mClimbType == MainActivity.ClimbType.bouldering ?
                Arrays.asList(getResources().getStringArray(R.array.bouldering_grades)) :
                Arrays.asList(getResources().getStringArray(R.array.rope_grades));

        List<BarEntry> barEntries = new ArrayList<>();

        // run a query for today
        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // loop through gradelist and save counts
        for (int i = 0; i<gradeList.size(); i++) {

            int count = mRealm.where(Climb.class)
                    .equalTo("type", mClimbType.ordinal())
                    .equalTo("grade", i)
                    .greaterThan("date",cal.getTime())
                    .findAll().size();

            if (count > 0) {
                barEntries.add(new BarEntry(i, count));
            }
        }
        if(barEntries.size() == 0) {
            mBarChart.setData(null);
        }else {
            AxisValueFormatter formatter = new AxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    return gradeList.get((int) value);
                }

                @Override
                public int getDecimalDigits() {
                    return 0;
                }
            };
            BarDataSet dataSet = new BarDataSet(barEntries, "grades");
            dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            dataSet.setDrawValues(false);
            ArrayList<IBarDataSet> dataSetList = new ArrayList<>();
            dataSetList.add(dataSet); // add the dataset

            // set data
            XAxis xAxis = mBarChart.getXAxis();
            xAxis.setValueFormatter(formatter);
            xAxis.setGranularity(1f);
            xAxis.setTextColor(Color.WHITE);
            xAxis.setDrawGridLines(false);

            YAxis yAxis = mBarChart.getAxisLeft();
            yAxis.setTextColor(Color.WHITE);
            yAxis.setGranularity(1f);
            yAxis.setAxisMinValue(0);
            yAxis.setDrawGridLines(false);

            mBarChart.setDescription("");
            mBarChart.getLegend().setEnabled(false);
            BarData data = new BarData(dataSetList);
            data.setBarWidth(0.9f);
            mBarChart.setFitBars(true);
            mBarChart.setData(data);
        }
        mBarChart.invalidate(); // refresh
    }


    @Override
    public void onChange(Realm element) {
        setUpBarChart();

    }
}
