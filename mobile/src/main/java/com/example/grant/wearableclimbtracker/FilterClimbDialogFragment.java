package com.example.grant.wearableclimbtracker;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.example.mysynclibrary.realm.Climb;
import com.farbod.labelledspinner.LabelledSpinner;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FilterClimbDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FilterClimbDialogFragment extends DialogFragment {
    public static final String PREF_SORT_BY = "pref_sortby";
    public static final String PREF_FILTER_GYM = "pref_filter_gym";
    public static final String PREF_FILTER_GYM_VAL = "pref_filter_gym_val";
    public static final String PREF_FILTER_AREA = "pref_filter_area";
    public static final String PREF_FILTER_AREA_VAL = "pref_filter_area_val";
    public static final String PREF_FILTER_PROJECTS = "pref_filter_projects";
    public static final String PREF_FILTER_SET = "pref_filter_set";
    private SortByField mSortByField;
    private OnDialogClosedListener mListener;
    private boolean mFilterGym;
    private String mFilterGymVal;
    private boolean mFilterArea;
    private String mFilterAreaVal;
    private boolean mFilterProjects;
    private boolean mFilterSet;
    private LabelledSpinner mSortSpinner;
    private LabelledSpinner mGymSpinner;
    private Switch mGymSwitch;
    private Switch mAreaSwitch;
    private LabelledSpinner mAreaSpinner;
    private Switch mProjectSwitch;
    private Switch mSetSwitch;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRealm.close();
    }

    private Realm mRealm;


    public enum SortByField{
        lastadded("Last Added"),
        lastclimbed("Last Climbed"),
        mostclimbed("Most Climbed"),
        progress("Progress"),
        grade_asc("Grade (Easy First)"),
        grade_desc("Grade (Hard First)");

        public String title;

        SortByField(String title) {
            this.title = title;
        }

        public static ArrayList<String> getTitles() {
            ArrayList<String> list = new ArrayList<>();
            for(SortByField s: SortByField.values()) {
                list.add(s.title);
            }
            return list;
        }
    }
    public interface OnDialogClosedListener {
        void onClose();
    }

    public void setOnDialogClosedListener(OnDialogClosedListener listener){
        mListener = listener;
    }

    public FilterClimbDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment FilterClimbDialogFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static FilterClimbDialogFragment newInstance() {
        FilterClimbDialogFragment fragment = new FilterClimbDialogFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get all previously saved preferences
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mSortByField = mSortByField.values()[pref.getInt(PREF_SORT_BY, SortByField.lastadded.ordinal())];
        mFilterGym = pref.getBoolean(PREF_FILTER_GYM, false);
        mFilterGymVal = pref.getString(PREF_FILTER_GYM_VAL, null);
        mFilterArea = pref.getBoolean(PREF_FILTER_AREA, false);
        mFilterAreaVal = pref.getString(PREF_FILTER_AREA_VAL, null);
        mFilterProjects = pref.getBoolean(PREF_FILTER_PROJECTS, false);
        mFilterSet = pref.getBoolean(PREF_FILTER_SET, false);

        mRealm = Realm.getDefaultInstance();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_filter_climb_dialog, container, false);
        getDialog().setTitle("Sort and Filter");

        // sort behavior
        mSortSpinner = (LabelledSpinner) view.findViewById(R.id.spinner_sortby);
        mSortSpinner.setCustomAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, SortByField.getTitles()));
        mSortSpinner.setSelection(mSortByField.ordinal());

        // filter -- gym
        mGymSwitch = (Switch) view.findViewById(R.id.switch_gym);
        mGymSwitch.setChecked(mFilterGym);
        mGymSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateGymUI();
            }
        });

        mGymSpinner = (LabelledSpinner) view.findViewById(R.id.spinner_gyms);
        mGymSpinner.setOnItemChosenListener(new LabelledSpinner.OnItemChosenListener() {
            @Override
            public void onItemChosen(View labelledSpinner, AdapterView<?> adapterView, View itemView, int position, long id) {
                populateAreaSpinner(); // only show areas in selected gym
            }

            @Override
            public void onNothingChosen(View labelledSpinner, AdapterView<?> adapterView) {

            }
        });
        RealmResults<Climb> results = mRealm.where(Climb.class).distinct("gym");
        final ArrayList<String> gyms = new ArrayList<>();
        for(Climb climb: results) {
            if(climb.getGym() != null) {
                gyms.add(climb.getGym());
            }
        }
        // if the gym list is empty, disable the filter switch
        if(gyms.isEmpty()) {
            mGymSwitch.setEnabled(false);
            mGymSwitch.setChecked(false);
        }else {
            mGymSpinner.setCustomAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, gyms));
            if(gyms.contains(mFilterGymVal)) {
                mGymSpinner.setSelection(gyms.indexOf(mFilterGymVal));
            }
        }
        updateGymUI();

        // filter -- area
        mAreaSwitch = (Switch) view.findViewById(R.id.switch_area);
        mAreaSwitch.setChecked(mFilterArea);
        mAreaSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateAreaUI();
            }
        });

        mAreaSpinner = (LabelledSpinner) view.findViewById(R.id.spinner_areas);
        populateAreaSpinner();
        updateAreaUI();

        // Other filters
        mProjectSwitch = (Switch) view.findViewById(R.id.switch_project);
        mProjectSwitch.setChecked(mFilterProjects);
        mSetSwitch = (Switch)view.findViewById(R.id.switch_set);
        mSetSwitch.setChecked(mFilterSet);


        // save/cancel
        Button saveButton = (Button)view.findViewById(R.id.button_ok);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // save all the results and dismiss
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = pref.edit();
                editor.putInt(PREF_SORT_BY, (int) mSortSpinner.getSpinner().getSelectedItemId());
                editor.putBoolean(PREF_FILTER_GYM, mGymSwitch.isChecked());
                if(mGymSpinner.getSpinner().getSelectedItem()!=null) {
                    editor.putString(PREF_FILTER_GYM_VAL, mGymSpinner.getSpinner().getSelectedItem().toString());
                }
                editor.putBoolean(PREF_FILTER_AREA, mAreaSwitch.isChecked());
                if(mAreaSpinner.getSpinner().getSelectedItem()!=null) {
                    editor.putString(PREF_FILTER_AREA_VAL, mAreaSpinner.getSpinner().getSelectedItem().toString());
                }
                editor.putBoolean(PREF_FILTER_PROJECTS, mProjectSwitch.isChecked());
                editor.putBoolean(PREF_FILTER_SET, mSetSwitch.isChecked());
                editor.apply();
                // save the current fields
                if(mListener!=null) {
                    mListener.onClose();
                }
                dismiss();
            }
        });

        Button cancelButton = (Button) view.findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        return view;
    }

    private void updateAreaUI() {
        mAreaSpinner.setVisibility(mAreaSwitch.isChecked()?View.VISIBLE:View.GONE);
    }

    private void populateAreaSpinner() {
        RealmResults<Climb> results;
        if(mGymSwitch.isChecked() && mGymSpinner.getSpinner().getSelectedItem()!=null) {
            results = mRealm.where(Climb.class).equalTo("gym", mGymSpinner.getSpinner().getSelectedItem().toString()).distinct("area");
        }else {
            results = mRealm.where(Climb.class).distinct("area");
        }
        final ArrayList<String> areas = new ArrayList<>();
        for(Climb climb: results) {
            if(climb.getArea() != null) {
                areas.add(climb.getArea());
            }
        }
        // if the Area list is empty, disable the filter switch
        if(areas.isEmpty()) {
            mAreaSwitch.setEnabled(false);
            mAreaSwitch.setChecked(false);
        }else {
            mAreaSpinner.setCustomAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, areas));
            if(areas.contains(mFilterAreaVal)) {
                mAreaSpinner.setSelection(areas.indexOf(mFilterAreaVal));
            }
        }
    }

    private void updateGymUI() {
        mGymSpinner.setVisibility(mGymSwitch.isChecked()?View.VISIBLE:View.GONE);
    }


}
