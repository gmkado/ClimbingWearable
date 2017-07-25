package com.example.grant.wearableclimbtracker;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.realm.Attempt;
import com.example.mysynclibrary.realm.Climb;
import com.farbod.labelledspinner.LabelledSpinner;
import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SVBar;
import com.polyak.iconswitch.IconSwitch;

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
        TabHost host = (TabHost)v.findViewById(R.id.tab_host);
        host.setup();

        //grade tab
        TabHost.TabSpec spec = host.newTabSpec("GRADE");
        spec.setContent(R.id.tab_grade);
        spec.setIndicator("GRADE");
        host.addTab(spec);
        // set the listview to the climbtype grades
        mGradeListView = (ListView) v.findViewById(R.id.grade_listview);

        //details tab
        spec = host.newTabSpec("DETAILS");
        spec.setContent(R.id.tab_details);
        spec.setIndicator("DETAILS");
        host.addTab(spec);

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
        LabelledSpinner ls = (LabelledSpinner)v.findViewById(R.id.spinner_gym);
        final EditText editTextAddGym = (EditText)v.findViewById(R.id.editText_gym);
        RealmResults<Climb> results = mRealm.where(Climb.class).distinct("gym");
        final ArrayList<String> gyms = new ArrayList<>();
        gyms.add(""); // Append "blank" option
        for(Climb climb: results) {
            if(climb.getGym() != null) {
                gyms.add(climb.getGym());
            }
        }
        gyms.add("Add new gym");
        ls.setCustomAdapter(new ArrayAdapter<>(getContext(),android.R.layout.simple_spinner_dropdown_item, gyms));
        // set item checked
        if(mClimb.getGym() != null) {
            ls.setSelection(gyms.indexOf(mClimb.getGym()));
        }else {
            ls.setSelection(0);
        }
        ls.setOnItemChosenListener(new LabelledSpinner.OnItemChosenListener() {
            @Override
            public void onItemChosen(View labelledSpinner, AdapterView<?> adapterView, View itemView, int position, long id) {
                // if it's the last item, show edit text, otherwise hide edit text
                if(position == adapterView.getAdapter().getCount()-1) {
                    // show edit text
                    editTextAddGym.setVisibility(View.VISIBLE);
                    mClimb.setGym(null);
                }else {
                    editTextAddGym.setVisibility(View.GONE);
                    if(position == 0) {
                        mClimb.setGym(null);
                    }else {
                        mClimb.setGym(adapterView.getSelectedItem().toString());
                    }
                }
            }

            @Override
            public void onNothingChosen(View labelledSpinner, AdapterView<?> adapterView) {

            }
        });
        editTextAddGym.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mClimb.setGym(s.toString());
            }
        });

        // ----- Area ------------------
        ls = (LabelledSpinner)v.findViewById(R.id.spinner_area);
        final EditText editTextAddArea = (EditText)v.findViewById(R.id.editText_area);
        results = mRealm.where(Climb.class).distinct("area");
        final ArrayList<String> areas = new ArrayList<>();
        areas.add(""); // Append "blank" option
        for(Climb climb: results) {
            if(climb.getArea() != null) {
                areas.add(climb.getArea());
            }
        }
        areas.add("Add new area");
        ls.setCustomAdapter(new ArrayAdapter<>(getContext(),android.R.layout.simple_spinner_dropdown_item, areas));
        // set item checked
        if(mClimb.getArea() != null) {
            ls.setSelection(areas.indexOf(mClimb.getArea()));
        }else {
            ls.setSelection(0);
        }
        ls.setOnItemChosenListener(new LabelledSpinner.OnItemChosenListener() {
            @Override
            public void onItemChosen(View labelledSpinner, AdapterView<?> adapterView, View itemView, int position, long id) {
                // if it's the last item, show edit text, otherwise hide edit text
                if(position == adapterView.getAdapter().getCount()-1) {
                    // show edit text
                    editTextAddArea.setVisibility(View.VISIBLE);
                    mClimb.setArea(null);
                }else {
                    editTextAddArea.setVisibility(View.GONE);
                    if(position == 0) {
                        mClimb.setArea(null);
                    }else {
                        mClimb.setArea(adapterView.getSelectedItem().toString());
                    }
                }
            }

            @Override
            public void onNothingChosen(View labelledSpinner, AdapterView<?> adapterView) {

            }
        });
        editTextAddArea.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mClimb.setArea(s.toString());
            }
        });

        // ----------- colors --------------------
        ls = (LabelledSpinner)v.findViewById(R.id.spinner_color);
        final View addColorLayout = v.findViewById(R.id.layout_addcolor);
        final ColorPicker picker = (ColorPicker) v.findViewById(R.id.picker);
        SVBar svBar = (SVBar) v.findViewById(R.id.svbar);
        picker.addSVBar(svBar);
        picker.setShowOldCenterColor(false);

        results = mRealm.where(Climb.class).distinct("color");
        final ArrayList<Integer> colors = new ArrayList<>();
        colors.add(0); // Append "blank" option
        for(Climb climb: results) {
            colors.add(climb.getColor());
        }
        colors.add(0); // add a color
        ls.setCustomAdapter(new CustomColorSpinnerAdapter<>(getContext(), colors));
        // set item checked
        if(mClimb.getColor() != -1) {
            ls.setSelection(colors.indexOf(mClimb.getColor()));
        }else {
            ls.setSelection(0);
        }
        ls.setOnItemChosenListener(new LabelledSpinner.OnItemChosenListener() {
            @Override
            public void onItemChosen(View labelledSpinner, AdapterView<?> adapterView, View itemView, int position, long id) {
                // if it's the last item, show edit text, otherwise hide edit text
                if(position == adapterView.getAdapter().getCount()-1) {
                    // show edit color
                    addColorLayout.setVisibility(View.VISIBLE);
                    mClimb.setColor(picker.getColor());
                }else {
                    addColorLayout.setVisibility(View.GONE);
                    if(position == 0) {
                        mClimb.setColor(-1);
                    }else {
                        mClimb.setColor(colors.get(position));
                    }
                }
            }

            @Override
            public void onNothingChosen(View labelledSpinner, AdapterView<?> adapterView) {

            }
        });

        picker.setOnColorSelectedListener(new ColorPicker.OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color) {
                mClimb.setColor(color);
            }
        });


        // Add notes to edit text
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

    private class CustomColorSpinnerAdapter<T> extends BaseAdapter {
        private final Context mContext;
        private final LayoutInflater mInflater;
        private final int mResource;
        private final ArrayList<T> mColors;
        private final int mDropDownResource;

        public CustomColorSpinnerAdapter(Context context, ArrayList<T> colors) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
            mResource = mDropDownResource = android.R.layout.simple_spinner_dropdown_item; // Note that this is hardcoded so this will only work well for dropdown spinners
            mColors = colors;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return createViewFromResource(mInflater, position, convertView, parent);
        }

        @Override
        public int getCount() {
            return mColors.size();
        }

        @Override
        public T getItem(int position) {
            return mColors.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return createViewFromResource(mInflater, position, convertView, parent);
        }

        private View createViewFromResource(LayoutInflater mInflater, int position, View convertView, ViewGroup parent) {
            final View view;
            final TextView text;

            if (convertView == null) {
                view = mInflater.inflate(mResource, parent, false);
            } else {
                view = convertView;
            }

            text = (TextView) view;

            int item = (Integer)getItem(position);
            text.setBackgroundColor(item);

            if(position == 0) {
                text.setText("No Color");
            }else if(position == mColors.size()-1) {
                text.setText("Add a color");
            }else {
                text.setText("");
            }
            return view;
        }


    }
}
