package com.example.mysynclibrary;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
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

import org.threeten.bp.temporal.ChronoUnit;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import io.realm.RealmResults;

import static com.example.mysynclibrary.ClimbStats.StatType.CLIMBS;
import static com.example.mysynclibrary.ClimbStats.StatType.GRADE;
import static com.example.mysynclibrary.ClimbStats.StatType.POINTS;

/**
 * Created by Grant on 4/21/2017.
 */

public class ClimbStats {
    private static final String TAG = "ClimbStats";
    private static final String NULL_GRADE_STR = "--";
    private final Shared.ClimbType mClimbType;
    private final ChronoUnit mDateRange;

    private List<String> mDateRangeLabels = Arrays.asList("DAY", "WEEK", "MONTH", "YEAR", "ALL");
    private List<ChronoUnit> mDateRanges = Arrays.asList(ChronoUnit.DAYS, ChronoUnit.WEEKS, ChronoUnit.MONTHS, ChronoUnit.YEARS, ChronoUnit.FOREVER);


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
    private ArrayList<Entry> gradeScatterEntries;
    private ArrayList<Entry> climbLineEntries;
    private ArrayList<Entry> pointLineEntries;
    private ArrayList<BarEntry> pointBarEntries;
    private ArrayList<BarEntry> climbBarEntries;

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

    public SpannableString getWearCenterText(boolean isAmbient) {
        SpannableString span;
        if(!isAmbient) {
            span = new SpannableString(TextUtils.concat(
                    getSingleStatString(POINTS.title, Integer.toString(mTotalPoints), Integer.toString(mPrefNumpoints), POINTS.basecolor.App), "\n",
                    getSingleStatString(CLIMBS.title, Integer.toString(mTotalClimbs), Integer.toString(mPrefNumClimbs), CLIMBS.basecolor.App), "\n",
                    getSingleStatString(GRADE.title, getGradeString(mMaxGrade), mClimbType.grades.get(mPrefTargetGrade), GRADE.basecolor.App)));
        }else {
            span = new SpannableString(TextUtils.concat(
                    getSingleStatString(POINTS.title, Integer.toString(mTotalPoints), Integer.toString(mPrefNumpoints), Color.WHITE), "\n",
                    getSingleStatString(CLIMBS.title, Integer.toString(mTotalClimbs), Integer.toString(mPrefNumClimbs), Color.WHITE), "\n",
                    getSingleStatString(GRADE.title, getGradeString(mMaxGrade), mClimbType.grades.get(mPrefTargetGrade), Color.WHITE)));

        }
        span.setSpan(new RelativeSizeSpan(0.8f), 0, span.length(), 0); // reduce size for wearable
        return span;
    }


    public enum StatType {
        POINTS ("POINTS", "Pts", ColorHelper.BaseColor.RED),
        CLIMBS("CLIMBS", "Clmb", ColorHelper.BaseColor.BLUE),
        GRADE("GRADE","Grd", ColorHelper.BaseColor.GREEN);

        String title;
        public ColorHelper.BaseColor basecolor;
        String abbr;

        StatType(String title, String abbr, ColorHelper.BaseColor basecolor) {
            this.title = title;
            this.basecolor = basecolor;
            this.abbr = abbr;
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

            // -------------- GET ALL UNIQUE SESSIONS IN RESULTS -----------------
            mSessionDates = new ArrayList<>();
            for(Climb climb:mResult) {
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

            // ------------------loop through all sessions to get session stats -------------------
            mMaxPoints = 0;
            mMaxClimbs = 0;
            mNumSessionsGradeGoalReached = 0;
            int runningMaxGradeSum = 0;
            gradeScatterEntries = new ArrayList<>();
            climbLineEntries = new ArrayList<>();
            pointLineEntries = new ArrayList<>();
            climbBarEntries = new ArrayList<>();
            pointBarEntries = new ArrayList<>();

            if(mDateRange == ChronoUnit.DAYS) {
                // ---------------- Overview stats ------------------------
                runningMaxGradeSum = mResult.max("grade").intValue();
                mTotalPoints = mResult.sum("grade").intValue();
                mTotalClimbs = mResult.size();

                // ------------------- Chart stats -----------------------
                for (Climb climb : mResult) {
                    float xValue = dateToXValue(climb.getDate());
                    gradeScatterEntries.add(new Entry(xValue, climb.getGrade()));
                    pointLineEntries.add(new Entry(xValue, mResult.where().lessThanOrEqualTo("date", climb.getDate()).sum("grade").intValue()));
                    climbLineEntries.add(new Entry(xValue, mResult.where().lessThanOrEqualTo("date", climb.getDate()).count()));
                }
            }else {
                // -------- USE A DIFFERENT DATELIST WITH EVERY DAY IN THE RANGE ----------------------------
                final List<Date> dateList = new ArrayList<>();
                Date startDate = mSessionDates.get(0);
                Date endDate = mSessionDates.get(mSessionDates.size()-1);

                while (startDate.before(endDate)) {
                    dateList.add(startDate);
                    startDate = Shared.ZDTToDate(Shared.DateToZDT(startDate).plusDays(1));
                }
                dateList.add(endDate);

                for (Date date : dateList) {
                    float xValue = dateToXValue(date);
                    if(mSessionDates.contains(date)) {
                        // Get the climbs in this session
                        RealmResults sessionResult = mResult.where().between("date", date,
                                Shared.ZDTToDate(Shared.DateToZDT(date).plusDays(1))).findAll();

                        // ---------------- Max session grade ------------------------
                        int maxSessionGrade = sessionResult.max("grade").intValue();
                        gradeScatterEntries.add(new Entry(xValue + 0.5f, maxSessionGrade)); //put dot in center
                        if (maxSessionGrade >= mPrefTargetGrade) {
                            mNumSessionsGradeGoalReached++;
                        }
                        runningMaxGradeSum += maxSessionGrade;

                        // -------------------- Session points -------------------
                        int sessionPoints = sessionResult.sum("grade").intValue();
                        pointBarEntries.add(new BarEntry(xValue, sessionPoints));
                        // check maxes
                        if (sessionPoints > mMaxPoints) {
                            mMaxPoints = sessionPoints;
                        }

                        // --------------------- session climbs -----------------
                        int sessionClimbs = sessionResult.size();
                        if (sessionClimbs > mMaxClimbs) {
                            mMaxClimbs = sessionClimbs;
                        }
                        climbBarEntries.add(new BarEntry(xValue, sessionClimbs));
                    }else {
                        pointBarEntries.add(new BarEntry(xValue, 0));
                        climbBarEntries.add(new BarEntry(xValue, 0));

                    }
                }
            }
            mNumSessions = mSessionDates.size();
            mAverageMaxGrade = runningMaxGradeSum / mNumSessions;
            mAveragePoints = mTotalPoints / mNumSessions;
            mAverageClimbs = mTotalClimbs / mNumSessions;
        } else {
            // TODO: fill in this case
        }
    }

    public SpannableString getCenterText(StatType statType) {
        // set the center text
        if(mDateRange == ChronoUnit.FOREVER) {
            statType = POINTS;
            SpannableString title = new SpannableString(statType.title);
            title.setSpan(new ForegroundColorSpan(statType.basecolor.App), 0, title.length(), 0);
            title.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), 0);
            SpannableString result = new SpannableString(TextUtils.concat(
                            title, "\n",
                            getSingleStatString("Average", Integer.toString(Math.round(mAveragePoints)), null, statType.basecolor.App), "\n",
                            getSingleStatString("Best", Integer.toString(Math.round(mMaxPoints)), null, statType.basecolor.App), "\n",
                            getSingleStatString("Total", Integer.toString(Math.round(mTotalPoints)), null, statType.basecolor.App)));
            statType = CLIMBS;
            title = new SpannableString(statType.title);
            title.setSpan(new ForegroundColorSpan(statType.basecolor.App), 0, title.length(), 0);
            title.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), 0);
            result = new SpannableString(TextUtils.concat(result,"\n\n",
                            title, "\n",
                            getSingleStatString("Average", Integer.toString(Math.round(mAverageClimbs)), null, statType.basecolor.App), "\n",
                            getSingleStatString("Best", Integer.toString(Math.round(mMaxClimbs)), null, statType.basecolor.App), "\n",
                            getSingleStatString("Total", Integer.toString(Math.round(mTotalClimbs)), null, statType.basecolor.App)));
            statType = GRADE;
            title = new SpannableString(statType.title);
            title.setSpan(new ForegroundColorSpan(statType.basecolor.App), 0, title.length(), 0);
            title.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), 0);
            result = new SpannableString(TextUtils.concat(result,"\n\n",
                            title, "\n",
                            getSingleStatString("Average", getGradeString(mAverageMaxGrade), null, statType.basecolor.App), "\n",
                            getSingleStatString("Best", getGradeString(mMaxGrade), null, statType.basecolor.App), "\n",
                            getSingleStatString("# Sessions achieved", Integer.toString(mNumSessionsGradeGoalReached), null, statType.basecolor.App)));
            return result;
        }else {
            SpannableString goalString;
            SpannableString percentString;
            SpannableString remainderString;

            int current = getCurrentForStatType(statType);
            int goal = getGoalForStatType(statType);
            int remainder = goal-current < 0 ? 0 : goal-current; // floor at 0
            int percent = current*100/goal;
            percentString = new SpannableString(Integer.toString(percent) + "%");

            if(statType != GRADE) {
                goalString = new SpannableString("SEND " + Integer.toString(goal) + " " + statType.title +
                        " PER " + mDateRangeLabels.get(mDateRanges.indexOf(mDateRange)));
                remainderString = new SpannableString(Integer.toString(remainder) + " " + statType.title + " TO GO");
            }else {
                if(mDateRange == ChronoUnit.DAYS) {
                    goalString = new SpannableString("SEND " + getGradeString(mPrefTargetGrade) +
                            " TODAY");
                    String gradeStr = getGradeString(mMaxGrade);
                    percentString = new SpannableString(gradeStr);
                    if(!gradeStr.equals(NULL_GRADE_STR)) {
                        remainderString = new SpannableString(Integer.toString(remainder) + " GRADES AWAY");
                    }else {
                        remainderString = new SpannableString("YOU CAN DO IT!");
                    }
                }else {
                    goalString = new SpannableString("SEND " + getGradeString(mPrefTargetGrade) +
                            " IN " + Integer.toString(goal) + " SESSIONS");
                    remainderString = new SpannableString(Integer.toString(remainder) + " SESSIONS TO GO");
                }
            }


            percentString.setSpan(new RelativeSizeSpan(4), 0, percentString.length(), 0);

            goalString.setSpan(new ForegroundColorSpan(statType.basecolor.App), 0, goalString.length(), 0);
            remainderString.setSpan(new ForegroundColorSpan(statType.basecolor.App), 0, remainderString.length(), 0);
            percentString.setSpan(new ForegroundColorSpan(statType.basecolor.App), 0, percentString.length(), 0);

            return new SpannableString(TextUtils.concat(
                    "\n\n", goalString, "\n",
                    percentString, "\n",
                    remainderString, "\n\n",
                    getPageIndicatorString(statType.ordinal(), StatType.values().length)
            ));
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
            return NULL_GRADE_STR;
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

    public PieData getPieData(StatType statType, boolean isWear) {
        // TODO: this should be brought back into overview fragment, only stats related ops should go in this class
        // set the appropriate bar values
        int current = getCurrentForStatType(statType);
        int goal = getGoalForStatType(statType);

        int remainder = goal-current < 0 ? 0 : goal-current; // floor at 0
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(remainder, "Goal"));
        entries.add(new PieEntry(current, "Current"));
        PieDataSet set = new PieDataSet(entries, "Stats");

        ArrayList<Integer> colors = new ArrayList<>();
        int background;
        int foreground;
        if(isWear) {
            background = statType.basecolor.Dark;
            foreground = statType.basecolor.Accent;
        }else {
            background = statType.basecolor.Soft;
            foreground = statType.basecolor.Accent;
        }
        if(current == 0) {
            colors.add(background);
        } else if(remainder == 0)
        {
            colors.add(foreground);
        }else {
            colors.add(background);
            colors.add(foreground);
        }
        set.setColors(colors);
        set.setSelectionShift(0f);
        return new PieData(set);
    }

    private int getGoalForStatType(StatType statType) {
        switch(statType) {
            case POINTS:
                return mPrefNumpoints * getGoalMultiplier();
            case CLIMBS:
                return mPrefNumClimbs * getGoalMultiplier();
            case GRADE:
                return mDateRange == ChronoUnit.DAYS? mPrefTargetGrade:getGoalMultiplier();
            default:
                // shouldnt get here
                Log.e(TAG, "Got unexpected climbtype");
                return -1;
        }
    }

    private int getCurrentForStatType(StatType statType) {
        switch(statType) {
            case POINTS:
                return mTotalPoints ;
            case CLIMBS:
                return mTotalClimbs;
            case GRADE:
                return mDateRange == ChronoUnit.DAYS?
                        mMaxGrade==null? 0:mMaxGrade.intValue():  // if mdaterange == DAY
                        mNumSessionsGradeGoalReached;           // otherwise
            default:
                // shouldnt get here
                Log.e(TAG, "Got unexpected climbtype");
                return -1;
        }
    }

    public ScatterData getScatterData() {
        // loop through each climb and add as an entry
        ScatterData data = new ScatterData();
        ScatterDataSet set = new ScatterDataSet(gradeScatterEntries, "grades");
        set.setScatterShapeSize(8f);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        set.setColors(new int[] {GRADE.basecolor.Accent});
        set.setDrawValues(false);
        data.addDataSet(set);
        return data;

    }


    public LineData getLineData() {
        // ensure that they are sorted
        Collections.sort(climbLineEntries, new EntryXComparator());
        Collections.sort(pointLineEntries, new EntryXComparator());

        int lineColor = CLIMBS.basecolor.Accent;
        int fillColor = CLIMBS.basecolor.Soft;
        LineData data = new LineData();
        LineDataSet dataSet = new LineDataSet(climbLineEntries, "climbs");
        dataSet.setColors(new int[] {lineColor});
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(fillColor);
        dataSet.setLineWidth(3f);
        dataSet.setMode(LineDataSet.Mode.LINEAR );
        dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        data.addDataSet(dataSet);

        lineColor = POINTS.basecolor.Accent;
        fillColor = POINTS.basecolor.Soft;
        dataSet = new LineDataSet(pointLineEntries, "points");
        dataSet.setColors(new int[] {lineColor});
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(false);
        dataSet.setDrawFilled(true); // TODO: there is a bug where this doesnt work with cubic mode: https://github.com/PhilJay/MPAndroidChart/issues/2028
        dataSet.setFillColor(fillColor);
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
        // ensure that they are sorted
        Collections.sort(climbBarEntries, new EntryXComparator());
        Collections.sort(pointBarEntries, new EntryXComparator());

        BarData data = new BarData();
        BarDataSet dataSet = new BarDataSet(climbBarEntries, "climbs");
        dataSet.setColors(new int[] {CLIMBS.basecolor.Accent});
        dataSet.setDrawValues(false);
        dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        data.addDataSet(dataSet);

        dataSet = new BarDataSet(pointBarEntries, "points");
        dataSet.setColors(new int[] {POINTS.basecolor.Accent});
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
