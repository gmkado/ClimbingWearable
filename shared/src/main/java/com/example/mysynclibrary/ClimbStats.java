package com.example.mysynclibrary;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.util.Log;

import com.example.mysynclibrary.realm.Climb;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.utils.EntryXComparator;

import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.realm.RealmResults;

import static com.example.mysynclibrary.ClimbStats.StatType.CLIMBS;
import static com.example.mysynclibrary.ClimbStats.StatType.GRADE;
import static com.example.mysynclibrary.ClimbStats.StatType.POINTS;

/**
 * Created by Grant on 4/21/2017.
 */

public class ClimbStats {
    private static final String TAG = "ClimbStats";
    private final Shared.ClimbType mClimbType;
    private final ChronoUnit mDateRange;



    private float mTimeStampScale;
    private long mTimeStampOffset;
    private RealmResults<Climb> mResult;
    private ArrayList<Date> mSessionDates;

    private int mTotalPoints;
    private float mAveragePoints;
    private int mMaxPoints;

    private int mTotalClimbs;
    private float mAverageClimbs;
    private int mMaxClimbs;

    private int mNumSessionsGradeGoalReached;
    private Number mMaxGrade;
    private Number mAverageMaxGrade;

    private int mNumSessions;

    public int getmPrefSessionsPerWeek() {
        return mPrefSessionsPerWeek;
    }

    public int getmPrefTargetGrade() {
        return mPrefTargetGrade;
    }

    public int getmPrefNumpoints() {
        return mPrefNumpoints;
    }

    public int getmPrefNumClimbs() {
        return mPrefNumClimbs;
    }

    // preferences
    private int mPrefSessionsPerWeek;
    private int mPrefTargetGrade;
    private int mPrefNumpoints;
    private int mPrefNumClimbs;


    public RealmResults<Climb> getRealmResult() {
        return mResult;
    }


    public int getGoalMultiplier(){
        switch(mDateRange) {
            case DAYS:
                return 1;
            case WEEKS:
                return mPrefSessionsPerWeek;
            case MONTHS:
                return 4 * mPrefSessionsPerWeek;
            case YEARS:
                return 52 * mPrefSessionsPerWeek;
        }
        return -1; // if we got an incorrect date type
    }

    public ChronoUnit getDateRange() {
        return mDateRange;
    }


    public Shared.ClimbType getClimbType() {
        return mClimbType;
    }

    public void updatePreferences(SharedPreferences sharedPref) {
        // get all related prefs
        List<String> gradeList = mClimbType.grades;
        if (mClimbType == Shared.ClimbType.bouldering) {
            mPrefSessionsPerWeek = sharedPref.getInt(Shared.KEY_GOAL_NUMSESSIONS_BOULDER, 0);
            mPrefNumClimbs = sharedPref.getInt(Shared.KEY_GOAL_NUMCLIMBS_BOULDER, 0);
            mPrefNumpoints = sharedPref.getInt(Shared.KEY_GOAL_VPOINTS_BOULDER, 0);
            mPrefTargetGrade = gradeList.indexOf(sharedPref.getString(Shared.KEY_GOAL_GRADE_BOULDER, gradeList.get(0)));
        }else {
            mPrefSessionsPerWeek = sharedPref.getInt(Shared.KEY_GOAL_NUMSESSIONS_ROPES, 0);
            mPrefNumClimbs = sharedPref.getInt(Shared.KEY_GOAL_NUMCLIMBS_ROPES, 0);
            mPrefNumpoints = sharedPref.getInt(Shared.KEY_GOAL_VPOINTS_ROPES, 0);
            mPrefTargetGrade = gradeList.indexOf(sharedPref.getString(Shared.KEY_GOAL_GRADE_ROPES, gradeList.get(0)));
        }

        calculateStats();
    }

    // update a single preference instead of all preferences, returns true if preference was updated
    public boolean updatePreference(SharedPreferences sharedPref, String key) {
        List<String> gradeList = mClimbType.grades;
        if (mClimbType == Shared.ClimbType.bouldering) {
            switch(key) {
                case Shared.KEY_GOAL_NUMSESSIONS_BOULDER:
                    mPrefSessionsPerWeek = sharedPref.getInt(Shared.KEY_GOAL_NUMSESSIONS_BOULDER, 0);
                    break;
                case Shared.KEY_GOAL_NUMCLIMBS_BOULDER:
                    mPrefNumClimbs = sharedPref.getInt(Shared.KEY_GOAL_NUMCLIMBS_BOULDER, 0);
                    break;
                case Shared.KEY_GOAL_VPOINTS_BOULDER:
                    mPrefNumpoints = sharedPref.getInt(Shared.KEY_GOAL_VPOINTS_BOULDER, 0);
                    break;
                case Shared.KEY_GOAL_GRADE_BOULDER:
                    mPrefTargetGrade = gradeList.indexOf(sharedPref.getString(Shared.KEY_GOAL_GRADE_BOULDER, gradeList.get(0)));
                    break;
                default:
                    return false;  // not a relevant preference
            }
        }else {
            switch(key) {
                case Shared.KEY_GOAL_NUMSESSIONS_ROPES:
                    mPrefSessionsPerWeek = sharedPref.getInt(Shared.KEY_GOAL_NUMSESSIONS_ROPES, 0);
                    break;
                case Shared.KEY_GOAL_NUMCLIMBS_ROPES:
                    mPrefNumClimbs = sharedPref.getInt(Shared.KEY_GOAL_NUMCLIMBS_ROPES, 0);
                    break;
                case Shared.KEY_GOAL_VPOINTS_ROPES:
                    mPrefNumpoints = sharedPref.getInt(Shared.KEY_GOAL_VPOINTS_ROPES, 0);
                    break;
                case Shared.KEY_GOAL_GRADE_ROPES:
                    mPrefTargetGrade = gradeList.indexOf(sharedPref.getString(Shared.KEY_GOAL_GRADE_ROPES, gradeList.get(0)));
                    break;
                default:
                    return false; // not a relevant pref
            }
        }
        // if we've gotten here then a preference was changed, so recalculate stats
        calculateStats();
        return true;
    }

    public SpannableString getWearCenterText() {
        SpannableString span =  new SpannableString(TextUtils.concat(
                getSingleStatString(POINTS.abbr, Integer.toString(mTotalPoints), Integer.toString(mPrefNumpoints), POINTS.color), "\n",
                getSingleStatString(CLIMBS.abbr, Integer.toString(mTotalClimbs), Integer.toString(mPrefNumClimbs), CLIMBS.color), "\n",
                getSingleStatString(StatType.GRADE.abbr, getGradeString(mMaxGrade), mClimbType.grades.get(mPrefTargetGrade), StatType.GRADE.color)));
        span.setSpan(new RelativeSizeSpan(0.8f), 0, span.length(), 0); // reduce size for wearable
        return span;
    }


    public enum StatType {
        POINTS ("Points", "Pts", Color.RED),
        CLIMBS("Climbs", "Clmb", Color.GREEN),
        GRADE("Max Grade","Grd", Color.BLUE);

        String title;
        public int color;
        String abbr;

        StatType(String title, String abbr, int color) {
            this.title = title;
            this.color = color;
            this.abbr = abbr;
        }

        public int getSoftColor(){
            float softSat = 0.2f;
            float[] hsvVals = new float[3];

            Color.colorToHSV(color, hsvVals);
            hsvVals[1] = softSat;
            return Color.HSVToColor(hsvVals);
        }
        public int getBoldColor(){
            float boldSat = 0.6f;
            float[] hsvVals = new float[3];

            Color.colorToHSV(color, hsvVals);
            hsvVals[1] = boldSat;
            return Color.HSVToColor(hsvVals);
        }
    }

    public ClimbStats(RealmResults<Climb> results, Shared.ClimbType type, ChronoUnit daterange, SharedPreferences sp) {
        mResult = results;
        mClimbType = type;
        mDateRange = daterange;

        updatePreferences(sp);
    }

    public void calculateStats() {
        if (!mResult.isEmpty()) {
            mTotalPoints = (mResult.sum("grade")).intValue();
            mTotalClimbs = mResult.size();
            mMaxGrade = mResult.max("grade");

            mMaxPoints = 0;
            mMaxClimbs = 0;
            mNumSessionsGradeGoalReached = 0;
            int runningMaxGradeSum = 0;

            // first get all the unique session dates
            mSessionDates = new ArrayList<>();
            // make sure results are sorted
            RealmResults<Climb> sortedResult = mResult.sort("date");
            for(Climb climb:sortedResult) {
                // if day, then don't filter the time out
                Date sessionDate;
                if(mDateRange == ChronoUnit.DAYS) {
                    sessionDate = climb.getDate();
                }else{
                    sessionDate = Shared.getStartofDate(climb.getDate());
                }
                if(!mSessionDates.contains(sessionDate)) {
                    mSessionDates.add(sessionDate);
                }
            }

            // Get scale and offset for x-axis, timestamp numbers are too large
            mTimeStampOffset = mSessionDates.get(0).getTime();
            if(mDateRange == ChronoUnit.DAYS) {
                mTimeStampScale = 1000*60;  // 1 minutes per tic
            }else {
                mTimeStampScale = 1000*60*60*24; // 1 day per tic
            }

            // loop through all sessions to get session stats
            for (Date date:mSessionDates) {
                // Get the climbs in this session
                RealmResults sessionResult = sortedResult.where().between("date", date,
                        Shared.ZDTToDate(Shared.DateToZDT(date).plusDays(1))).findAll();

                // ---------------- Max session grade ------------------------
                int maxSessionGrade = sessionResult.max("grade").intValue();
                if(maxSessionGrade>=mPrefTargetGrade) {
                    mNumSessionsGradeGoalReached++;
                }
                runningMaxGradeSum += maxSessionGrade;

                // -------------------- Session points -------------------
                int sessionPoints = sessionResult.sum("grade").intValue();
                // check maxes
                if (sessionPoints > mMaxPoints) {
                    mMaxPoints = sessionPoints;
                }

                // --------------------- session climbs -----------------
                int sessionClimbs = sessionResult.size();
                if (sessionClimbs > mMaxClimbs) {
                    mMaxClimbs = sessionClimbs;
                }
            }
            mNumSessions = mSessionDates.size();
            mAverageMaxGrade = runningMaxGradeSum/mNumSessions;
            mAveragePoints = mTotalPoints/mNumSessions;
            mAverageClimbs = mTotalClimbs/mNumSessions;
        } else {
            // TODO: fill in this case
        }
    }

    public SpannableString getCenterText(StatType statType) {
        // set the center text
        if(mDateRange == ChronoUnit.DAYS) {
            // show all three stat types at once
            return new SpannableString(TextUtils.concat(
                    getSingleStatString(POINTS.title, Integer.toString(mTotalPoints), Integer.toString(mPrefNumpoints), POINTS.color), "\n",
                    getSingleStatString(CLIMBS.title, Integer.toString(mTotalClimbs), Integer.toString(mPrefNumClimbs), CLIMBS.color), "\n",
                    getSingleStatString(StatType.GRADE.title, getGradeString(mMaxGrade), mClimbType.grades.get(mPrefTargetGrade), StatType.GRADE.color)));
        }else if(mDateRange == ChronoUnit.FOREVER) {
            statType = POINTS;
            SpannableString title = new SpannableString(statType.title);
            title.setSpan(new ForegroundColorSpan(statType.color), 0, title.length(), 0);
            title.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), 0);
            SpannableString result = new SpannableString(TextUtils.concat(
                            title, "\n",
                            getSingleStatString("Average", Integer.toString(Math.round(mAveragePoints)), null, statType.color), "\n",
                            getSingleStatString("Best", Integer.toString(Math.round(mMaxPoints)), null, statType.color), "\n",
                            getSingleStatString("Total", Integer.toString(Math.round(mTotalPoints)), null, statType.color)));
            statType = CLIMBS;
            title = new SpannableString(statType.title);
            title.setSpan(new ForegroundColorSpan(statType.color), 0, title.length(), 0);
            title.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), 0);
            result = new SpannableString(TextUtils.concat(result,"\n\n",
                            title, "\n",
                            getSingleStatString("Average", Integer.toString(Math.round(mAverageClimbs)), null, statType.color), "\n",
                            getSingleStatString("Best", Integer.toString(Math.round(mMaxClimbs)), null, statType.color), "\n",
                            getSingleStatString("Total", Integer.toString(Math.round(mTotalClimbs)), null, statType.color)));
            statType = GRADE;
            title = new SpannableString(statType.title);
            title.setSpan(new ForegroundColorSpan(statType.color), 0, title.length(), 0);
            title.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), 0);
            result = new SpannableString(TextUtils.concat(result,"\n\n",
                            title, "\n",
                            getSingleStatString("Average", getGradeString(mAverageMaxGrade), null, statType.color), "\n",
                            getSingleStatString("Best", getGradeString(mMaxGrade), null, statType.color), "\n",
                            getSingleStatString("# Sessions achieved", Integer.toString(mNumSessionsGradeGoalReached), null, statType.color)));
            return result;
        }else {
            SpannableString title = new SpannableString(statType.title);
            title.setSpan(new ForegroundColorSpan(statType.color), 0, title.length(), 0);
            title.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), 0);
            switch(statType) {
                case POINTS:
                    return new SpannableString(TextUtils.concat(
                            title, "\n",
                            getSingleStatString("Average", Integer.toString(Math.round(mAveragePoints)), null, statType.color), "\n",
                            getSingleStatString("Best", Integer.toString(Math.round(mMaxPoints)), null, statType.color), "\n",
                            getSingleStatString("Total", Integer.toString(Math.round(mTotalPoints)), Integer.toString(mPrefNumpoints * getGoalMultiplier()), statType.color),"\n",
                            getPageIndicatorString(0, 3)));
                case CLIMBS:
                    return new SpannableString(TextUtils.concat(
                            title, "\n",
                            getSingleStatString("Average", Integer.toString(Math.round(mAverageClimbs)), null, statType.color), "\n",
                            getSingleStatString("Best", Integer.toString(Math.round(mMaxClimbs)), null, statType.color), "\n",
                            getSingleStatString("Total", Integer.toString(Math.round(mTotalClimbs)), Integer.toString(mPrefNumClimbs * getGoalMultiplier()), statType.color), "\n",
                            getPageIndicatorString(1, 3)));
                case GRADE:
                    return new SpannableString(TextUtils.concat(
                            title, "\n",
                            getSingleStatString("Average", getGradeString(mAverageMaxGrade), null, statType.color), "\n",
                            getSingleStatString("Best", getGradeString(mMaxGrade), null, statType.color), "\n",
                            getSingleStatString("# Sessions achieved", Integer.toString(mNumSessionsGradeGoalReached), Integer.toString(getGoalMultiplier()), statType.color), "\n",
                            getPageIndicatorString(2, 3)));
                default:
                    return null;
            }
        }
    }

    private SpannableString getPageIndicatorString(int selectedPage, int totalPages) {
        SpannableString normal = new SpannableString("o");
        SpannableString selected = new SpannableString("●");
        //selected.setSpan(new StyleSpan(Typeface.BOLD), 0, selected.length(), 0);
        CharSequence cs = "";

        for(int i = 0; i<totalPages; i ++) {
            if(i == selectedPage) {
                cs = TextUtils.concat(cs, selected);
            }else {
                cs = TextUtils.concat(cs, normal);
            }
        }

        return new SpannableString(cs);

    }

    private String getGradeString(Number grade) {
        if(grade == null) {
            return "--";
        }else {
            return mClimbType.grades.get(grade.intValue());
        }
    }


    private SpannableString getSingleStatString(String title, String current, String goal, int color) {
        SpannableString titleSpan = new SpannableString(title + ": ");

        SpannableString statSpan = new SpannableString(current);
        statSpan.setSpan(new RelativeSizeSpan(2f), 0, statSpan.length(), 0);

        SpannableString goalSpan = new SpannableString("");
        if(goal!=null) {
            goalSpan = new SpannableString("/" + goal);
            goalSpan.setSpan(new SubscriptSpan(), 0, goalSpan.length(), 0);
        }

        // Anything applied to entire string gets added here
        SpannableString s = new SpannableString(TextUtils.concat(titleSpan, statSpan, goalSpan));
        s.setSpan(new ForegroundColorSpan(color), 0, s.length(), 0);
        return s;
    }

    public PieData getPieData(StatType statType) {
        // TODO: this should be brought back into overview fragment, only stats related ops should go in this class
        // set the appropriate bar values
        float current;
        int goal;
        switch(statType) {
            case POINTS:
                current = mTotalPoints ;
                goal = mPrefNumpoints * getGoalMultiplier();
                break;
            case CLIMBS:
                current = mTotalClimbs;
                goal = mPrefNumClimbs * getGoalMultiplier();
                break;
            case GRADE:
                current = mDateRange == ChronoUnit.DAYS?
                        mMaxGrade==null? 0:mMaxGrade.intValue():  // if mdaterange == DAY
                        mNumSessionsGradeGoalReached;           // otherwise
                goal = mDateRange ==    ChronoUnit.DAYS? mPrefTargetGrade:getGoalMultiplier();
                break;
            default:
                // shouldnt get here
                Log.e(TAG, "Got unexpected climbtype");
                goal = 0;
                current = 0;
                break;
        }
        float remainder = goal-current < 0 ? 0 : goal-current; // floor at 0
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(remainder, "Goal"));
        entries.add(new PieEntry(current, "Current"));
        PieDataSet set = new PieDataSet(entries, "Stats");

        ArrayList<Integer> colors = new ArrayList<>();
        if(current == 0) {
            colors.add(statType.getSoftColor());
        } else if(remainder == 0)
        {
            colors.add(statType.getBoldColor());
        }else {
            colors.add(statType.getSoftColor());
            colors.add(statType.getBoldColor());
        }
        set.setColors(colors);
        return new PieData(set);
    }

    public ScatterData getScatterData() {
        // loop through each climb and add as an entry
        ScatterData data = new ScatterData();
        ArrayList<Entry> maxGradeEntries = new ArrayList<>();
        if(mDateRange == ChronoUnit.DAYS) {
            for (Climb climb : mResult) {
                maxGradeEntries.add(new Entry(dateToXValue(climb.getDate()), climb.getGrade()));
            }
        }else {
            for (int i = 0; i < mSessionDates.size(); i++) {
                float xValue = dateToXValue(mSessionDates.get(i));
                if(mDateRange != ChronoUnit.DAYS) {
                     xValue += 0.5f; //put dot in center
                }
                if (i == mSessionDates.size() - 1) {
                    // last date entry
                    maxGradeEntries.add(new Entry(xValue, mResult.where().greaterThan("date", mSessionDates.get(i)).max("grade").intValue()));
                } else {
                    maxGradeEntries.add(new Entry(xValue, mResult.where().between("date", mSessionDates.get(i), mSessionDates.get(i + 1)).max("grade").intValue()));
                }
            }
        }

        ScatterDataSet set = new ScatterDataSet(maxGradeEntries, "grades");
        set.setScatterShapeSize(8f);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        set.setColors(new int[] {StatType.GRADE.color});
        set.setDrawValues(false);
        data.addDataSet(set);
        return data;

    }


    public LineData getLineData() {
        ArrayList<Entry> climbEntries = new ArrayList<>();
        ArrayList<Entry> pointsEntries = new ArrayList<>();
        for(int i = 0; i<mSessionDates.size(); i++) {
            float xValue =  dateToXValue(mSessionDates.get(i));
            //if(mDateRange == ChronoUnit.DAYS) {
            // use cumulative sum
            if (i== mSessionDates.size() -1) {
                pointsEntries.add(new Entry(xValue, mResult.where().sum("grade").intValue()));
                climbEntries.add(new Entry(xValue, mResult.where().count()));
            }else {
                pointsEntries.add(new Entry(xValue, mResult.where().lessThan("date", mSessionDates.get(i + 1)).sum("grade").intValue()));
                climbEntries.add(new Entry(xValue, mResult.where().lessThan("date", mSessionDates.get(i + 1)).count()));
            }

            /*} else {
                if (i == mSessionDates.size() - 1) {
                    // last date entry
                    climbEntries.add(new Entry(xValue, mResult.where().greaterThan("date", mSessionDates.get(i)).sum("grade").intValue()));
                    pointsEntries.add(new Entry(xValue, mResult.where().greaterThan("date", mSessionDates.get(i)).count()));
                } else {
                    climbEntries.add(new Entry(xValue, mResult.where().between("date", mSessionDates.get(i), mSessionDates.get(i + 1)).sum("grade").intValue()));
                    pointsEntries.add(new Entry(xValue, mResult.where().between("date", mSessionDates.get(i), mSessionDates.get(i + 1)).count()));
                }
            }*/
        }
        // ensure that they are sorted
        Collections.sort(climbEntries, new EntryXComparator());
        Collections.sort(pointsEntries, new EntryXComparator());

        LineData data = new LineData();
        LineDataSet dataSet = new LineDataSet(climbEntries, "climbs");
        dataSet.setColors(new int[] {CLIMBS.getBoldColor()});
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(CLIMBS.getSoftColor());
        dataSet.setLineWidth(3f);
        dataSet.setMode(LineDataSet.Mode.LINEAR );
        dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        data.addDataSet(dataSet);

        dataSet = new LineDataSet(pointsEntries, "points");
        dataSet.setColors(new int[] {POINTS.getBoldColor()});
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(false);
        dataSet.setDrawFilled(true); // TODO: there is a bug where this doesnt work with cubic mode: https://github.com/PhilJay/MPAndroidChart/issues/2028
        dataSet.setFillColor(POINTS.getSoftColor());
        dataSet.setLineWidth(3f);
        dataSet.setMode(LineDataSet.Mode.LINEAR);
        dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        data.addDataSet(dataSet);

        return data;
    }

    public Date XValueToDate(float value) {
        Date date =  new Date();
        date.setTime((long) (value * mTimeStampScale + mTimeStampOffset));
        return date;
    }


    public float dateToXValue(Date date) {
        return (date.getTime() - mTimeStampOffset)/mTimeStampScale;  // subtract reference timestamp to deal with smaller numbers
    }


    public BarData getBarData() {
        List<BarEntry> climbEntries = new ArrayList<>();
        List<BarEntry> pointsEntries = new ArrayList<>();

        // -------- USE A DIFFERENT DATELIST WITH EVERY DAY IN THE RANGE ----------------------------
        RealmResults<Climb> subResult =  mResult.sort("date"); // ensure results are sorted with earliest first
        final List<Date> dateList = new ArrayList<>();
        ZonedDateTime startZDT;
        ZonedDateTime endZDT;

        // Get the first day of the subresult and add one to get the limits of the query
        startZDT = Shared.DateToZDT(subResult.first().getDate()).truncatedTo(ChronoUnit.DAYS);
        endZDT = Shared.DateToZDT(subResult.last().getDate()).truncatedTo(ChronoUnit.DAYS).plusDays(1);
        while (startZDT.isBefore(endZDT)) {
            dateList.add(Shared.ZDTToDate(startZDT));
            startZDT = startZDT.plusDays(1);
        }

        // TODO: can this be done in calculateStats()
        for(int i = 0; i<dateList.size(); i++) {
            float xValue =  dateToXValue(dateList.get(i));
            if(i == dateList.size()-1) {
                // last date entry
                pointsEntries.add(new BarEntry(xValue, mResult.where().greaterThan("date", dateList.get(i)).sum("grade").intValue()));
                climbEntries.add(new BarEntry(xValue, mResult.where().greaterThan("date", dateList.get(i)).count()));
            }else {
                pointsEntries.add(new BarEntry(xValue, mResult.where().between("date", dateList.get(i), dateList.get(i + 1)).sum("grade").intValue()));
                climbEntries.add(new BarEntry(xValue, mResult.where().between("date", dateList.get(i), dateList.get(i + 1)).count()));
            }
        }
        // ensure that they are sorted
        Collections.sort(climbEntries, new EntryXComparator());
        Collections.sort(pointsEntries, new EntryXComparator());

        BarData data = new BarData();
        BarDataSet dataSet = new BarDataSet(climbEntries, "climbs");
        dataSet.setColors(new int[] {CLIMBS.getBoldColor()});
        dataSet.setDrawValues(false);
        dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        data.addDataSet(dataSet);

        dataSet = new BarDataSet(pointsEntries, "points");
        dataSet.setColors(new int[] {POINTS.getBoldColor()});
        dataSet.setDrawValues(false);
        dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        data.addDataSet(dataSet);

        float groupSpace = 0.06f;
        float barSpace = 0.02f; // x2 dataset
        float barWidth = 0.45f; // x2 dataset
        // (0.45 + 0.02) * 2 + 0.06 = 1.00 -> interval per "group"

        data.setBarWidth(barWidth);
        data.groupBars(0, groupSpace, barSpace);

        return data;
    }
}
