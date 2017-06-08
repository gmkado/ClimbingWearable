package com.example.mysynclibrary;

import android.content.Context;
import android.support.v4.util.Pair;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.example.mysynclibrary.realm.ClimbingModule;
import com.example.mysynclibrary.realm.MyMigration;
import com.google.android.gms.wearable.Asset;
import com.google.gson.Gson;

import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.temporal.ChronoUnit;
import org.threeten.bp.temporal.TemporalField;
import org.threeten.bp.temporal.WeekFields;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by Grant on 9/28/2016.
 */


public class Shared {
    private static final String TAG = "Shared";

    //message api paths
    public static final String REALM_SYNC_PATH = "/sync-data"; // changes here need to be changed in mobile manifest
    public static final String REALM_ACK_PATH = "/sync-ack";

    // ********** shared preference keys ***********//
    public static final String KEY_WEAR_ENABLED = "wear_enabled_switch";
    // warmup preferences
    public static final String KEY_WARMUP_ENABLED = "warmup_enabled_switch";
    public static final String KEY_WARMUP_MAXGRADE_BOULDER = "warmup_maxgrade_boulder";
    public static final String KEY_WARMUP_NUMCLIMBS_BOULDER = "warmup_numclimbs_boulder";
    public static final String KEY_WARMUP_MAXGRADE_ROPES = "warmup_maxgrade_ropes";
    public static final String KEY_WARMUP_NUMCLIMBS_ROPES = "warmup_numclimbs_ropes";
    // bouldering goal preference
    public static final String KEY_GOAL_GRADE_BOULDER = "goal_grade_boulder";
    public static final String KEY_GOAL_NUMCLIMBS_BOULDER = "goal_numclimbs_boulder";
    public static final String KEY_GOAL_VPOINTS_BOULDER = "goal_vpoints_boulder";
    public static final String KEY_GOAL_NUMSESSIONS_BOULDER = "goal_numsessions_boulder";
    // rope goal preferences
    public static final String KEY_GOAL_GRADE_ROPES = "goal_grade_ropes";
    public static final String KEY_GOAL_NUMCLIMBS_ROPES = "goal_numclimbs_ropes";
    public static final String KEY_GOAL_VPOINTS_ROPES = "goal_vpoints_ropes";
    public static final String KEY_GOAL_NUMSESSIONS_ROPES = "goal_numsessions_ropes";




    public static Gson getGson() {
        return new Gson();
        /*
        // see https://gist.github.com/cmelchior/ddac8efd018123a1e53a


            Gson gson = new GsonBuilder()
                    .setExclusionStrategies(new ExclusionStrategy() {
                        @Override
                        public boolean shouldSkipField(FieldAttributes f) {
                            return f.getDeclaringClass().equals(RealmObject.class);
                        }

                        @Override
                        public boolean shouldSkipClass(Class<?> clazz) {
                            return false;
                        }
                    })
                    .registerTypeAdapter(Climb.class, new ClimbSerializer())
                    .create();


        return gson; */
    }

    public static void initRealm(Context context) {
        Realm.init(context);

        // Create the Realm (or database).  The Realm file will be located in Context.getFilesDir() with name "default.realm"
        RealmConfiguration config = new RealmConfiguration.Builder()
                //.schemaVersion(0)
                //.migration(new MyMigration())
                .deleteRealmIfMigrationNeeded()
                .modules(new ClimbingModule()) // this is necessary for library modules
                .build();

        Realm.setDefaultConfiguration(config);
    }

    /* Datetime utils*/
    public static Date ZDTToDate(ZonedDateTime zdt) {
        return DateTimeUtils.toDate(zdt.toInstant());
    }

    public static ZonedDateTime DateToZDT(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return DateTimeUtils.toZonedDateTime(cal);

    }

    public static Date getStartofDate(Date date) {
        ZonedDateTime startZDT;
        if(date != null) {
            startZDT = DateToZDT(date);

        }else {
            startZDT = ZonedDateTime.now();
        }
        startZDT = startZDT.truncatedTo(ChronoUnit.DAYS);
        return DateTimeUtils.toDate(startZDT.toInstant());

/*        // null if using today
        Calendar cal = Calendar.getInstance();

        if(date != null) {
            cal.setTime(date);
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);


        return cal.getTime();*/
    }

    // return a start and end date based on the date range (e.g. dateRange = MONTHS, start = first day of month, end = last day of month
    // offset = number of "dateRanges" to subtract (e.g. offset = -1 then use previous month)
    public static Pair<Date, Date> getDatesFromRange(ChronoUnit dateRange, int dateOffset) {
        ZonedDateTime startZDT = ZonedDateTime.now();
        startZDT = startZDT.truncatedTo(ChronoUnit.DAYS);

        switch(dateRange) {
            case DAYS:
                break;
            case WEEKS:
                startZDT = startZDT.with(WeekFields.of(Locale.getDefault()).dayOfWeek(),1);
                break;
            //return DateTimeUtils.toDate(zdt.minusWeeks(1).toInstant());
            case MONTHS:
                startZDT = startZDT.withDayOfMonth(1);
                break;
            //return DateTimeUtils.toDate(zdt.minusMonths(1).toInstant());
            case YEARS:
                startZDT = startZDT.withDayOfYear(1);
                break;
            //return DateTimeUtils.toDate(zdt.minusYears(1).toInstant());
            case FOREVER:
                // return null which signifies use ALL dates
                return null;
            default:
                Log.d(TAG, "getStartofDateRange: case not recognized");
                return null;
        }
        startZDT = startZDT.plus(dateOffset, dateRange);
        ZonedDateTime endZDT = startZDT.plus(1, dateRange);

        return new Pair<> (ZDTToDate(startZDT),ZDTToDate(endZDT));
    }

    public static void matchDeviceSizeProgrammatically(Context context, View rootView) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        rootView.setLayoutParams(new ViewGroup.LayoutParams(width, height));
    }

    // return the offset that encompasses date based on the date range (e.g. today = 5/9/17, date = 5/8/17, dateRange = DAYS, return -1
    public static int getOffsetFromDate(Date date, ChronoUnit mDateRange) {
        ZonedDateTime startZDT = ZonedDateTime.now();
        startZDT = startZDT.truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime endZDT = DateToZDT(date);
        endZDT = endZDT.truncatedTo(ChronoUnit.DAYS);

        return (int) mDateRange.between(startZDT, endZDT);
    }


    /*public enum DateRange{
        DAY,
        WEEK,
        MONTH,
        YEAR,
        ALL;

        DateRange() {}

        public static ArrayList<String> getLabels() {
            ArrayList<String> labels = new ArrayList<>();
            for (DateRange dr: DateRange.values()) {
                labels.add(dr.name());
            }
            return labels;
        }
    }*/

    public enum ClimbLevel{
        beginner("Beginner"),
        intermediate("Intermediate"),
        advanced("Advanced"),
        expert("Expert");

        public String title;
        ClimbLevel(String title) {
            this.title = title;
        }
    }

    public enum ClimbType {
        bouldering("Bouldering", R.drawable.icon_boulder,
                createGradeList(0, 17, "V", 18, null),
                Arrays.asList("V3", "V6", "V9")),
        ropes("Ropes", R.drawable.icon_ropes,
                createGradeList(6, 15, "5.",10, Arrays.asList("a","b","c","d")),
                Arrays.asList("5.8", "5.10d", "5.12d"));

        private static List<String> createGradeList(int minGrade, int maxGrade, String prefix, int minSuffixGrade, List<String> suffixList) {
            ArrayList<String> gradeList = new ArrayList();

            for(int grade = minGrade; grade <= maxGrade; grade++) {
                // this is janky but oh well
                if(grade >= minSuffixGrade) {
                    for(String suffix: suffixList) {
                        gradeList.add(prefix + grade + suffix);
                    }
                }else {
                    gradeList.add(prefix + grade);
                }
            }
            return gradeList;
        }

        public String title;
        public int icon;
        public List<String> grades;
        public List<Integer> indMaxGradeForLevel; // index of the max grade for a particular level

        ClimbType(String title, int icon, List<String> grades, List<String> levelDef){
            this.title = title;
            this.icon = icon;
            this.grades = grades;

            // levelDef = hardest grades for easy, med, hard. Expert is assumed as anything larger than hard
            assert levelDef.size() == ClimbLevel.values().length - 1;
            indMaxGradeForLevel = Arrays.asList(
                    grades.indexOf(levelDef.get(ClimbLevel.beginner.ordinal())),
                    grades.indexOf(levelDef.get(ClimbLevel.intermediate.ordinal())),
                    grades.indexOf(levelDef.get(ClimbLevel.advanced.ordinal())),
                    grades.size() - 1);

            if(indMaxGradeForLevel.contains(-1)) {
                // this means one of the grades was not found so throw an error
                throw new IndexOutOfBoundsException();
            }
        }

        public int getIndexOfMaxGradeForLevel(ClimbLevel level) {
            return indMaxGradeForLevel.get(level.ordinal());
        }

        public String getLabelForLevel(ClimbLevel level) {
            int startInd;
            if(level.ordinal() == 0) {
                startInd = 0;
            }else {
                startInd = getIndexOfMaxGradeForLevel(level.values()[level.ordinal()-1])+1;
            }
            int endInd = getIndexOfMaxGradeForLevel(level);
            return level.title + " (" + grades.get(startInd) + " to " +grades.get(endInd) + ")";
        }
    }

    public static Asset assetFromFile(String path) {
        try {
            return Asset.createFromBytes(readFile(path));
        } catch (IOException e) {
            return null;
        }
    }

    public static Asset assetFromFile(File file) {
        try {
            return Asset.createFromBytes(readFile(file));
        } catch (IOException e) {
            return null;
        }
    }

    public static byte[] readFile(String file) throws IOException {
        return readFile(new File(file));
    }

    public static byte[] readFile(File file) throws IOException {
        // Open file
        RandomAccessFile f = new RandomAccessFile(file, "r");
        try {
            // Get and check length
            long longlength = f.length();
            int length = (int) longlength;
            if (length != longlength)
                throw new IOException("File size >= 2 GB");
            // Read file and return data
            byte[] data = new byte[length];
            f.readFully(data);
            return data;
        } finally {
            f.close();
        }
    }


}
