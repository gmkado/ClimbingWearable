package com.example.mysynclibrary.goalDAO;

import android.util.Pair;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.realm.Attempt;
import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.realm.Goal;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.github.mikephil.charting.utils.EntryXComparator;

import org.threeten.bp.Duration;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.temporal.WeekFields;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmQuery;
import io.realm.RealmResults;


/**
 * Created by Grant on 6/3/2017.
 */

public class CustomGoalDAO extends GoalDAO {
    public static final String TYPE = "Custom";

    private RealmResults<Attempt> mResults;
    private Goal mGoal;

    private ArrayList<BarEntry> mBarEntries;

    private float mNonRecurrTsScale;
    private float mNonRecurrTsOffset;
    private ArrayList<Date> mRecurrDateList;

    private ArrayList<Entry> mScatterEntries;
    private ArrayList<Entry> mLineEntries;
    private ZonedDateTime recurringStartZDT;
    private ZonedDateTime recurringEndZDT;

    /* Recurring goal fields */
    private int mCurrentStreak;
    private int mLongestStreak;
    private int mSuccessfulPeriodCount;
    private int mNonRecurringProgress;
    private ZonedDateTime nonRecurringStartZDT;
    private ZonedDateTime nonRecurringEndZDT;


    public CustomGoalDAO(Goal goal) {
        super();
        mGoal = goal;

        // TODO: does this need to be removed somewhere?  If I have a RecyclerView.Adapter wrap around a list of GoalDAO's,
        // they should be strong references and not removed unless the entire fragment is destroyed
        mGoal.addChangeListener(new RealmChangeListener<Goal>() {
            @Override
            public void onChange(Goal element) {
                if(element.isValid()) { // Otherwise we deleted it
                    // rerun the query
                    getClimbsAndDateRangeFromGoalCriteria();
                    calculateStats();
                }
            }
        });
        getClimbsAndDateRangeFromGoalCriteria();
        calculateStats();
    }

    public static CustomGoalDAO
    getGoalDAOFromID(String goalUUID) {
        // create goal from UUID
        return new CustomGoalDAO(Realm.getDefaultInstance().where(Goal.class).equalTo("id", goalUUID).findFirst());
    }

    @Override
    public String getSummary() {
        return mGoal.getSummary();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean isRecurring() {
        return mGoal.isRecurring();
    }

    @Override
    public int getNonrecurringProgress() {
        return mNonRecurringProgress;
    }

    @Override
    public int getTarget() {
        return mGoal.getTarget();
    }

    @Override
    public float getRecurringPercent() {
        if(isRecurring()) {
            return 1.0f * mSuccessfulPeriodCount/mRecurrDateList.size();
        }else {
            return 0;
        }
    }

    @Override
    public CombinedData getNonrecurringChartData() {
        CombinedData data = new CombinedData();
        switch(mGoal.getGoalUnit()) {
            case CLIMBS:
                data.setData(getLineData());

                break;
            case HEIGHT:
                // TODO: fill this in
                break;
            case POINTS:
                data.setData(getLineData());
                break;
        }
        data.setData(getScatterData());
        return data;
    }

    @Override
    public DateFormat getNonrecurringDateFormat() {
        return SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    }

    @Override
    public AxisValueFormatter getNonrecurringXFormatter() {
        return new AxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                Date date = XValueToDate(value, false);
                Duration duration = Duration.between(nonRecurringStartZDT, nonRecurringEndZDT == null? ZonedDateTime.now() : nonRecurringEndZDT); // TODO: should only do this comparison once in calcNonrecurringStats
                if(duration.compareTo(Duration.ofDays(1))<=0) {
                    return SimpleDateFormat.getTimeInstance(DateFormat.SHORT).format(date);
                }else {
                    return SimpleDateFormat.getDateInstance(DateFormat.SHORT).format(date);
                }
            }

            @Override
            public int getDecimalDigits() {
                return 0;
            }
        };
    }

    @Override
    public AxisValueFormatter getYFormatter() {
        return new AxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    return mGoal.getClimbType().grades.get((int) value);
                }

                @Override
                public int getDecimalDigits() {
                    return 0;
                }
            };
    }

    @Override
    public CombinedData getRecurringChartData() {
        CombinedData data = new CombinedData();
        data.setData(getBarData());

        return data;
    }

    @Override
    public AxisValueFormatter getRecurringXFormatter() {
        return new AxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                if(value>=0 && value < mRecurrDateList.size()) {
                    Date date = XValueToDate(value, true);
                    return SimpleDateFormat.getDateInstance(DateFormat.SHORT).format(date);
                }else {
                    return "";
                }

            }

            @Override
            public int getDecimalDigits() {
                return 0;
            }
        };
    }

    @Override
    public Pair<ZonedDateTime, ZonedDateTime> getDateRange(boolean recurring) {
        if(recurring) {
            return new Pair<>(recurringStartZDT,recurringEndZDT == null ? ZonedDateTime.now() : recurringEndZDT );

        }else {
            return new Pair<>(nonRecurringStartZDT,nonRecurringEndZDT == null ? ZonedDateTime.now() : nonRecurringEndZDT );
        }
    }

    public String getID() {
        return mGoal.getUUID();
    }

    /**
     * Return the date for the given xvalue
     * TODO: If recurring is true, return the date for the recurring graph, otherwise return the date for the nonrecurring graph
     */
    private Date XValueToDate(float value, boolean recurring) {
        if(recurring) {
            return mRecurrDateList.get((int) value);
        }else {
            Date date = new Date();
            date.setTime((long) (value * mNonRecurrTsScale + mNonRecurrTsOffset));
            return date;
        }
    }

    /**
     * Return the xvalue for the given date
     * TODO: If recurring is true, return the xvalue for the recurring graph, otherwise return the date for the nonrecurring graph
     */
    private float dateToXValue(Date date) {
        return (date.getTime() - mNonRecurrTsOffset) / mNonRecurrTsScale;  // subtract reference timestamp to deal with smaller numbers

    }

    private void getClimbsAndDateRangeFromGoalCriteria() {
        try (Realm realm = Realm.getDefaultInstance()) {
            // No need to close the Realm instance manually since this is wrapped in try statement
            // https://realm.io/docs/java/latest/#closing-realm-instances

            // query for distinct session dates and add constraint to query for "numperiod" sessions
            RealmResults<Attempt> results = realm.where(Attempt.class).distinct("date");
            ArrayList<Date> sessionDates = new ArrayList<>();
            for (Attempt attempt : results) {
                sessionDates.add(attempt.getDatetime());
            }


            /******************** set the end criteria **********************/
            recurringStartZDT = Shared.DateToZDT(mGoal.getStartDate());
            switch (mGoal.getEndType()) {
                case NO_END:
                    recurringEndZDT = null;
                    break;
                case DATE:
                    recurringEndZDT = Shared.DateToZDT(mGoal.getEndDate());
                    break;
                case PERIOD:
                    if (mGoal.getPeriod() == Goal.Period.SESSION) {
                        if (mGoal.getNumPeriods() > sessionDates.size()) {
                            // we haven't reached the end, so use today
                            recurringEndZDT = null;
                        } else {
                            recurringEndZDT = Shared.DateToZDT(sessionDates.get(mGoal.getNumPeriods())); // TODO: is this off by 1?
                        }
                    } else {
                        switch (mGoal.getPeriod()) {
                            case WEEKLY:
                                recurringStartZDT = recurringStartZDT.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
                                break;
                            case MONTHLY:
                                recurringStartZDT = recurringStartZDT.withDayOfMonth(1);
                                break;
                            case YEARLY:
                                recurringStartZDT = recurringStartZDT.withDayOfYear(1);
                                break;
                            default:
                                throw new IllegalArgumentException("Got invalid goal period");
                        }
                        // add "numperiod" increments of "unit" and add constraint to query
                        recurringEndZDT = recurringStartZDT.plus(mGoal.getNumPeriods(), mGoal.getPeriod().unit);
                        if (recurringEndZDT.isAfter(ZonedDateTime.now())) {
                            recurringEndZDT = ZonedDateTime.now();
                        }
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Got invalid end type");
            }


            if(!mGoal.isRecurring()) {
                // Actually don't have recurring data so these ZDTs are the nonrecurring date range
                nonRecurringStartZDT = recurringStartZDT;
                nonRecurringEndZDT = recurringEndZDT;
                if(listener!=null) {
                    listener.onNonrecurringDateRangeChanged(); // notify parent to react to new data and date ranges
                }
            }else {
                if(listener!=null) {
                    listener.onRecurringDateRangeChanged(); // notify parent to react to new data and date ranges
                }
            }

            /* Use fields to query for appropriate climbs*/
            RealmQuery<Attempt> query = realm.where(Attempt.class)
                    .greaterThanOrEqualTo("datetime", mGoal.getStartDate())
                    .equalTo("climb.type", mGoal.getClimbType().ordinal());
            if(recurringEndZDT != null) {
                query.lessThanOrEqualTo("datetime", Shared.ZDTToDate(recurringEndZDT));
            }
            mResults = query.findAll();
            mResults.addChangeListener(new RealmChangeListener<RealmResults<Attempt>>() {
                @Override
                public void onChange(RealmResults<Attempt> element) {
                    calculateStats();
                }
            });
        }
    }

    private void calculateStats() {
        if(mGoal.isRecurring()) {
            calculateRecurringStats();

            setNonrecurringDateRangeFromIndex(mBarEntries.size()-1);
            calculateNonrecurringStats();
        }else {
            calculateNonrecurringStats();
        }
    }

    /**
     * Run a query for the given goal and calculate the result
     */
    private void calculateRecurringStats() {
        /*
         * Step through from recurringStartZDT:period:recurringEndZDT and add the appropriate stat to barentry
         * Also keep track of the current and longest streak
         * TODO: add keeping track of max/average (see ClimbStats)
         */
        mBarEntries = new ArrayList<>();
        mRecurrDateList = new ArrayList<>();
        ZonedDateTime currZDT = recurringStartZDT;

        ZonedDateTime endZDT  = recurringEndZDT == null ? ZonedDateTime.now() : recurringEndZDT;
        while (currZDT.isBefore(endZDT)) {
            float xValue = mRecurrDateList.size();
            mRecurrDateList.add(Shared.ZDTToDate(currZDT));

            ZonedDateTime nextZDT = currZDT.plus(1, mGoal.getPeriod().unit);
            // Get the climbs in this period
            RealmResults sessionResult = mResults.where()
                    .between("date", Shared.ZDTToDate(currZDT), Shared.ZDTToDate(nextZDT))
                    .greaterThanOrEqualTo("grade", mGoal.getMingrade())
                    .findAll();
            if (!sessionResult.isEmpty()) {
                switch (mGoal.getGoalUnit()) {
                    case POINTS:
                        int sessionPoints = sessionResult.sum("grade").intValue();
                        mBarEntries.add(new BarEntry(xValue, sessionPoints));
                        if (sessionPoints >= mGoal.getTarget()) {
                            mSuccessfulPeriodCount++;
                            mCurrentStreak++;
                            if (mCurrentStreak > mLongestStreak) {
                                mLongestStreak = mCurrentStreak;
                            }
                        } else {
                            mCurrentStreak = 0;
                        }
                        break;
                    case CLIMBS:
                        int sessionClimbs = sessionResult.size();
                        mBarEntries.add(new BarEntry(xValue, sessionClimbs));
                        if (sessionClimbs >= mGoal.getTarget()) {
                            mSuccessfulPeriodCount++;
                            mCurrentStreak++;
                            if (mCurrentStreak > mLongestStreak) {
                                mLongestStreak = mCurrentStreak;
                            }
                        } else {
                            mCurrentStreak = 0;
                        }
                        break;
                    case HEIGHT:
                        // TODO: fill this in
                        break;
                }
            } else {
                // no climbs in this range, so add 0
                mBarEntries.add(new BarEntry(xValue, 0));
            }

            currZDT = nextZDT;
        }
        if(listener!=null) {
            listener.onRecurringStatsChange(); // notify parent to react to new data and date ranges
        }
    }

    private void setNonrecurringDateRangeFromIndex(int index) {
        // calculate the nonrecurring stats from the recurring index
        nonRecurringStartZDT = Shared.DateToZDT(XValueToDate(index, true));
        nonRecurringEndZDT = nonRecurringStartZDT.plus(1, mGoal.getPeriod().unit);

        if(listener!=null) {
            listener.onNonrecurringDateRangeChanged(); // notify parent to react to new data and date ranges
        }
    }

    private void calculateNonrecurringStats() {
        // start should be zero
        Date start = Shared.ZDTToDate(nonRecurringStartZDT);
        Date end = Shared.ZDTToDate(nonRecurringEndZDT == null ? ZonedDateTime.now(): nonRecurringEndZDT);
        mNonRecurrTsOffset = start.getTime();
        mNonRecurrTsScale = 1000*60;  // 1 minutes per tic

        // if duration is less than one day, timescale should be in minutes
        // otherwise set timescale to days

        // Loop through the climbs from start to end and add the appropriate stat to line or scatter entry
        mLineEntries = new ArrayList<>();
        mScatterEntries = new ArrayList<>();
        RealmResults<Attempt> attempts = mResults.where().greaterThanOrEqualTo("datetime",start).lessThanOrEqualTo("datetime",end).findAll();

        mNonRecurringProgress = 0;
        // populate graph data
        for(Attempt attempt:attempts) {
            float xValue = dateToXValue(attempt.getDatetime());
            Climb climb =attempt.getClimb();

            mScatterEntries.add(new Entry(xValue, climb.getGrade()));
            if(attempt.getClimb().getGrade()>=mGoal.getMingrade()) {  // only count the climb if its > mingrade
                switch (mGoal.getGoalUnit()) {
                    case POINTS:
                        mNonRecurringProgress += climb.getGrade();
                        mLineEntries.add(new Entry(xValue, attempts.where().lessThanOrEqualTo("date", attempt.getDatetime()).sum("grade").intValue()));
                        break;
                    case CLIMBS:
                        mNonRecurringProgress ++;
                        mLineEntries.add(new Entry(xValue, attempts.where().lessThanOrEqualTo("date", attempt.getDatetime()).count()));
                        break;
                    case HEIGHT:
                        // TODO: fill this in
                        break;
                }
            }
        }

        if(listener!=null) {
            listener.onNonrecurringStatsChanged(); // notify parent to react to new data and date ranges
        }
    }

    public Goal getGoal() {
        return mGoal;
    }

    private ScatterData getScatterData(){
        // loop through each climb and add as an entry
        ScatterData data = new ScatterData();
        ScatterDataSet set = new ScatterDataSet(mScatterEntries, "scatter");
        set.setScatterShapeSize(8f);
        set.setAxisDependency(YAxis.AxisDependency.RIGHT);
        set.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        set.setDrawValues(false);
        data.addDataSet(set);
        return data;

    }

    private LineData getLineData(){
        // ensure that they are sorted
        Collections.sort(mLineEntries, new EntryXComparator());

        LineData data = new LineData();
        LineDataSet dataSet = new LineDataSet(mLineEntries, "line");
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(false);
        dataSet.setDrawFilled(true);
        dataSet.setLineWidth(3f);
        dataSet.setMode(LineDataSet.Mode.LINEAR );
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        data.addDataSet(dataSet);

        return data;
    }

    private BarData getBarData() {
        // ensure that they are sorted
        Collections.sort(mBarEntries, new EntryXComparator());

        BarData data = new BarData();
        BarDataSet dataSet = new BarDataSet(mBarEntries, "bar");
        dataSet.setDrawValues(false);
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        data.addDataSet(dataSet);
        data.setBarWidth(0.9f);

        return data;
    }

}
