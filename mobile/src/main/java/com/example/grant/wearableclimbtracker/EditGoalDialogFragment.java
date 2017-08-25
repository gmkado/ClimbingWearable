package com.example.grant.wearableclimbtracker;

import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.InputType;
import android.text.ParcelableSpan;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
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

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.SimpleSpanBuilder;
import com.example.mysynclibrary.realm.Goal;
import com.farbod.labelledspinner.LabelledSpinner;
import com.github.clans.fab.Label;
import com.polyak.iconswitch.IconSwitch;

import org.honorato.multistatetogglebutton.MultiStateToggleButton;
import org.honorato.multistatetogglebutton.ToggleButton;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.temporal.ChronoUnit;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import io.realm.Realm;


public class EditGoalDialogFragment extends DialogFragment {
    private static final String ARG_CLIMBTYPE = "climbtype";
    private static final String ARG_GOALUUID = "goalUUID";
    private static final String TAG = "EditGoalDialogFragment";
    private static final String SPACE_STRING = "   ";

    private Shared.ClimbType mClimbType;
    private String mGoalUUID;
    private Realm mRealm;
    private Button mSaveButton;
    private Goal mGoal;
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
    public static EditGoalDialogFragment newInstance(Shared.ClimbType climbType, String climbUuid) {
        EditGoalDialogFragment fragment = new EditGoalDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CLIMBTYPE, climbType.ordinal());
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
        View v = inflater.inflate(R.layout.dialog_edit_goal, container, false);

        // get all the UI elements
        mSaveButton = (Button)v.findViewById(R.id.save_button);
        mGoalSummary = (TextView) v.findViewById(R.id.textView_goalSummary);

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
                        realm.beginTransaction();
                        realm.where(Goal.class).equalTo("id", mGoalUUID).findFirst().deleteFromRealm();
                        realm.commitTransaction();
                    } finally {
                        realm.close();
                        dismiss();
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
            mGoal.setIncludeAttempts(false);
            deleteButton.setVisibility(View.GONE);
        }
        updateGoalSpannable();


        /***************** SAVE BUTTON *******************/
        mSaveButton.setEnabled(false);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // save the climb
                Realm realm = Realm.getDefaultInstance();
                try{
                    realm.beginTransaction();
                    realm.copyToRealmOrUpdate(mGoal);
                    realm.commitTransaction();
                }finally{
                    realm.close();
                    dismiss();
                }
            }
        });

        checkGoalValidity();
        return v;
    }

    private void updateGoalSpannable() {
        // I want to <BOULDER/ROPE> climb <#> <CLIMBS/POINTS/HEIGHT> at least <V#/5.##>
        //      if HEIGHT replace with "<FEET/METERS>"
        // starting <TODAY> and <RECURRING/NOT RECURRING>
        //      if RECURRING add "every <SESSION/WEEK/MONTH/YEAR>" (this will automatically link to end type period)
        // ending <DATE/PERIOD/NEVER>
        //      if DATE replace with <MM/DD/YYYY>
        //      if PERIOD replace with <AFTER> and add "<#> <SESSION/WEEK/MONTH/YEAR>"  (this will automatically link to recurring period)

        DateFormat df = SimpleDateFormat.getDateInstance(DateFormat.SHORT);
        SimpleSpanBuilder ssb = new SimpleSpanBuilder();
        ssb.append("I want to")
                .append(SPACE_STRING)
                .append(mGoal.getClimbType().name(),
                        new ClickableSpan() {
                            @Override
                            public void onClick(View widget) {
                                new MaterialDialog.Builder(getContext())
                                        .title("Climb type")
                                        .items("boulder", "ropes")
                                        .itemsCallbackSingleChoice(mGoal.getClimbType().ordinal(), new MaterialDialog.ListCallbackSingleChoice() {
                                            @Override
                                            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                                mGoal.setClimbType(Shared.ClimbType.values()[which]);
                                                if(mGoal.getMingrade() > mGoal.getClimbType().grades.size()-1) {
                                                    mGoal.setMingrade(mGoal.getClimbType().grades.size()-1);
                                                }
                                                updateGoalSpannable();
                                                return true;
                                            }
                                        })
                                        .positiveText("Choose")
                                        .show();
                            }
                        })
                .append(SPACE_STRING)
                .append(Integer.toString(mGoal.getTarget()), new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        new MaterialDialog.Builder(getContext())
                                .title("Input Target")
                                .inputType(InputType.TYPE_CLASS_NUMBER)
                                .input("", Integer.toString(mGoal.getTarget()), new MaterialDialog.InputCallback() {
                                    @Override
                                    public void onInput(MaterialDialog dialog, CharSequence input) {
                                        try {
                                            int target = Integer.parseInt(input.toString());
                                            if (target > 0) {
                                                mGoal.setTarget(target);
                                                updateGoalSpannable();
                                            }
                                        }catch (NumberFormatException e) {
                                            Log.e(TAG, "Bad number format");
                                        }
                                    }
                                }).show();
                    }
                })
                .append(SPACE_STRING);

        ClickableSpan goalUnitSpan =  new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                new MaterialDialog.Builder(getContext())
                        .title("Goal unit")
                        .items(Goal.GoalUnit.getStringArray())
                        .itemsCallbackSingleChoice(mGoal.getGoalUnit().ordinal(), new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                Goal.GoalUnit gu = Goal.GoalUnit.values()[which];
                                if(gu == Goal.GoalUnit.HEIGHT) {
                                    new MaterialDialog.Builder(getContext())
                                            .title("Height unit")
                                            .items(Goal.HeightUnit.getStringArray())
                                            .itemsCallbackSingleChoice(mGoal.getHeightunit().ordinal(), new MaterialDialog.ListCallbackSingleChoice() {
                                                @Override
                                                public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                                    mGoal.setGoalunit(Goal.GoalUnit.HEIGHT);
                                                    mGoal.setHeightunit(Goal.HeightUnit.values()[which]);
                                                    updateGoalSpannable();
                                                    return true;
                                                }
                                            })
                                            .positiveText("Choose")
                                            .show();
                                }else {
                                    mGoal.setGoalunit(gu);
                                    updateGoalSpannable();
                                }
                                return true;
                            }
                        })
                        .positiveText("Choose")
                        .show();
            }
        };
        if(mGoal.getGoalUnit() == Goal.GoalUnit.HEIGHT) {
            ssb.append(mGoal.getHeightunit().name(), goalUnitSpan);
        }else {
            ssb.append(mGoal.getGoalUnit().name(), goalUnitSpan);
        }
        ssb.append(SPACE_STRING)
                .append("at least")
                .append(SPACE_STRING)
                .append(mGoal.getClimbType().grades.get(mGoal.getMingrade()), new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        new MaterialDialog.Builder(getContext())
                                .title("Select a grade")
                                .items(mGoal.getClimbType().grades)
                                .itemsCallback(new MaterialDialog.ListCallback() {
                                    @Override
                                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                        mGoal.setMingrade(which);
                                        updateGoalSpannable();
                                    }
                                })
                                .show();
                    }
                })
                .append(SPACE_STRING)
                .append("starting")
                .append(SPACE_STRING)
                .append(df.format(mGoal.getStartDate()), new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        // Inflate the datepicker view and set the initial date
                        LayoutInflater li = LayoutInflater.from(getContext());
                        View datePickerView = li.inflate(R.layout.dialog_datepicker, null);
                        DatePicker dp = (DatePicker) datePickerView.findViewById(R.id.datePicker);
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(mGoal.getStartDate());
                        dp.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), null); // HACK: stupid workaround since there is no mDatePicker.setOnDateChangedListener()

                        // show the datepicker
                        new MaterialDialog.Builder(getContext())
                                .title("Pick start date")
                                .customView(datePickerView, false)
                                .positiveText(android.R.string.ok)
                                .negativeText(android.R.string.cancel)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        DatePicker dp = (DatePicker) dialog.getCustomView().findViewById(R.id.datePicker);
                                        Calendar cal = Calendar.getInstance();
                                        cal.set(Calendar.YEAR, dp.getYear());
                                        cal.set(Calendar.MONTH, dp.getMonth());
                                        cal.set(Calendar.DAY_OF_MONTH, dp.getDayOfMonth());
                                        cal.set(Calendar.HOUR_OF_DAY, 0);
                                        cal.set(Calendar.MINUTE, 0);
                                        cal.set(Calendar.SECOND, 0);
                                        cal.set(Calendar.MILLISECOND, 0);

                                        mGoal.setStartDate(cal.getTime());
                                        updateGoalSpannable();
                                    }
                                })
                                .show();
                    }
                })
                .append(SPACE_STRING)
                .append("and")
                .append(SPACE_STRING)
                .append(mGoal.isRecurring() ? "RECURRING" : "NOT RECURRING", new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        new MaterialDialog.Builder(getContext())
                                .title("Recurring?")
                                .items("RECURRING", "NOT RECURRING")
                                .itemsCallbackSingleChoice(mGoal.isRecurring() ? 0 : 1, new MaterialDialog.ListCallbackSingleChoice() {
                                    @Override
                                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                        mGoal.setRecurring(which == 0);
                                        updateGoalSpannable();
                                        return true;
                                    }
                                })
                                .positiveText("Choose")
                                .show();
                    }
                })
                .append(SPACE_STRING);


        if(mGoal.isRecurring()) {
            ssb.append("every")
                    .append(SPACE_STRING)
                    .append(mGoal.getPeriod().name(), new ClickableSpan() {
                        @Override
                        public void onClick(View widget) {
                            new MaterialDialog.Builder(getContext())
                                    .title("Period")
                                    .items(Goal.Period.getStringArray())
                                    .itemsCallbackSingleChoice(mGoal.getPeriod().ordinal(), new MaterialDialog.ListCallbackSingleChoice() {
                                        @Override
                                        public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                            mGoal.setPeriod(Goal.Period.values()[which]);
                                            updateGoalSpannable();
                                            return true;
                                        }
                                    })
                                    .positiveText("Choose")
                                    .show();
                        }
                    })
                    .append(SPACE_STRING);
        }
        ssb.append("ending")
                .append(SPACE_STRING);


        ClickableSpan endTypeSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                new MaterialDialog.Builder(getContext())
                        .title("End Type")
                        .items(Goal.EndType.getStringArray())
                        .itemsCallbackSingleChoice(mGoal.getEndType().ordinal(), new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                switch (Goal.EndType.values()[which]) {
                                    case NEVER:
                                        mGoal.setEndtype(Goal.EndType.NEVER);
                                        updateGoalSpannable();
                                        break;
                                    case DATE:
                                        // Inflate the datepicker view and set the initial date
                                        LayoutInflater li = LayoutInflater.from(getContext());
                                        View datePickerView = li.inflate(R.layout.dialog_datepicker, null);
                                        DatePicker dp = (DatePicker) datePickerView.findViewById(R.id.datePicker);
                                        Calendar cal = Calendar.getInstance();
                                        cal.setTime(mGoal.getEndDate() == null ? mGoal.getStartDate(): mGoal.getEndDate());
                                        dp.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), null); // HACK: stupid workaround since there is no mDatePicker.setOnDateChangedListener()

                                        // show the datepicker
                                        new MaterialDialog.Builder(getContext())
                                                .title("Pick end date")
                                                .customView(datePickerView, false)
                                                .positiveText(android.R.string.ok)
                                                .negativeText(android.R.string.cancel)
                                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                    @Override
                                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                        DatePicker dp = (DatePicker) dialog.getCustomView().findViewById(R.id.datePicker);
                                                        Calendar cal = Calendar.getInstance();
                                                        cal.set(Calendar.YEAR, dp.getYear());
                                                        cal.set(Calendar.MONTH, dp.getMonth());
                                                        cal.set(Calendar.DAY_OF_MONTH, dp.getDayOfMonth());
                                                        cal.set(Calendar.HOUR_OF_DAY, 0);
                                                        cal.set(Calendar.MINUTE, 0);
                                                        cal.set(Calendar.SECOND, 0);
                                                        cal.set(Calendar.MILLISECOND, 0);
                                                        mGoal.setEndtype(Goal.EndType.DATE);
                                                        mGoal.setEndDate(cal.getTime());
                                                        updateGoalSpannable();
                                                    }
                                                })
                                                .show();
                                        break;
                                    case PERIOD:
                                        new MaterialDialog.Builder(getContext())
                                                .title("Input number of periods")
                                                .inputType(InputType.TYPE_CLASS_NUMBER)
                                                .input("", Integer.toString(mGoal.getNumPeriods()), new MaterialDialog.InputCallback() {
                                                    @Override
                                                    public void onInput(MaterialDialog dialog, CharSequence input) {
                                                        try {
                                                            int numPeriods = Integer.parseInt(input.toString());
                                                            if (numPeriods > 0) {
                                                                mGoal.setNumPeriods(numPeriods);
                                                                mGoal.setEndtype(Goal.EndType.PERIOD);
                                                                updateGoalSpannable();
                                                            }
                                                        } catch (NumberFormatException e) {
                                                            Log.e(TAG, "Bad number format");
                                                        }
                                                    }
                                                }).show();
                                        break;
                                }
                                return true;
                            }
                        })
                        .positiveText("Choose")
                        .show();
            }
        };
        switch(mGoal.getEndType()) {
            case NEVER:
                ssb.append(mGoal.getEndType().name(), endTypeSpan);
                break;
            case DATE:
                ssb.append(df.format(mGoal.getEndDate()), endTypeSpan);
                break;
            case PERIOD:
                ssb.append(String.format(Locale.getDefault(), "after %d", mGoal.getNumPeriods()), endTypeSpan)
                        .append(SPACE_STRING)
                        .append(mGoal.getPeriod().name(), new ClickableSpan() {
                            @Override
                            public void onClick(View widget) {
                                new MaterialDialog.Builder(getContext())
                                        .title("Period")
                                        .items(Goal.Period.getStringArray())
                                        .itemsCallbackSingleChoice(mGoal.getPeriod().ordinal(), new MaterialDialog.ListCallbackSingleChoice() {
                                            @Override
                                            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                                mGoal.setPeriod(Goal.Period.values()[which]);
                                                updateGoalSpannable();
                                                return true;
                                            }
                                        })
                                        .positiveText("Choose")
                                        .show();
                            }
                        });
                break;
        }

        mGoalSummary.setText(ssb.build());
        mGoalSummary.setClickable(true);
        mGoalSummary.setMovementMethod(LinkMovementMethod.getInstance()); // NOTE: https://stackoverflow.com/questions/8641343/android-clickablespan-not-calling-onclick


        checkGoalValidity();
    }

    /*private void updateClimbTypeUI() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),R.layout.support_simple_spinner_dropdown_item, mGoal.getClimbType().grades);
        mGradeSpinner.setCustomAdapter(adapter);
    }*/

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
        window.setLayout((int) (width * 0.9), (int) (height * 0.9));
        window.setGravity(Gravity.CENTER);
    }


    private void checkGoalValidity() {
        //mGoalSummary.setText(mGoal.getSummary());
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
