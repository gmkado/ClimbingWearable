package com.example.grant.wearableclimbtracker;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.DelayedConfirmationView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.realm.ClimbFields;

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
        mListView = (ListView)findViewById(R.id.listView);
        mListView.setOnItemClickListener(AddClimbActivity.this);
        setGradeListAdapter();

        mDelayedView = (DelayedConfirmationView) findViewById(R.id.saveDelayedConfirmationView);
        setDelayedViewVisible(false);
    }

    public void setGradeListAdapter() {
        // get the appropriate climb type preferences and grade lists
        List<String> gradeList = mClimbType.grades;

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean warmupEnabled = pref.getBoolean(Shared.KEY_WARMUP_ENABLED, false);

        //	if warmup enabled && date = today
        if(warmupEnabled) {
            String maxGradeKey;
            String numClimbsKey;

            if (mClimbType == Shared.ClimbType.bouldering) {
                maxGradeKey = Shared.KEY_WARMUP_MAXGRADE_BOULDER;
                numClimbsKey = Shared.KEY_WARMUP_NUMCLIMBS_BOULDER;
            }else {
                maxGradeKey = Shared.KEY_WARMUP_MAXGRADE_ROPES;
                numClimbsKey = Shared.KEY_WARMUP_NUMCLIMBS_ROPES;
            }

            // get all climbs from today
            long numClimbs = mRealm.where(Climb.class).equalTo(ClimbFields.TYPE, mClimbType.ordinal()).greaterThanOrEqualTo("date", Shared.getStartofDate(null)).count();
            if (numClimbs < pref.getInt(numClimbsKey, 0)) {
                // New Listadapter (shortlist)

                int maxGradeInd = gradeList.indexOf(pref.getString(maxGradeKey, gradeList.get(0)));
                mListView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_activated_1, gradeList.subList(0, maxGradeInd + 1)));

                return;
            }
        }
        // otherwise continue and update with full list
        mListView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_activated_1, gradeList));
    }

    private void setDelayedViewVisible(boolean delayedViewVisible) {
        if(delayedViewVisible){
            findViewById(R.id.listViewLayout).setVisibility(View.GONE);
            findViewById(R.id.saveConfirmationLayout).setVisibility(View.VISIBLE);
        }else{

            findViewById(R.id.listViewLayout).setVisibility(View.VISIBLE);
            findViewById(R.id.saveConfirmationLayout).setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "onItemClick");
        mSelectedPosition = position;

        // set list view
        TextView tv = (TextView) findViewById(R.id.saveConfirmationTextView);
        tv.setText(String.format("Saving %s send", mGradeList.get(position)));
        setDelayedViewVisible(true);


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


        mRealm.executeTransactionAsync(new Realm.Transaction() {

                                           @Override
                                           public void execute(Realm realm) {
                                               // TODO: restructure this like mobile -> climb.setGrade(mSelectedPosition);
                                               Climb climb = new Climb(mClimbType, null, null);
                                               climb.setGrade(mSelectedPosition);
                                               realm.copyToRealm(climb);
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
                setDelayedViewVisible(false);
            }
        }, 50);
    }

}
