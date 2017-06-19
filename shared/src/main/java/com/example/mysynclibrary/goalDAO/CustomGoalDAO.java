package com.example.mysynclibrary.goalDAO;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.realm.Goal;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.YAxis;
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
import io.realm.RealmResults;

import static com.example.mysynclibrary.ClimbStats.StatType.GRADE;

/**
 * Created by Grant on 6/3/2017.
 */

public class CustomGoalDAO extends GoalDAO {
    public static final String TYPE = "Custom";

    private RealmResults<Climb> mResults;
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
    private int mTotalPeriodCount;
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
                    calculateStats();
                }
            }
        });
        calculateStats();
    }

    public static CustomGoalDAO getGoalDAOFromID(String goalUUID) {
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
    public int getNonrecurringTarget() {
        return mGoal.getTarget();
    }

    @Override
    public float getRecurringPercent() {
        if(isRecurring()) {
            return 1.0f * mSuccessfulPeriodCount/mTotalPeriodCount;
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
            case GRADE:
                data.setData(getScatterData());
                break;
            case HEIGHT:
                // TODO: fill this in
                break;
            case POINTS:
                data.setData(getLineData());
                break;
        }
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
                Duration duration = Duration.between(nonRecurringStartZDT, nonRecurringEndZDT); // TODO: should only do this comparison once in calcNonrecurringStats
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
    public AxisValueFormatter getNonrecurringYFormatter() {
        if(mGoal.getGoalUnit() == Goal.GoalUnit.GRADE) {
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
        }else {
            return null;
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

    private void setup() {
        try (Realm realm = Realm.getDefaultInstance()) {
            // No need to close the Realm instance manually since this is wrapped in try statement
            // https://realm.io/docs/java/latest/#closing-realm-instances
            // tODO: any reason not to query all climbs like done here?
            mResults = realm.where(Climb.class)
                    .greaterThanOrEqualTo("date", mGoal.getStartDate())
                    .equalTo("type", mGoal.getClimbType().ordinal())
                    .findAll();

            // query for distinct session dates and add constraint to query for "numperiod" sessions
            RealmResults<Climb> results = realm.where(Climb.class).distinct("sessionDate");
            ArrayList<Date> sessionDates = new ArrayList<>();
            for (Climb climb : results) {
                sessionDates.add(climb.getDate());
            }


            /******************** set the end criteria **********************/
            recurringStartZDT = Shared.DateToZDT(mGoal.getStartDate());
            switch (mGoal.getEndType()) {
                case NO_END:
                    recurringEndZDT = ZonedDateTime.now();
                    break;
                case DATE:
                    recurringEndZDT = Shared.DateToZDT(mGoal.getEndDate());
                    break;
                case PERIOD:
                    if (mGoal.getPeriod() == Goal.Period.SESSION) {
                        if (mGoal.getNumPeriods() > sessionDates.size()) {
                            // we haven't reached the end, so use today
                            recurringEndZDT = ZonedDateTime.now();
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
        }
    }

    private void calculateStats() {
        setup();
        if(mGoal.isRecurring()) {
            calculateRecurringStats();
            calculateNonRecurringStats(mBarEntries.size()-1);
        }else {
            nonRecurringStartZDT = recurringStartZDT;
            nonRecurringEndZDT = recurringEndZDT;
            calculateNonRecurringStats();
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

        mTotalPeriodCount = 0;
        while (currZDT.isBefore(recurringEndZDT)) {
            float xValue = mTotalPeriodCount++;
            mRecurrDateList.add(Shared.ZDTToDate(currZDT));

            ZonedDateTime nextZDT = currZDT.plus(1, mGoal.getPeriod().unit);

            // Get the climbs in this period
            RealmResults sessionResult = mResults.where()
                    .between("date", Shared.ZDTToDate(currZDT), Shared.ZDTToDate(nextZDT)).findAll();
            if (!sessionResult.isEmpty()) {
                switch (mGoal.getGoalUnit()) {
                    case GRADE:
                        // get number of grades > target
                        long count = mResults.where().greaterThan("grade", mGoal.getTarget()).count();
                        mBarEntries.add(new BarEntry(xValue, count));
                        if (count != 0) {
                            mSuccessfulPeriodCount++;
                            mCurrentStreak++;
                            if (mCurrentStreak > mLongestStreak)
                                mLongestStreak = mCurrentStreak;
                        } else {
                            mCurrentStreak = 0;
                        }
                        break;
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
    }

    private void calculateNonRecurringStats(int index) {
        // calculate the nonrecurring stats from the recurring index
        nonRecurringStartZDT = Shared.DateToZDT(XValueToDate(index, true));
        nonRecurringEndZDT = nonRecurringStartZDT.plus(1, mGoal.getPeriod().unit);
        calculateNonRecurringStats();
    }

    private void calculateNonRecurringStats() {
        // start should be zero
        Date start = Shared.ZDTToDate(nonRecurringStartZDT);
        Date end = Shared.ZDTToDate(nonRecurringEndZDT);
        mNonRecurrTsOffset = start.getTime();
        mNonRecurrTsScale = 1000*60;  // 1 minutes per tic

        // if duration is less than one day, timescale should be in minutes
        // otherwise set timescale to days

        // Loop through the climbs from start to end and add the appropriate stat to line or scatter entry
        mLineEntries = new ArrayList<>();
        mScatterEntries = new ArrayList<>();
        RealmResults<Climb> climbs = mResults.where().greaterThanOrEqualTo("date",start).lessThanOrEqualTo("date",end).findAll();
        switch(mGoal.getGoalUnit()) {
            case GRADE:
                mNonRecurringProgress = climbs.max("grade")==null? 0 : climbs.max("grade").intValue();
                break;
            case POINTS:
                mNonRecurringProgress = climbs.sum("grade")==null? 0: climbs.sum("grade").intValue();
                break;
            case CLIMBS:
                mNonRecurringProgress = climbs.size();
                break;
            case HEIGHT:
                // TODO: fill this in
                break;
        }

        // populate graph data
        for(Climb climb:climbs) {
            float xValue = dateToXValue(climb.getDate());
            switch(mGoal.getGoalUnit()) {
                case GRADE:
                    mScatterEntries.add(new Entry(xValue, climb.getGrade()));
                    break;
                case POINTS:
                    mLineEntries.add(new Entry(xValue, climbs.where().lessThanOrEqualTo("date", climb.getDate()).sum("grade").intValue()));
                    break;
                case CLIMBS:
                    mLineEntries.add(new Entry(xValue, climbs.where().lessThanOrEqualTo("date", climb.getDate()).count()));
                    break;
                case HEIGHT:
                    // TODO: fill this in
                    break;
            }
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
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        set.setColors(new int[] {GRADE.basecolor.Accent});
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
        dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        data.addDataSet(dataSet);

        return data;
    }

}
