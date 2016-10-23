package com.example.grant.wearableclimbtracker;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mysynclibrary.ClimbResultsProvider;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.model.Climb;
import com.example.mysynclibrary.model.RealmResultsEvent;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.RealmChangeListener;
import io.realm.RealmResults;

/**
 * Created by Grant on 10/17/2016.
 */
public class BarChartMobileFragment extends Fragment {
    private static final String TAG = "BarChartMobileFragment";
    private BarChart mBarChart;
    private RealmResults<Climb> mResult;
    private ClimbResultsProvider mClimbResultsProvider;

    public BarChartMobileFragment() {

    }



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Log.d(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_barchart_mobile, container, false);
        mBarChart = (BarChart)rootView.findViewById(R.id.chart);
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
        mResult.addChangeListener(new RealmChangeListener<RealmResults<Climb>>() {
            @Override
            public void onChange(RealmResults<Climb> element) {
                setUpBarChart();
            }
        });

        setUpBarChart();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        super.onAttach(context);
        try {
            mClimbResultsProvider = (ClimbResultsProvider) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement ClimbResultsProvider");
        }

    }

    private void setUpBarChart() {
        Log.d(TAG, "setUpBarChart");

        if (mResult.isEmpty()) {
            mBarChart.setData(null);
        } else {
            // get the climb type from the data
            final Shared.ClimbType type = mClimbResultsProvider.getType();

            HashMap<Integer, Integer> bins = new HashMap<>();
            for (Climb climb : mResult) {
                Integer grade = climb.getGrade();
                if (!bins.containsKey(grade)) {
                    // set count to 1
                    bins.put(grade, 1);
                } else {
                    // increment count
                    bins.put(grade, bins.get(grade) + 1);
                }
            }

            // add ALL to bar entries
            List<BarEntry> barEntries = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : bins.entrySet()) {
                barEntries.add(new BarEntry(entry.getKey(), entry.getValue()));
            }
            // ensure that they are sorted
            Collections.sort(barEntries, new EntryXComparator());

            BarDataSet dataSet = new BarDataSet(barEntries, "grades");
            dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            dataSet.setDrawValues(false);

            ArrayList<IBarDataSet> dataSetList = new ArrayList<>();
            dataSetList.add(dataSet); // add the dataset

            // format the chart
            AxisValueFormatter formatter = new AxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    return type.grades.get((int) value);
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
            data.setBarWidth(0.9f);
            mBarChart.setFitBars(true);
            mBarChart.setData(data);
        }
        mBarChart.invalidate(); // refresh
    }

}
