package com.example.mysynclibrary.goalDAO;

import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.realm.Goal;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.formatter.AxisValueFormatter;

import java.text.DateFormat;

import io.realm.RealmResults;

/**
 * Created by Grant on 6/3/2017.
 */

public class ProjectGoalDAO extends GoalDAO{
    public static final String TYPE = "Goal";

    public ProjectGoalDAO(){
        super();
    }

    @Override
    public int getNonrecurringProgress() {
        return 0;
    }

    @Override
    public int getNonrecurringTarget() {
        return 0;
    }

    @Override
    public String getSummary() {
        return "Complete all projects";
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean isRecurring() {
        return false;
    }

    @Override
    public float getRecurringPercent() {
        return 0; // not applicable since project goal is nonrecurring
    }

    @Override
    public CombinedData getNonrecurringChartData() {
        return null;
    }

    @Override
    public DateFormat getNonrecurringDateFormat() {
        return null;
    }

    @Override
    public AxisValueFormatter getNonrecurringXFormatter() {
        return null;
    }

    @Override
    public AxisValueFormatter getNonrecurringYFormatter() {
        return null;
    }

}
