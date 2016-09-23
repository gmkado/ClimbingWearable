package com.example.grant.wearableclimbtracker;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.WatchViewStub;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class AddClimbActivity extends Activity implements AdapterView.OnItemClickListener, DelayedConfirmationView.DelayedConfirmationListener {
    private static final String TAG = "AddClimbActivity";
    private MainActivity.ClimbType mClimbType;
    private List<String> mGradeList;
    private DbHelper mDbHelper;
    private DelayedConfirmationView mDelayedView;
    private int mSelectedPosition; // the current selected grade
    private ListView mListView;
    private LinearLayout mDelayedViewLayout;
    private FrameLayout mListViewLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_climb);

        Bundle extras = getIntent().getExtras();
        mClimbType = (MainActivity.ClimbType)extras.get(MainActivity.EXTRA_CLIMBTYPE);
        mGradeList = mClimbType == MainActivity.ClimbType.bouldering ?
                Arrays.asList(getResources().getStringArray(R.array.bouldering_grades)) :
                Arrays.asList(getResources().getStringArray(R.array.rope_grades));

        mDbHelper = new DbHelper(this);
        //TODO: is oncreate for dbhelper called?
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mListViewLayout = (FrameLayout) findViewById(R.id.listViewLayout);
                mDelayedViewLayout = (LinearLayout) findViewById(R.id.delayedConfirmationLayout);

                mListView = (ListView)findViewById(R.id.listView);


                ArrayAdapter adapter = new ArrayAdapter(AddClimbActivity.this, android.R.layout.simple_list_item_1, mGradeList);
                mListView.setAdapter(adapter);
                mListView.setOnItemClickListener(AddClimbActivity.this);
                mDelayedView = (DelayedConfirmationView) findViewById(R.id.delayed_confirm);


                setListViewVisible(true);
            }
        });




    }

    private void setListViewVisible(boolean listViewVisible) {
        Log.d(TAG, String.format("setListViewVisible(%b)",listViewVisible));
        if(listViewVisible){
            mListViewLayout.setVisibility(View.VISIBLE);
            mDelayedViewLayout.setVisibility(View.GONE);
        }else{

            mListViewLayout.setVisibility(View.GONE);
            mDelayedViewLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "onItemClick");
        mSelectedPosition = position;

        // set list view
        TextView tv = (TextView) findViewById(R.id.saveConfirmationTextView);
        tv.setText(String.format("Saving %s send", mGradeList.get(position)));
        setListViewVisible(false);


        mDelayedView.setListener(AddClimbActivity.this);
        // Two seconds to cancel the action
        mDelayedView.setTotalTimeMs(2000);
        // Start the timer
        mDelayedView.start();
    }

    @Override
    public void onTimerFinished(View view) {
        // not cancelled so save the climb
        Log.d(TAG, "onTimerFinished");

        // get database
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DbHelper.ClimbEntry.COLUMN_CLIMB_TYPE, mClimbType.ordinal());

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        values.put(DbHelper.ClimbEntry.COLUMN_DATETIME, df.format(date));
        values.put(DbHelper.ClimbEntry.COLUMN_GRADE_INDEX, mSelectedPosition);

        long rowId = db.insert(DbHelper.ClimbEntry.TABLE_NAME, null, values);

        // show confirmation
        Intent intent = new Intent(this, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                ConfirmationActivity.SUCCESS_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                "Saved");
        startActivity(intent);
    }

    @Override
    public void onTimerSelected(View view) {
        Log.d(TAG, "onTimerSelected");

        mDelayedView.setPressed(true);

        // Prevent onTimerFinished from being heard.
        mDelayedView.setListener(null);

        // cancel selected, so go back to listview
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                setListViewVisible(true);
            }
        }, 50);
    }
}
