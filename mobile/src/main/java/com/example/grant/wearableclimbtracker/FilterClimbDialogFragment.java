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

import com.example.mysynclibrary.Shared;
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
    public static final String PREF_FILTER_PROJECTS = "pref_filter_projects";
    public static final String PREF_FILTER_SET = "pref_filter_set";
    private LabelledSpinner mSortSpinner;
    private Switch mProjectSwitch;
    private Switch mSetSwitch;




    enum SortByField{
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
    public static FilterClimbDialogFragment newInstance() {
        FilterClimbDialogFragment fragment = new FilterClimbDialogFragment();
        return fragment;
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_filter_climb_dialog, container, false);
        getDialog().setTitle("Sort and Filter");

        // get all previously saved preferences
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // sort behavior
        mSortSpinner = (LabelledSpinner) view.findViewById(R.id.spinner_sortby);
        mSortSpinner.setCustomAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, SortByField.getTitles()));
        mSortSpinner.setSelection(pref.getInt(PREF_SORT_BY, SortByField.lastadded.ordinal()));

        mProjectSwitch = (Switch) view.findViewById(R.id.switch_project);
        mProjectSwitch.setChecked(pref.getBoolean(PREF_FILTER_PROJECTS, false));
        mSetSwitch = (Switch)view.findViewById(R.id.switch_set);
        mSetSwitch.setChecked(pref.getBoolean(PREF_FILTER_SET, false));


        // save/cancel
        Button saveButton = (Button)view.findViewById(R.id.button_ok);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // save all the results and dismiss
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = pref.edit();
                editor.putInt(PREF_SORT_BY, (int) mSortSpinner.getSpinner().getSelectedItemId());

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
}
