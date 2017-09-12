package com.example.grant.wearableclimbtracker;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.realm.Attempt;
import com.example.mysynclibrary.realm.AttemptFields;
import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.realm.ClimbFields;
import com.github.florent37.androidslidr.Slidr;
import com.shawnlin.numberpicker.NumberPicker;

import java.util.Calendar;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmResults;

import static com.example.mysynclibrary.realm.ISyncableRealmObject.SyncState.DELETE;

/**
 * Created by Grant on 7/15/2017.
 */

public class EditAttemptsDialogFragment extends DialogFragment {
    private static final java.lang.String ARG_CLIMB_ID = "climbUUID";
    private static final String ARG_ATTEMPT_ID = "attemptID";
    private Climb mClimb;
    private String mAttemptId;
    private Attempt mAttempt;
    private Button mSaveButton;

    @Override
    public void onDestroyView() {
        mRealm.close();
        super.onDestroyView();
    }

    private Realm mRealm;

    public EditAttemptsDialogFragment() {}

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment EditClimbDialogFragment.
     */
    public static EditAttemptsDialogFragment newInstance(String climbId, Attempt attempt) {
        EditAttemptsDialogFragment fragment = new EditAttemptsDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CLIMB_ID, climbId);
        args.putString(ARG_ATTEMPT_ID, attempt==null? null : attempt.getId());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRealm = Realm.getDefaultInstance();
        if (getArguments() != null) {
            mClimb = mRealm.copyFromRealm(mRealm.where(Climb.class).equalTo(ClimbFields.ID,getArguments().getString(ARG_CLIMB_ID)).findFirst()); // Have to use an unmanaged object
            mAttemptId = getArguments().getString(ARG_ATTEMPT_ID);
        }else {
            // shouldn't get here
            throw new IllegalArgumentException("Missing climb id in EditAttemptsDialogFragment");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_add_attempts, container, false);

        Button deleteButton = (Button) v.findViewById(R.id.delete_button);
        mSaveButton = (Button) v.findViewById(R.id.save_button);

        if(mAttemptId != null) {
            Attempt attempt = mRealm.where(Attempt.class).equalTo(AttemptFields.ID, mAttemptId).findFirst();
            mAttempt = mRealm.copyFromRealm(attempt);  // detach from realm so changes can be made without saving until save button is pressed
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try (Realm realm = Realm.getDefaultInstance()) {
                        realm.beginTransaction();
                        Attempt attempt = realm.where(Attempt.class).equalTo(AttemptFields.ID, mAttemptId).findFirst();
                        attempt.safedelete(false);
                        realm.commitTransaction();
                    } finally {
                        dismiss();
                    }
                }
            });

        }else {
            // Try to set the current progress to the most recent progress
            RealmResults<Attempt> attempts = mRealm.where(Attempt.class)
                    .equalTo(AttemptFields.CLIMB.ID, mClimb.getId())
                    .notEqualTo(AttemptFields.SYNC_STATE, DELETE.name()).findAllSorted("date");
            float progress = 0f;
            if(!attempts.isEmpty()) {
                progress = attempts.last().getProgress();
            }
            // create unmanaged attempt
            mAttempt = Attempt.createAttempt(mClimb, progress, 1, false);

        }


        // count
        NumberPicker np = (NumberPicker)v.findViewById(R.id.number_picker);
        np.setValue(mAttempt.getCount());
        np.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                mAttempt.setCount((newVal));
            }
        });

        // progress
        Slidr slidr = (Slidr) v.findViewById(R.id.slidr_progress);
        slidr.setMax(100);

        // is lead
        Switch leadSwitch = (Switch)v.findViewById(R.id.switch_leadAttempt);
        if(mClimb.getType() == Shared.ClimbType.bouldering) {
            leadSwitch.setVisibility(View.GONE);
        }else {
            leadSwitch.setVisibility(View.VISIBLE);
            if (mAttempt.isOnLead()) {
                leadSwitch.setChecked(true);
            }
            leadSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mAttempt.setOnLead(isChecked);
                }
            });
        }
        slidr.setTextFormatter(new PercentTextFormatter());
        slidr.setCurrentValue(mAttempt.getProgress());
        slidr.setListener(new Slidr.Listener() {
            @Override
            public void valueChanged(Slidr slidr, float currentValue) {
                mAttempt.setProgress(currentValue);
            }

            @Override
            public void bubbleClicked(Slidr slidr) {

            }
        });

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // save the climb
                try (Realm realm = Realm.getDefaultInstance()) {
                    realm.beginTransaction();
                    realm.copyToRealmOrUpdate(mAttempt);
                    realm.commitTransaction();
                } finally {
                    dismiss();
                }
            }
        });
        return v;
    }

    public class PercentTextFormatter implements Slidr.TextFormatter {

        @Override
        public String format(float value) {
            return String.format("%d %%", (int) value);
        }
    }
}
