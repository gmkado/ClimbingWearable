package com.example.grant.wearableclimbtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
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

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.eventbus.ClimbSortFilterEvent;
import com.example.mysynclibrary.eventbus.LocationFilterEvent;
import com.example.mysynclibrary.realm.Area;
import com.example.mysynclibrary.realm.Gym;
import com.farbod.labelledspinner.LabelledSpinner;
import com.polyak.iconswitch.IconSwitch;

import org.greenrobot.eventbus.EventBus;
import java.util.ArrayList;
import io.realm.Realm;
import io.realm.RealmResults;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FilterLocationDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FilterLocationDialogFragment extends DialogFragment {
    public static final String PREF_FILTER_GYM_ID = "pref_filter_gym_val";
    public static final String PREF_FILTER_AREA_ID = "pref_filter_area_val";
    public static final String PREF_FILTER_CLIMBTYPE = "pref_filter_climbtype";
    private LabelledSpinner mGymSpinner;
    private LabelledSpinner mAreaSpinner;
    private String mGymId;
    private String mAreaId;
    private Shared.ClimbType mClimbType;
    private RealmResults<Gym> mGymObjects;
    private RealmResults<Area> mAreaObjects;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRealm.close();
    }

    private Realm mRealm;

    public FilterLocationDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment FilterClimbDialogFragment.
     */
    public static FilterLocationDialogFragment newInstance() {
        FilterLocationDialogFragment fragment = new FilterLocationDialogFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRealm = Realm.getDefaultInstance();

        // get all previously saved preferences
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mGymId = pref.getString(PREF_FILTER_GYM_ID, null);
        mAreaId = pref.getString(PREF_FILTER_AREA_ID, null);
        mClimbType = Shared.ClimbType.values()[pref.getInt(PREF_FILTER_CLIMBTYPE, Shared.ClimbType.bouldering.ordinal())];
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_filter_location_dialog, container, false);
        getDialog().setTitle("Choose your location");

        mGymSpinner = (LabelledSpinner) view.findViewById(R.id.spinner_gyms);
        mGymSpinner.setOnItemChosenListener(new LabelledSpinner.OnItemChosenListener() {
            @Override
            public void onItemChosen(View labelledSpinner, AdapterView<?> adapterView, View itemView, int position, long id) {
                if(position == 0 ) {
                    mGymId = null;
                }else {
                    mGymId = mGymObjects.get(position - 1).getId();
                }
                updateAreaUI(); // only show areas in selected gym
            }

            @Override
            public void onNothingChosen(View labelledSpinner, AdapterView<?> adapterView) {

            }
        });
        mGymObjects = mRealm.where(Gym.class).findAll();

        final ArrayList<String> gymNames = new ArrayList<>();
        int filterGymIndex = 0;
        gymNames.add("None selected"); // Add unselected option
        for(Gym gym: mGymObjects) {
            gymNames.add(gym.getName());
            if(gym.getId().equals(mGymId)) {
                filterGymIndex = gymNames.size()-1;
            }
        }
        // if the gym list is empty, disable the filter switch
        if(mGymObjects.isEmpty()) {
            // TODO: show a message where to add gyms
        }
        mGymSpinner.setCustomAdapter(new ArrayAdapter<String>(getContext(),android.R.layout.simple_spinner_dropdown_item, gymNames));
        mGymSpinner.setSelection(filterGymIndex);

        // filter -- area
        mAreaSpinner = (LabelledSpinner) view.findViewById(R.id.spinner_areas);
        updateAreaUI();

        // filter -- climbtype
        IconSwitch climbTypeSwitch = (IconSwitch) view.findViewById(R.id.switch_climbtype);
        climbTypeSwitch.setChecked(mClimbType == Shared.ClimbType.bouldering? IconSwitch.Checked.LEFT: IconSwitch.Checked.RIGHT);
        climbTypeSwitch.setCheckedChangeListener(new IconSwitch.CheckedChangeListener() {
            @Override
            public void onCheckChanged(IconSwitch.Checked current) {
                mClimbType = current== IconSwitch.Checked.LEFT? Shared.ClimbType.bouldering: Shared.ClimbType.ropes;
                // need to update the areas to reflect the correct climbtype
                updateAreaUI();
            }
        });
        // save/cancel
        Button saveButton = (Button)view.findViewById(R.id.button_ok);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // save all the results and dismiss
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = pref.edit();
                editor.putString(PREF_FILTER_GYM_ID, mGymId);
                editor.putString(PREF_FILTER_AREA_ID, mAreaId);
                editor.putInt(PREF_FILTER_CLIMBTYPE, mClimbType.ordinal());
                editor.apply();
                // post event to notify climblist fragment
                EventBus.getDefault().post(new LocationFilterEvent());
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
        final ArrayList<String> areaNames = new ArrayList<>();
        areaNames.add("None selected");
        int filterAreaIndex = 0;

        if(mGymSpinner.getSpinner().getSelectedItemPosition()==0) {
            mAreaSpinner.setEnabled(false);
        }else {
            mAreaSpinner.setEnabled(true);
            String gymName = (String) mGymSpinner.getSpinner().getSelectedItem();
            mAreaObjects = mRealm.where(Area.class)
                    .equalTo("gym.name", gymName)
                    .in("type", new Integer[] {Area.AreaType.MIXED.ordinal(), mClimbType == Shared.ClimbType.bouldering?
                            Area.AreaType.BOULDER_ONLY.ordinal():
                            Area.AreaType.ROPES_ONLY.ordinal()})
                    .findAll();
            for (Area area : mAreaObjects) {
                areaNames.add(area.getName() + " (" + area.getType().getTitle()+")");
                if(area.getId().equals(mAreaId)) {
                    filterAreaIndex = areaNames.size()-1;
                }
            }
            // if the Area list is empty, disable the filter switch
            if (mAreaObjects.isEmpty()) {
                // TODO: alert where to add areas
            }

        }
        mAreaSpinner.setCustomAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, areaNames));
        mAreaSpinner.setOnItemChosenListener(new LabelledSpinner.OnItemChosenListener() {
            @Override
            public void onItemChosen(View labelledSpinner, AdapterView<?> adapterView, View itemView, int position, long id) {
                if(position==0) {
                    mAreaId = null;
                }else{
                    // set area id
                    mAreaId = mAreaObjects.get(position-1).getId();
                }
            }

            @Override
            public void onNothingChosen(View labelledSpinner, AdapterView<?> adapterView) {

            }
        });
        mAreaSpinner.setSelection(filterAreaIndex);
    }

}
