package com.example.grant.wearableclimbtracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Created by Grant on 7/13/2016.
 */
public class DbHelper extends SQLiteOpenHelper{
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "ClimbEntries.db";

    // Helper strings
    private static final String TYPE_TEXT = " TEXT";
    private static final String TYPE_INTEGER = " INTEGER";
    private static final String TYPE_TIMESTAMP = " TIMESTAMP";


    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRY_TABLE =
            "CREATE TABLE " + ClimbEntry.TABLE_NAME + " (" +
                    ClimbEntry._ID + TYPE_INTEGER + " PRIMARY KEY" + COMMA_SEP +
                    ClimbEntry.COLUMN_DATETIME + TYPE_TIMESTAMP + COMMA_SEP +
                    ClimbEntry.COLUMN_CLIMB_TYPE + TYPE_INTEGER + COMMA_SEP +
                    ClimbEntry.COLUMN_GRADE_INDEX + TYPE_INTEGER +
            " )";

    private static final String SQL_DELETE_ENTRY_TABLE =
            "DROP TABLE IF EXISTS " + ClimbEntry.TABLE_NAME;
    private static final String TAG = "DbHelper";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d( TAG, "Created sql table with command " + SQL_CREATE_ENTRY_TABLE);
        db.execSQL(SQL_CREATE_ENTRY_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This DB is a cache for phone app, discard the data and start over
        db.execSQL(SQL_DELETE_ENTRY_TABLE);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }


    public abstract class ClimbEntry implements BaseColumns {
        public static final String TABLE_NAME = "climbs";
        public static final String COLUMN_CLIMB_TYPE = "type";
        public static final String COLUMN_GRADE_INDEX = "grade";
        public static final String COLUMN_DATETIME = "send_dt";
    }

}
