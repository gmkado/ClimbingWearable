package com.example.grant.wearableclimbtracker;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.eventbus.ClimbColorSelectedEvent;
import com.example.mysynclibrary.realm.Area;
import com.example.mysynclibrary.realm.Attempt;
import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.realm.Gym;
import com.farbod.labelledspinner.LabelledSpinner;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmResults;




public class EditClimbDialogFragment extends DialogFragment {
    private static final String ARG_CLIMBTYPE = "climbtype";
    private static final String ARG_CLIMBUUID = "climbUUID";
    private static final String TAG = "EditClimbDialogFragment";
    private static final String ARG_MODE = "editmode";
    private LabelledSpinner mAreaSpinner;

    public enum EditClimbMode{
        ADD_SEND,
        ADD_PROJECT,
        EDIT
    }

    private String mClimbUUID;
    private Realm mRealm;
    private Climb mClimb;
    private Button mSaveButton;
    private EditClimbMode mMode;
    private ListView mGradeListView;
    private Shared.ClimbType mDefaultType;

    public EditClimbDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param climbType T ordinal of climb type enum.
     * @param climbUuid String uuid of climb (if editing a climb.
     * @return A new instance of fragment EditClimbDialogFragment.
     */
    public static EditClimbDialogFragment newInstance(Shared.ClimbType climbType, String climbUuid, EditClimbMode mode) {
        EditClimbDialogFragment fragment = new EditClimbDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CLIMBTYPE, climbType.ordinal());
        args.putString(ARG_CLIMBUUID, climbUuid);
        args.putInt(ARG_MODE, mode.ordinal());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof ColorChooserDialog.ColorCallback))  // NOTE: https://github.com/afollestad/material-dialogs/issues/1368
            throw new RuntimeException("ColorCallback not implemented in EditClimb context");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mClimbUUID = getArguments().getString(ARG_CLIMBUUID);
            mMode = EditClimbMode.values()[getArguments().getInt(ARG_MODE)];
            mDefaultType = Shared.ClimbType.values()[getArguments().getInt(ARG_CLIMBTYPE)];
        }

        mRealm = Realm.getDefaultInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.dialog_edit_climb, container, false);

        // set the listview to the climbtype grades
        mGradeListView = (ListView) v.findViewById(R.id.grade_listview);

        Button deleteButton = (Button) v.findViewById(R.id.delete_button);
        mSaveButton = (Button) v.findViewById(R.id.save_button);
        switch(mMode) {
            case ADD_SEND:
                mSaveButton.setText("ADD SEND");
                deleteButton.setVisibility(View.GONE);
                break;
            case ADD_PROJECT:
                mSaveButton.setText("ADD PROJECT");
                deleteButton.setVisibility(View.GONE);
                break;
            case EDIT:
                mSaveButton.setText("SAVE");
                break;
        }
        if(mClimbUUID != null) {
            Climb climb = mRealm.where(Climb.class).equalTo("id", mClimbUUID).findFirst();
            mClimb = mRealm.copyFromRealm(climb);  // detach from realm so changes can be made without saving until save button is pressed
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Realm realm = Realm.getDefaultInstance();
                    try {
                        realm.beginTransaction();
                        Climb climb = realm.where(Climb.class).equalTo("id", mClimbUUID).findFirst();
                        if (climb.isOnwear()) {
                            // this climb is on the wearable, so we need to keep track of it until we sync
                            climb.setDelete(true);
                        } else {
                            climb.deleteFromRealm();
                        }
                         realm.commitTransaction();
                    } finally {
                        realm.close();
                        dismiss();
                    }
                }
            });

        }else {
            // create unmanaged climb and initialize all default fields here
            mClimb = new Climb();
            mClimb.setId(UUID.randomUUID().toString());
            mClimb.setType(mDefaultType);
            mClimb.setGrade(0);
            mClimb.setColor(-1);
            mClimb.setOnwear(false);
            mClimb.setDelete(false);
            // TODO: set gym and area if filter is set in app
        }
        Calendar cal = Calendar.getInstance();
        // this will reflect the current time, so use it to set "lastedit"
        mClimb.setLastedit(cal.getTime());

        /* ******************** GRADE UI ****************************/
        invalidateGradeList();

        mGradeListView.setItemChecked(mClimb.getGrade(), true);
        mGradeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                mClimb.setGrade(pos);
                checkClimbValidity();
            }
        });

        /* ********************** DETAIL FIELDS ****************/
        // -------  Gym  ------------
        LabelledSpinner gymSpinner = (LabelledSpinner)v.findViewById(R.id.spinner_gym);
        mAreaSpinner = (LabelledSpinner)v.findViewById(R.id.spinner_area);

        final RealmResults<Gym> gymObjects = mRealm.where(Gym.class).findAll();
        final ArrayList<String> gymNames = new ArrayList<>();
        gymNames.add(""); // Append "blank" option
        for(Gym gym: gymObjects) {
            gymNames.add(gym.getName());
        }
        gymNames.add("Add new gym");
        gymSpinner.setCustomAdapter(new ArrayAdapter<>(getContext(),android.R.layout.simple_spinner_dropdown_item, gymNames));
        // set item checked
        if(mClimb.getGym() != null && gymNames.contains(mClimb.getGym().getName())) {
            gymSpinner.setSelection(gymNames.indexOf(mClimb.getGym().getName()));
        }else {
            gymSpinner.setSelection(0);
            mClimb.setGym(null);
        }
        gymSpinner.setOnItemChosenListener(new LabelledSpinner.OnItemChosenListener() {
            @Override
            public void onItemChosen(View labelledSpinner, AdapterView<?> adapterView, View itemView, int position, long id) {
                // if it's the last item, show edit text, otherwise hide edit text
                if(position == adapterView.getAdapter().getCount()-1) {
                    // TODO: show dialog for adding gym and create unmanaged object on callback
                    mClimb.setGym(null);
                }else {
                    if(position == 0) {
                        mClimb.setGym(null);
                    }else {
                        mClimb.setGym(gymObjects.get(position-1)); // position - 1 since first item is blank
                    }
                    updateAreaUI();
                }
            }

            @Override
            public void onNothingChosen(View labelledSpinner, AdapterView<?> adapterView) {

            }
        });
        updateAreaUI();

        // ----------- colors --------------------
        ImageButton colorImageButton = (ImageButton)v.findViewById(R.id.imagebutton_color);
        RealmResults<Climb> distinctColorClimbs = mRealm.where(Climb.class).distinct("color");

        final ArrayList<Integer> colors = new ArrayList<>();
        colors.add(0); // Append "blank" option
        for(Climb climb: distinctColorClimbs) {
            colors.add(climb.getColor());
        }
        colors.add(0); // add a color
        colorImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Pass a context, along with the title of the dialog
                new ColorChooserDialog.Builder(getContext(), R.string.colorchooser_title)
                        .accentMode(false)  // when true, will display accent palette instead of primary palette
                        .preselect(mClimb.getColor())  // optionally preselects a color
                        .dynamicButtonColor(true)  // defaults to true, false will disable changing action buttons' color to currently selected color
                        .show(getActivity()); // an AppCompatActivity which implements ColorCallback
            }
        });
        // set item checked
        if(mClimb.getColor() != -1) {
            colorImageButton.setBackgroundColor(mClimb.getColor());
        }else {
            colorImageButton.setBackgroundColor(Color.WHITE);
        }


        // TODO: Add notes to edit text
        // Add change listener after notes are entered

        /* ****************** SAVE BUTTON ***************************/
        mSaveButton.setEnabled(false);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // save the climb
                Realm realm = Realm.getDefaultInstance();
                try {
                    realm.beginTransaction();
                    Climb managedClimb = realm.copyToRealmOrUpdate(mClimb);
                    if(mMode == EditClimbMode.ADD_SEND)
                    {
                        Attempt send = mRealm.createObject(Attempt.class,UUID.randomUUID().toString());
                        send.setSend(true);
                        send.setDate(Calendar.getInstance().getTime());
                        send.setCount(1);
                        send.setProgress(100);
                        send.setClimb(managedClimb);
                    }
                    realm.commitTransaction();
                }finally{
                    realm.close();
                    dismiss();
                }
            }
        });
        checkClimbValidity();
        return v;
    }

    /**
     * Something changed that requires area GUI update.
     */
    private void updateAreaUI() {
        if (mClimb.getGym() != null) {
            // show area spinner
            mAreaSpinner.setEnabled(true);

            final RealmResults<Area> areaObjects = mRealm.where(Area.class).equalTo("gym.id", mClimb.getGym().getId()).findAll();
            final ArrayList<String> areaNames = new ArrayList<>();
            areaNames.add(""); // Append "blank" option
            for (Area area : areaObjects) {
                areaNames.add(area.getName());
            }
            areaNames.add("Add new area");
            mAreaSpinner.setCustomAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, areaNames));
            // set item checked
            if (mClimb.getArea() != null && areaNames.contains(mClimb.getArea().getName())) {
                mAreaSpinner.setSelection(areaNames.indexOf(mClimb.getArea().getName()));
            } else {
                mAreaSpinner.setSelection(0);
                mClimb.setArea(null);
            }
            mAreaSpinner.setOnItemChosenListener(new LabelledSpinner.OnItemChosenListener() {
                @Override
                public void onItemChosen(View labelledSpinner, AdapterView<?> adapterView, View itemView, int position, long id) {
                    // if it's the last item, show edit text, otherwise hide edit text
                    if (position == adapterView.getAdapter().getCount() - 1) {
                        // show edit text
                        // TODO: show dialog for adding area and create unmanaged object on callback
                        mClimb.setArea(null);
                    } else {
                        if (position == 0) {
                            mClimb.setArea(null);
                        } else {
                            mClimb.setArea(areaObjects.get(position-1)); // position - 1 since first item is blank
                        }
                    }
                }

                @Override
                public void onNothingChosen(View labelledSpinner, AdapterView<?> adapterView) {

                }
            });
        } else {
            mAreaSpinner.setEnabled(false);
        }
    }

    private void invalidateGradeList() {
        mGradeListView.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_activated_1, mClimb.getType().grades));
    }

    private void checkClimbValidity() {
        //mGoalSummary.setText(mGoal.getSummary());
        if(mClimb.isValidClimb()) {
            mSaveButton.setEnabled(true);
        }else {
            mSaveButton.setEnabled(false);
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRealm.close();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onClimbColorEvent(ClimbColorSelectedEvent event) {
        mClimb.setColor(event.color);
    }


}
