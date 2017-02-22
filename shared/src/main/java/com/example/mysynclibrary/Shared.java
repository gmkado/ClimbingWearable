package com.example.mysynclibrary;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import com.example.mysynclibrary.realm.ClimbingModule;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Utils;
import com.google.android.gms.wearable.Asset;
import com.google.gson.Gson;

import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.temporal.ChronoUnit;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by Grant on 9/28/2016.
 */


public class Shared {
    public static final String REALM_SYNC_PATH = "/sync-data"; // changes here need to be changed in mobile manifest
    public static final String DB_DATA_KEY = "com.example.key.data"; // key for realm data json string
    public static final String REALM_ACK_PATH = "/sync-ack";
    private static final String TAG = "Shared";

    public static Gson getGson() {
        return new Gson();
    }

    public static void initRealm(Context context) {
        // Create the Realm (or database).  The Realm file will be located in Context.getFilesDir() with name "default.realm"
        RealmConfiguration config = new RealmConfiguration.Builder(context)
                .deleteRealmIfMigrationNeeded()
                .modules(new ClimbingModule())
                .build();
        Realm.setDefaultConfiguration(config);
    }

    public static Date getStartofDate(Date date) {
        // null if using today
        Calendar cal = Calendar.getInstance();

        if(date != null) {
            cal.setTime(date);
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);


        return cal.getTime();
    }

    public static Date getStartOfDateRange(DateRange dr) {
        ZonedDateTime zdt = ZonedDateTime.now();
        zdt = zdt.truncatedTo(ChronoUnit.DAYS);
        switch(dr) {
            case DAY:
                return DateTimeUtils.toDate(zdt.toInstant());
            case WEEK:
                return DateTimeUtils.toDate(zdt.minusWeeks(1).toInstant());
            case MONTH:
                return DateTimeUtils.toDate(zdt.minusMonths(1).toInstant());
            case YEAR:
                return DateTimeUtils.toDate(zdt.minusYears(1).toInstant());
            case ALL:
                // return null which signifies use ALL dates
                return null;
            default:
                Log.d(TAG, "getStartofDateRange: case not recognized");
                return null;
        }
    }


    public enum DateRange{
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
    }

    public enum ClimbLevel{
        beginner,
        intermediate,
        advanced,
        expert
    }

    public enum ClimbType {
        bouldering("Bouldering", R.drawable.icon_boulder,
                createGradeList(0, 15, "V", 16, null),
                Arrays.asList("V3", "V6", "V9")),
        ropes("Ropes", R.drawable.icon_ropes,
                createGradeList(6, 13, "5.",10, Arrays.asList("a","b","c","d")),
                Arrays.asList("5.8", "5.10d", "5.12d"));

        private static List<String> createGradeList(int minGrade, int maxGrade, String prefix, int minSuffixGrade, List<String> suffixList) {
            ArrayList<String> gradeList = new ArrayList();

            for(int grade = minGrade; grade < maxGrade+1; grade++) {
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

        public int getMaxGradeInd(ClimbLevel level) {
            return indMaxGradeForLevel.get(level.ordinal());
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
