package com.example.grant.wearableclimbtracker;

import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.eventbus.EditGoalDialogEvent;
import com.example.mysynclibrary.realm.Goal;

import org.greenrobot.eventbus.EventBus;
import org.honorato.multistatetogglebutton.MultiStateToggleButton;
import org.honorato.multistatetogglebutton.ToggleButton;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.Calendar;
import java.util.UUID;

import io.realm.Realm;


public class EditGoalDialogFragment extends DialogFragment {
    private static final String ARG_CLIMBTYPE = "climbtype";
    private static final String ARG_GOALUUID = "goalUUID";
    private static final String TAG = "EditClimbDialogFragment";

    private Shared.ClimbType mClimbType;
    private String mGoalUUID;
    private Realm mRealm;
    private Button mSaveButton;
    private Goal mGoal;
    private MultiStateToggleButton mGoalUnitMSTB;
    private MultiStateToggleButton mPeriodMSTB;
    private MultiStateToggleButton mEndTypeMSTB;
    private DatePicker mEndDatePicker;
    private LinearLayout mPeriodLayout;
    private Spinner mGradeSpinner;
    private Switch mClimbTypeSwitch;
    private TextView mTargetSuffix;
    private EditText mTargetEditText;
    private Spinner mHeightUnitSpinner;
    private TextView mPeriodSuffix;
    private TextView mGoalSummary;

    public EditGoalDialogFragment() {
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
    public static EditGoalDialogFragment newInstance(int climbType, String climbUuid) {
        EditGoalDialogFragment fragment = new EditGoalDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CLIMBTYPE, climbType);
        args.putString(ARG_GOALUUID, climbUuid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mClimbType = Shared.ClimbType.values()[getArguments().getInt(ARG_CLIMBTYPE)];
            mGoalUUID = getArguments().getString(ARG_GOALUUID);
        }

        mRealm = Realm.getDefaultInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_edit_goal_dialog, container, false);
        TabHost host = (TabHost)v.findViewById(R.id.tab_host);
        host.setup();

        //Tab 1
        TabHost.TabSpec spec = host.newTabSpec("DETAILS");
        spec.setContent(R.id.details);
        spec.setIndicator("DETAILS");
        host.addTab(spec);

        //Tab 2
        spec = host.newTabSpec("START");
        spec.setContent(R.id.startdate);
        spec.setIndicator("START");
        host.addTab(spec);

        //Tab 3
        spec = host.newTabSpec("END");
        spec.setContent(R.id.enddate);
        spec.setIndicator("END");
        host.addTab(spec);

        // get all the UI elements
        mGoalUnitMSTB = (MultiStateToggleButton)v.findViewById(R.id.mstb_goalunit);
        mEndTypeMSTB = (MultiStateToggleButton)v.findViewById(R.id.mstb_goalendtype);
        mPeriodMSTB = (MultiStateToggleButton)v.findViewById(R.id.mstb_period);
        mSaveButton = (Button)v.findViewById(R.id.save_button);
        mEndDatePicker = (DatePicker)v.findViewById(R.id.datePicker_enddate);
        mPeriodLayout = (LinearLayout) v.findViewById(R.id.layout_period);
        mClimbTypeSwitch = (Switch) v.findViewById(R.id.switch_climbtype);
        mGradeSpinner = (Spinner) v.findViewById(R.id.spinner_grade);
        mTargetSuffix = (TextView) v.findViewById(R.id.textView_targetSuffix);
        mPeriodSuffix = (TextView) v.findViewById(R.id.textview_periodSuffix);
        mGoalSummary = (TextView) v.findViewById(R.id.textView_goalSummary);
        mTargetEditText = (EditText) v.findViewById(R.id.editText_target);
        mHeightUnitSpinner = (Spinner) v.findViewById(R.id.spinner_heightunit);
        /************** Try getting goal***************************/
        Button deleteButton = (Button) v.findViewById(R.id.delete_button);
        if(mGoalUUID !=null) {
            Goal goal  = mRealm.where(Goal.class).equalTo("id", mGoalUUID).findFirst();
            mGoal = mRealm.copyFromRealm(goal);  // detach from realm so changes can be made without saving until save button is pressed
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Realm realm = Realm.getDefaultInstance();
                    try {
                        realm.executeTransactionAsync(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                realm.where(Goal.class).equalTo("id", mGoalUUID).findFirst().deleteFromRealm();
                            }
                        }, new Realm.Transaction.OnSuccess() {

                            @Override
                            public void onSuccess() {
                                dismiss();
                            }
                        });
                    } finally {
                        realm.close();
                    }
                }
            });
        }else {
            mGoal = new Goal(); // This is unmanaged, only gets saved when we press the save button
            mGoal.setId(UUID.randomUUID().toString());
            mGoal.setClimbType(mClimbType);
            ZonedDateTime zdt = Shared.DateToZDT(Calendar.getInstance().getTime());
            mGoal.setStartDate(Shared.ZDTToDate(zdt.truncatedTo(ChronoUnit.DAYS)));
            mGoal.setMingrade(0);
            deleteButton.setVisibility(View.GONE);
        }

        /******************* DETAILS **********************************/
        mClimbTypeSwitch.setChecked(mGoal.getClimbType()== Shared.ClimbType.bouldering);
        mClimbTypeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mGoal.setClimbType(isChecked? Shared.ClimbType.bouldering: Shared.ClimbType.ropes);
                updateClimbTypeUI();
            }
        });
        updateClimbTypeUI();

        mHeightUnitSpinner.setAdapter(new ArrayAdapter<>(getContext(),R.layout.support_simple_spinner_dropdown_item, Goal.HeightUnit.getStringArray()));
        mHeightUnitSpinner.setSelection(mGoal.getHeightunit().ordinal());
        mHeightUnitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mGoal.setHeightunit(Goal.HeightUnit.values()[position]);
                checkGoalValidity();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        // TargetEditText and GradeEditText are set once initially depending on the initial unitType.  After that, they are never set programmatically, only driven from the user
        // This allows us to keep track of two different numbers using the UI
        mGradeSpinner.setSelection(mGoal.getMingrade()); // must happen after updateClimbTypeUI
        mTargetEditText.setText(Integer.toString(mGoal.getTarget()));

        mTargetEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    mGoal.setTarget(Integer.parseInt(s.toString()));
                    checkGoalValidity();
                }catch(NumberFormatException e) {
                }
            }
        });
        mGradeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mGoal.setMingrade(position);
                checkGoalValidity();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mGoalUnitMSTB.setElements(Goal.GoalUnit.getStringArray());
        mGoalUnitMSTB.setValue(mGoal.getGoalUnit().ordinal());
        mGoalUnitMSTB.setOnValueChangedListener(new ToggleButton.OnValueChangedListener() {
            @Override
            public void onValueChanged(int value) {
                mGoal.setGoalunit(Goal.GoalUnit.values()[mGoalUnitMSTB.getValue()]);
                mGoal.setTarget(Integer.parseInt(mTargetEditText.getText().toString()));

                checkGoalValidity();
                updateUnitUI();
            }
        });
        updateUnitUI();


        /******************* START **********************************/
        DatePicker startPicker = (DatePicker) v.findViewById(R.id.datepicker_startdate);
        Calendar cal = Calendar.getInstance();
        cal.setTime(mGoal.getStartDate());

        // stupid workaround since there is no mDatePicker.setOnDateChangedListener()
        startPicker.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {

            @Override
            public void onDateChanged(DatePicker datePicker, int year, int month, int dayOfMonth) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, month);
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                mGoal.setStartDate(cal.getTime());
                checkGoalValidity();
            }
        });

        /******************* END - RECURRENCE **********************************/
        mPeriodMSTB.setElements(Goal.Period.getStringArray());
        mPeriodMSTB.setValue(mGoal.getPeriod().ordinal());

        Switch mRecurringSwitch = (Switch) v.findViewById(R.id.switch_recurring);
        mRecurringSwitch.setChecked(mGoal.isRecurring());
        mRecurringSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mGoal.setRecurring(isChecked);
                if(!isChecked && mGoal.getEndType() == Goal.EndType.PERIOD) {
                    mGoal.setEndtype(Goal.EndType.NO_END);
                    mEndTypeMSTB.setValue(Goal.EndType.NO_END.ordinal());
                }
                checkGoalValidity();
                updateRecurrenceUI();
            }
        });
        mPeriodMSTB.setOnValueChangedListener(new ToggleButton.OnValueChangedListener() {
            @Override
            public void onValueChanged(int value) {
                mGoal.setPeriod(Goal.Period.values()[mPeriodMSTB.getValue()]);
                checkGoalValidity();
                updatePeriodUI();
            }
        });
        updatePeriodUI();
        updateRecurrenceUI();

        /******************* END - TYPE **********************************/
        // mEndTypeMSTB elements are set in updateRecurrenceUI, so we don't need to do it here
        mEndTypeMSTB.setOnValueChangedListener(new ToggleButton.OnValueChangedListener() {
            @Override
            public void onValueChanged(int value) {
                mGoal.setEndtype(Goal.EndType.values()[mEndTypeMSTB.getValue()]);
                checkGoalValidity();
                updateEndTypeUI();
            }
        });
        updateEndTypeUI();

        // set initial values
        EditText numPeriodEditText = (EditText) v.findViewById(R.id.edittext_numperiod);
        if(mGoal.getEndType() == Goal.EndType.DATE) {
            cal.setTime(mGoal.getEndDate());
        }else {
            ZonedDateTime zdt = Shared.DateToZDT(Calendar.getInstance().getTime());
            mGoal.setEndDate(Shared.ZDTToDate(zdt.truncatedTo(ChronoUnit.DAYS)));
            mGoal.setEndDate(cal.getTime());
            if(mGoal.getEndType() == Goal.EndType.PERIOD) {
                numPeriodEditText.setText(Integer.toString(mGoal.getNumPeriods()));
            }
        }
        // stupid workaround since there is no mDatePicker.setOnDateChangedListener()
        mEndDatePicker.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {

            @Override
            public void onDateChanged(DatePicker datePicker, int year, int month, int dayOfMonth) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, month);
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                mGoal.setEndDate(cal.getTime());
                checkGoalValidity();
            }
        });
        numPeriodEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try { // so we don't try to parse an empty string
                    mGoal.setNumPeriods(Integer.parseInt(s.toString()));
                    checkGoalValidity();
                }catch(NumberFormatException e) {
                }
            }
        });

        /***************** SAVE AND DELETE BUTTONS *******************/
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
                            realm.copyToRealmOrUpdate(mGoal);

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

        checkGoalValidity();
        return v;
    }

    private void updateClimbTypeUI() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),R.layout.support_simple_spinner_dropdown_item, mGoal.getClimbType().grades);
        mGradeSpinner.setAdapter(adapter);
        if(mClimbTypeSwitch.isChecked()) {
            // bouldering
            mClimbTypeSwitch.setText("Climb Type: Bouldering");
        }else {
            // ropes
            mClimbTypeSwitch.setText("Climb Type: Ropes");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // set the window height so it doesn't keep changing, as per https://stackoverflow.com/questions/26974068/how-to-set-the-width-of-a-dialogfragment-in-percentage/28596836
        Window window = getDialog().getWindow();
        Point size = new Point();

        Display display = window.getWindowManager().getDefaultDisplay();
        display.getSize(size);

        int width = size.x;
        int height = size.y;
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, (int) (height * 0.9));
        window.setGravity(Gravity.CENTER);
    }

    /**
     * Call this when the end type is changed or initially set
     */
    private void updateEndTypeUI() {
        switch(Goal.EndType.values()[mEndTypeMSTB.getValue()]) {
            case NO_END:
                mPeriodLayout.setVisibility(View.GONE);
                mEndDatePicker.setVisibility(View.GONE);
                break;
            case DATE:
                mPeriodLayout.setVisibility(View.GONE);
                mEndDatePicker.setVisibility(View.VISIBLE);
                break;
            case PERIOD:
                mPeriodLayout.setVisibility(View.VISIBLE);
                mEndDatePicker.setVisibility(View.GONE);
                break;
        }

    }

    /**
     * Call this when the duration is changed or initially set
     */
    private void updatePeriodUI() {
        switch(Goal.Period.values()[mPeriodMSTB.getValue()]) {
            case SESSION:
                mPeriodSuffix.setText("sessions");
                break;
            case WEEKLY:
                mPeriodSuffix.setText("weeks");
                break;
            case MONTHLY:
                mPeriodSuffix.setText("months");
                break;
            case YEARLY:
                mPeriodSuffix.setText("years");
                break;
        }
    }

    /**
     * Call this when the unit of the goal is changed or initially set
     */
    private void updateUnitUI() {
        switch(Goal.GoalUnit.values()[mGoalUnitMSTB.getValue()]) {
            case POINTS:
                mTargetEditText.setVisibility(View.VISIBLE);
                mTargetSuffix.setVisibility(View.VISIBLE);
                mHeightUnitSpinner.setVisibility(View.GONE);
                mTargetSuffix.setText("points");
                break;
            case HEIGHT:
                mTargetEditText.setVisibility(View.VISIBLE);
                mTargetSuffix.setVisibility(View.GONE);
                mHeightUnitSpinner.setVisibility(View.VISIBLE);
                break;
            case CLIMBS:
                mTargetEditText.setVisibility(View.VISIBLE);
                mTargetSuffix.setVisibility(View.VISIBLE);
                mHeightUnitSpinner.setVisibility(View.GONE);
                mTargetSuffix.setText("climbs");
                break;

        }
    }

    /**
     * Call this when isRecurring is changed or initially set
     */
    private void updateRecurrenceUI() {
        boolean isRecurring = mGoal.isRecurring();
        mEndTypeMSTB.setElements(Goal.EndType.getStringArray(isRecurring));
        mPeriodMSTB.setVisibility(isRecurring?View.VISIBLE:View.GONE);

        // if "Period" is selected and recurring has been changed to false, then we should change mgoal to "no-end" since "period is no longer available
        int endtype = mGoal.getEndType().ordinal();
        if(endtype < mEndTypeMSTB.getStates().length) {
            mEndTypeMSTB.setValue(endtype);
        }

    }

    private void checkGoalValidity() {
        mGoalSummary.setText(mGoal.getSummary());
        if(mGoal.isValidGoal()) {
            mSaveButton.setEnabled(true);
        }else {
            mSaveButton.setEnabled(false);
        }

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

}
