package com.example.grant.wearableclimbtracker;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.realm.Climb;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.realm.Realm;

public class AddClimbActivity extends Activity implements AdapterView.OnItemClickListener, DelayedConfirmationView.DelayedConfirmationListener{
    private static final String TAG = "AddClimbActivity";
    private Shared.ClimbType mClimbType;
    private List<String> mGradeList;
    private DelayedConfirmationView mDelayedView;
    private int mSelectedPosition; // the current selected grade
    private ListView mListView;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    private LinearLayout mDelayedViewLayout;
    private FrameLayout mListViewLayout;
    private Realm mRealm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_climb);

        Bundle extras = getIntent().getExtras();
        mClimbType = (Shared.ClimbType)extras.get(MainActivity.EXTRA_CLIMBTYPE);

        mGradeList = mClimbType.grades;

        mRealm = Realm.getDefaultInstance();

        // setup the listview
        mListViewLayout = (FrameLayout) findViewById(R.id.listViewLayout);
        mDelayedViewLayout = (LinearLayout) findViewById(R.id.delayedConfirmationLayout);

        mListView = (ListView)findViewById(R.id.listView);
        mListView.setOnItemClickListener(AddClimbActivity.this);
        setGradeListAdapter();

        mDelayedView = (DelayedConfirmationView) findViewById(R.id.delayed_confirm);
        setListViewVisible(true);
    }

    public void setGradeListAdapter() {
        // get the appropriate climb type preferences and grade lists
        String maxGradeKey;
        String numClimbsKey;

        if (mClimbType == Shared.ClimbType.bouldering) {
            maxGradeKey = Shared.KEY_MAXGRADE_BOULDER;
            numClimbsKey = Shared.KEY_NUMCLIMBS_BOULDER;
        }else {
            maxGradeKey = Shared.KEY_MAXGRADE_ROPES;
            numClimbsKey = Shared.KEY_NUMCLIMBS_ROPES;
        }
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean warmupEnabled = pref.getBoolean(Shared.KEY_WARMUP_ENABLED, false);

        //	if warmup enabled && date = today
        if(warmupEnabled) {
            // get all climbs from today
            long numClimbs = mRealm.where(Climb.class).equalTo("type", mClimbType.ordinal()).greaterThanOrEqualTo("date", Shared.getStartofDate(null)).count();
            if (numClimbs < pref.getInt(numClimbsKey, 0)) {
                // TODO: check here is we actually need to update the list, keep track of state
                // New Listadapter (shortlist)
                List<String> gradeList = mClimbType.grades;
                int maxGradeInd = gradeList.indexOf(pref.getString(maxGradeKey, gradeList.get(0)));
                mListView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_activated_1, gradeList.subList(0, maxGradeInd+1)));

                return;
            }
        }
        // TODO: check here is we actually need to update the list, keep track of state
        // otherwise continue and update with full list
        mListView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_activated_1, mClimbType.grades));
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
        mDelayedView.setTotalTimeMs(3000);
        // Start the timer
        mDelayedView.start();
    }

    @Override
    public void onTimerFinished(View view) {
        // not cancelled so save the climb
        Log.d(TAG, "onTimerFinished");


        mRealm.executeTransaction(new Realm.Transaction(){

            @Override
            public void execute(Realm realm) {
                Climb climb = mRealm.createObject(Climb.class, UUID.randomUUID().toString());

                Date now = new Date();
                // set climb fields
                climb.setDate(now);
                climb.setLastedit(now);
                climb.setGrade(mSelectedPosition);
                climb.setType(mClimbType.ordinal());
                climb.setDelete(false);


            }
        });

        // show confirmation
        Intent intent = new Intent(this, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                ConfirmationActivity.SUCCESS_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                "Saved");
        startActivity(intent);

        finish();
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
