package com.example.mysynclibrary;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.util.Pair;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.mysynclibrary.realm.Area;
import com.example.mysynclibrary.realm.Attempt;
import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.realm.ClimbFields;
import com.example.mysynclibrary.realm.ClimbingModule;
import com.example.mysynclibrary.realm.Goal;
import com.example.mysynclibrary.realm.Gym;
import com.example.mysynclibrary.realm.ISyncableRealmObject;
import com.google.android.gms.wearable.Asset;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.temporal.ChronoUnit;
import org.threeten.bp.temporal.WeekFields;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmModel;
import io.realm.RealmObject;
import io.realm.RealmResults;

/**
 * Created by Grant on 9/28/2016.
 */


public class Shared {
    private static final String TAG = "Shared";


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
    private static final String SYNC_KEY_CLIMBS = "key_climbs";
    private static final String SYNC_KEY_ATTEMPTS = "key_attempts";
    private static final String SYNC_KEY_GYMS = "key_gyms";
    private static final String SYNC_KEY_AREAS = "key_areas";
    private static final String SYNC_KEY_GOALS = "key_goals";
    private static final Type SYNC_MAP_TYPE = new TypeToken<HashMap<String, String>>() {}.getType();
    private static Gson mGson;


    public static Gson getGson() {
        if(mGson == null) {
            mGson = new Gson();
        }
        return mGson;
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
        Realm.setDefaultConfiguration(getRealmConfig(null));
    }

    public static RealmConfiguration getRealmConfig(String name) {
        RealmConfiguration.Builder builder = new RealmConfiguration.Builder()
                //.schemaVersion(0)
                //.migration(new MyMigration())
                .deleteRealmIfMigrationNeeded()
                .modules(new ClimbingModule()); // this is necessary for library module
        if(name !=null) {
            builder.name(name);
        }
        return builder.build();
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

    // input w and h are in dp
    public static Drawable getScaledDrawable(Context context, int res, int w, int h) {
        Bitmap bmap = BitmapFactory.decodeResource(context.getResources(), res);
        Bitmap bmapScaled = Bitmap.createScaledBitmap(bmap, dpToPx(context, w), dpToPx(context, h), false);
        return new BitmapDrawable(context.getResources(), bmapScaled);

    }

    public static int dpToPx(Context context, int dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) (dp * ((float)metrics.densityDpi/DisplayMetrics.DENSITY_DEFAULT));
    }

    public static int pxToDp(Context context, int px) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) (px / ((float)metrics.densityDpi/DisplayMetrics.DENSITY_DEFAULT));
    }


}
