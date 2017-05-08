package com.example.grant.wearableclimbtracker;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TimePicker;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.eventbus.EditClimbDialogEvent;
import com.example.mysynclibrary.realm.Climb;

import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.realm.Realm;


public class EditClimbDialogFragment extends DialogFragment {
    private static final String ARG_CLIMBTYPE = "climbtype";
    private static final String ARG_CLIMBUUID = "climbUUID";
    private static final String TAG = "EditClimbDialogFragment";

    private Shared.ClimbType mClimbType;
    private String mClimbUUID;
    private Realm mRealm;
    private Integer mGrade;
    private ListView mListView;
    private Button mSaveButton;
    private DatePicker mDatePicker;
    private int mCurrentMaxGradeInd;

    public EditClimbDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param climbType Integer ordinal of climb type enum.
     * @param climbUuid String uuid of climb (if editing a climb.
     * @return A new instance of fragment EditClimbDialogFragment.
     */
    public static EditClimbDialogFragment newInstance(int climbType, String climbUuid) {
        EditClimbDialogFragment fragment = new EditClimbDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CLIMBTYPE, climbType);
        args.putString(ARG_CLIMBUUID, climbUuid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mClimbType = Shared.ClimbType.values()[getArguments().getInt(ARG_CLIMBTYPE)];
            mClimbUUID = getArguments().getString(ARG_CLIMBUUID);
        }

        mRealm = Realm.getDefaultInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_edit_climb_dialog, container, false);
        TabHost host = (TabHost)v.findViewById(R.id.tab_host);
        host.setup();

        //Tab 1
        TabHost.TabSpec spec = host.newTabSpec("GRADE");
        spec.setContent(R.id.tab1);
        spec.setIndicator("GRADE");
        host.addTab(spec);
        // set the listview to the climbtype grades
        mListView = (ListView) v.findViewById(R.id.grade_listview);

        //Tab 2
        spec = host.newTabSpec("DATE");
        spec.setContent(R.id.tab2);
        spec.setIndicator("DATE");
        host.addTab(spec);

        //Tab 3
        spec = host.newTabSpec("TIME");
        spec.setContent(R.id.tab3);
        spec.setIndicator("TIME");
        host.addTab(spec);


        Button deleteButton = (Button) v.findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            Realm realm = Realm.getDefaultInstance();
            try{
                realm.executeTransactionAsync(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        Climb climb = getClimbFromRealm(realm);
                        if(climb.isOnwear()) {
                            // this climb is on the wearable, so we need to keep track of it until we sync
                            climb.setDelete(true);
                        }else {
                            climb.deleteFromRealm();
                        }
                    }
                }, new Realm.Transaction.OnSuccess(){

                    @Override
                    public void onSuccess() {
                        dismiss();
                    }
                });
            }finally {
                realm.close();
            }
            }
        });

        // setup date and time pickers.  If editing a climb, set this to the climbs date/time
        mDatePicker = (DatePicker) v.findViewById(R.id.datePicker);
        final TimePicker tp = (TimePicker) v.findViewById(R.id.timePicker);

        // try to get the climb
        final Climb climb = getClimbFromRealm(mRealm);
        Calendar cal = Calendar.getInstance();
        if(climb != null) {
            // grab values from climb and update GUI

            mGrade = climb.getGrade();
            cal.setTime(climb.getDate());

            // update the time
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                tp.setHour(cal.get(Calendar.HOUR));
                tp.setMinute(cal.get(Calendar.MINUTE));
            }else {
                tp.setCurrentHour(cal.get(Calendar.HOUR));
                tp.setCurrentMinute(cal.get(Calendar.MINUTE));
            }

            // set delete to visible
            deleteButton.setVisibility(View.VISIBLE);
        }else {
            // adding a climb, so set delete to invisible
            deleteButton.setVisibility(View.GONE);
        }

        // stupid workaround since there is no mDatePicker.setOnDateChangedListener()
        mDatePicker.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {

            @Override
            public void onDateChanged(DatePicker datePicker, int year, int month, int dayOfMonth) {
                setGradeListAdapter();
                updateSaveButtonEnabled();
            }
        });

        /*tp.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                updateSaveButtonEnabled();
            }
        });*/

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                mGrade = pos;
                updateSaveButtonEnabled();
            }
        });

        mSaveButton = (Button)v.findViewById(R.id.save_button);
        mSaveButton.setEnabled(false);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // save the climb
                Realm realm = Realm.getDefaultInstance();
                try {
                    realm.executeTransactionAsync(new Realm.Transaction() {

                        @Override
                        public void execute(Realm realm) {
                            // set climb fields
                            Climb climb = getClimbFromRealm(realm);
                            if (climb == null) {
                                // this is a new climb, so we need to create it
                                climb = realm.createObject(Climb.class,  UUID.randomUUID().toString());
                                climb.setType(mClimbType.ordinal());
                            }

                            // get the date/time from pickers
                            Calendar cal = Calendar.getInstance();
                            // this will reflect the current time, so use it to set "lastedit"
                            climb.setLastedit(cal.getTime());

                            cal.set(Calendar.YEAR, mDatePicker.getYear());
                            cal.set(Calendar.MONTH, mDatePicker.getMonth());
                            cal.set(Calendar.DAY_OF_MONTH, mDatePicker.getDayOfMonth());
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                cal.set(Calendar.HOUR_OF_DAY, tp.getHour());
                                cal.set(Calendar.MINUTE, tp.getMinute());
                            } else {
                                cal.set(Calendar.HOUR_OF_DAY, tp.getCurrentHour());
                                cal.set(Calendar.MINUTE, tp.getCurrentMinute());
                            }

                            climb.setOnwear(false);
                            climb.setDelete(false);
                            climb.setDate(cal.getTime());
                            climb.setGrade(mGrade);
                        }
                    }, new Realm.Transaction.OnSuccess() {
                        @Override
                        public void onSuccess() {
                            dismiss();
                        }
                    });
                }finally{
                    realm.close();
                }
            }
        });

        mCurrentMaxGradeInd = -1; // ensure list adapter is created, even if maxGradeInd = 0
        setGradeListAdapter();  // NOTE: this must come after mDatePicker is initialized and mGrade is retrieved from climb
        return v;
    }

    private void updateSaveButtonEnabled() {
        // this only gets called when datepicker, timepicker, or grade is clicked, indicating a change
        if(mGrade!=null) {
            // check this in case we're adding a climb and grade hasn't been selected yet
            mSaveButton.setEnabled(true);
        }
    }

    public void setGradeListAdapter() {
        // get the date/time from pickers
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, mDatePicker.getYear());
        cal.set(Calendar.MONTH, mDatePicker.getMonth());
        cal.set(Calendar.DAY_OF_MONTH, mDatePicker.getDayOfMonth());
        Date pickedDate = cal.getTime();

        List<String> gradeList = mClimbType.grades;

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean warmupEnabled = pref.getBoolean(Shared.KEY_WARMUP_ENABLED, false);


        //	if warmup enabled && date = today
        if(warmupEnabled && pickedDate.after(Shared.getStartofDate(null))) {
            // get the appropriate climb type preferences
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
            long numClimbs = mRealm.where(Climb.class).equalTo("type", mClimbType.ordinal()).greaterThanOrEqualTo("date", Shared.getStartofDate(null)).count();
            if (numClimbs < pref.getInt(numClimbsKey, 0)) {
                // New Listadapter (shortlist)
                int maxGradeInd = gradeList.indexOf(pref.getString(maxGradeKey, gradeList.get(0)));
                if(maxGradeInd != mCurrentMaxGradeInd) {
                    mListView.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_activated_1, gradeList.subList(0, maxGradeInd + 1)));

                    //    If grade is set
                    if (mGrade != null) {
                        //    If  grade > warmup max climb
                        if (mGrade > maxGradeInd) {
                            //    Deselect and update save button
                            mGrade = null;
                            updateSaveButtonEnabled();
                        } else {
                            //    Select grade
                            mListView.setItemChecked(mGrade, true);
                        }
                    }
                    mCurrentMaxGradeInd = maxGradeInd;
                }
                return;
            }
        }
        // otherwise continue and update with full list
        if(gradeList.size()-1 != mCurrentMaxGradeInd) {
            mListView.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_activated_1, gradeList));
            // If grade is set
            if (mGrade != null) {
                //    Select grade
                mListView.setItemChecked(mGrade, true);
            }
            mCurrentMaxGradeInd = gradeList.size() -1;
        }

    }



    private Climb getClimbFromRealm(Realm realm) {
        // try to get the climb
        if(mClimbUUID !=null) {
            return realm.where(Climb.class).equalTo("id", mClimbUUID).findFirst();
        }else {
            return null;
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Log.d(TAG, "onDismiss");
        super.onDismiss(dialog);
        EventBus.getDefault().post(new EditClimbDialogEvent(EditClimbDialogEvent.DialogActionType.DISMISSED, null));
    }
}
