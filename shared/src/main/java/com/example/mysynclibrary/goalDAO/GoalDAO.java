package com.example.mysynclibrary.goalDAO;

import android.util.Pair;

import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.realm.Goal;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarLineScatterCandleBubbleData;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.formatter.AxisValueFormatter;

import org.threeten.bp.ZonedDateTime;

import java.text.DateFormat;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmModel;
import io.realm.RealmResults;

/**
 * Created by Grant on 6/1/2017.
 * This class is a wrapper for Goal RealmObjects.  It abstracts the process of querying for the appropriate climbs
 */

public abstract class GoalDAO {
    GoalDAOListener listener;

    GoalDAO() {
        listener = null;
    }

    /**
     *
     * @return the current value for the progress bar
     */
    public abstract int getNonrecurringProgress();

    /**
     *
     * @return the target for the progress bar
     */
    public abstract int getTarget();

    public abstract String getSummary();

    public abstract String getType();

    public abstract boolean isRecurring();

    /**
     * Get the percent success rate for this recurring goal
     * @return
     */
    public abstract float getRecurringPercent();

    public abstract CombinedData getNonrecurringChartData();

    public abstract DateFormat getNonrecurringDateFormat();

    public abstract AxisValueFormatter getNonrecurringXFormatter();

    public abstract AxisValueFormatter getYFormatter();

    public abstract CombinedData getRecurringChartData();

    public abstract AxisValueFormatter getRecurringXFormatter();

    public abstract Pair<ZonedDateTime, ZonedDateTime> getDateRange(boolean recurring);

    public interface GoalDAOListener{
        void onRecurringStatsChange();

        void onNonrecurringStatsChanged();

        void onNonrecurringDateRangeChanged();

        void onRecurringDateRangeChanged();
    }

    public void setGoalListener(GoalDAOListener listener) {
        this.listener = listener;
    }
}
