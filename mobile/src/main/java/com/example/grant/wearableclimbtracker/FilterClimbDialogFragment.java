package com.example.grant.wearableclimbtracker;

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

import com.example.mysynclibrary.eventbus.ClimbSortFilterEvent;
import com.example.mysynclibrary.realm.Area;
import com.example.mysynclibrary.realm.Gym;
import com.farbod.labelledspinner.LabelledSpinner;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmResults;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FilterClimbDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FilterClimbDialogFragment extends DialogFragment {
    public static final String PREF_SORT_BY = "pref_sortby";
    public static final String PREF_FILTER_GYM = "pref_filter_gym";
    public static final String PREF_FILTER_GYM_NAME = "pref_filter_gym_val";
    public static final String PREF_FILTER_AREA = "pref_filter_area";
    public static final String PREF_FILTER_AREA_NAME = "pref_filter_area_val";
    public static final String PREF_FILTER_PROJECTS = "pref_filter_projects";
    public static final String PREF_FILTER_SET = "pref_filter_set";
    private SortByField mSortByField;
    private boolean mFilterGym;
    private String mFilterGymId;
    private boolean mFilterArea;
    private String mFilterAreaId;
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
        mFilterGymId = pref.getString(PREF_FILTER_GYM_NAME, null);
        mFilterArea = pref.getBoolean(PREF_FILTER_AREA, false);
        mFilterAreaId = pref.getString(PREF_FILTER_AREA_NAME, null);
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
        RealmResults<Gym> gymObjects = mRealm.where(Gym.class).findAll();
        final ArrayList<String> gymNames = new ArrayList<>();
        for(Gym gym: gymObjects) {
            gymNames.add(gym.getName());
        }
        // if the gym list is empty, disable the filter switch
        if(gymObjects.isEmpty()) {
            mGymSwitch.setEnabled(false);
            mGymSwitch.setChecked(false);
        }else {
            mGymSpinner.setCustomAdapter(new ArrayAdapter<>(getContext(),android.R.layout.simple_spinner_dropdown_item, gymNames));
            int filterGymIndex = gymObjects.indexOf(gymObjects.where().equalTo("id", mFilterGymId).findFirst());
            if(filterGymIndex !=-1) {
                mGymSpinner.setSelection(filterGymIndex);
            }else {
                // unselect
                mGymSpinner.setSelected(false);
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
                if(mGymSwitch.isChecked() && mGymSpinner.getSpinner().getSelectedItem()!=null) {
                    editor.putString(PREF_FILTER_GYM_NAME, mGymSpinner.getSpinner().getSelectedItem().toString());
                }

                editor.putBoolean(PREF_FILTER_AREA, mAreaSwitch.isChecked());
                if(mAreaSwitch.isChecked() && mAreaSpinner.getSpinner().getSelectedItem()!=null) {
                    editor.putString(PREF_FILTER_AREA_NAME, mAreaSpinner.getSpinner().getSelectedItem().toString());
                }

                editor.putBoolean(PREF_FILTER_PROJECTS, mProjectSwitch.isChecked());
                editor.putBoolean(PREF_FILTER_SET, mSetSwitch.isChecked());
                editor.apply();

                // post event to notify climblist fragment
                EventBus.getDefault().post(new ClimbSortFilterEvent());
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
        if(!mGymSwitch.isChecked() || mGymSpinner.getSpinner().getSelectedItem()==null) {
            // hide all area UI elements
            mAreaSwitch.setVisibility(View.GONE);
            mAreaSwitch.setChecked(false);
            mAreaSpinner.setVisibility(View.GONE);
            return;
        }
        String gymName = (String) mGymSpinner.getSpinner().getSelectedItem();
        RealmResults<Area> areaObjects = mRealm.where(Area.class).equalTo("gym.name",gymName).findAll();
        final ArrayList<String> areaNames = new ArrayList<>();
        for (Area area : areaObjects) {
            areaNames.add(area.getName());
        }
        // if the Area list is empty, disable the filter switch
        if(areaObjects.isEmpty()) {
            mAreaSwitch.setEnabled(false);
            mAreaSwitch.setChecked(false);
        }else {
            mAreaSpinner.setCustomAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, areaNames));
            int filterAreaIndex = areaObjects.indexOf(areaObjects.where().equalTo("id", mFilterAreaId).findFirst());
            if(filterAreaIndex !=-1) {
                mAreaSpinner.setSelection(filterAreaIndex);
            }else {
                // unselect
                mAreaSpinner.setSelected(false);
            }
        }
    }

    private void updateGymUI() {
        mGymSpinner.setVisibility(mGymSwitch.isChecked()?View.VISIBLE:View.GONE);
    }


}
