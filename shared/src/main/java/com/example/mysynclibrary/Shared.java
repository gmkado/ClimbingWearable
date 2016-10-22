package com.example.mysynclibrary;

import android.content.Context;

import com.example.mysynclibrary.model.Climb;
import com.example.mysynclibrary.model.ClimbingModule;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.google.android.gms.wearable.Asset;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * Created by Grant on 9/28/2016.
 */


public class Shared {
    public static final String REALM_SYNC_PATH = "/sync-data"; // changes here need to be changed in mobile manifest
    public static final String DB_DATA_KEY = "com.example.key.data"; // key for realm data json string
    public static final String REALM_ACK_PATH = "/sync-ack";

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

    public static Date getStartOfDay() {
        // null if using today
        Calendar cal = Calendar.getInstance();

        /*if(date != null) {
            cal.setTime(date);
        }*/

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
    }


    public enum ClimbType {
        bouldering("Bouldering", R.drawable.icon_boulder, createGradeList(0, 15, "V", 16, null)),
        ropes("Ropes", R.drawable.icon_ropes, createGradeList(6, 13, "5.",10, Arrays.asList("a","b","c","d")));

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

        ClimbType(String title, int icon, List<String> grades){
            this.title = title;
            this.icon = icon;
            this.grades = grades;
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
