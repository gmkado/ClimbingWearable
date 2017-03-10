package com.example.grant.wearableclimbtracker;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
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
import com.example.mysynclibrary.realm.Climb;

import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.Instant;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import io.realm.Realm;


public class EditClimbDialogFragment extends DialogFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_CLIMBTYPE = "climbtype";
    private static final String ARG_CLIMBUUID = "climbUUID";

    // TODO: Rename and change types of parameters
    private Shared.ClimbType mClimbType;
    private String mClimbUUID;
    private Realm mRealm;

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
    // TODO: Rename and change types and number of parameters
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
        final ListView lv = (ListView) v.findViewById(R.id.grade_listview);
        lv.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, mClimbType.grades));

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

        final DatePicker dp = (DatePicker) v.findViewById(R.id.datePicker);
        final TimePicker tp = (TimePicker) v.findViewById(R.id.timePicker);
        Button deleteButton = (Button) v.findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Realm realm = Realm.getDefaultInstance();
                try{
                    Climb climb = getClimbFromThread(realm);
                    // if this climb is on the wearable too, mark for deletion, otherwise delete climb
                    if(climb.isOnwear()){
                        realm.executeTransactionAsync(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                Climb climb = getClimbFromThread(realm);
                                climb.setDelete(true);
                            }
                        }, new Realm.Transaction.OnSuccess(){

                            @Override
                            public void onSuccess() {
                                dismiss();
                            }
                        });
                    }else {
                        realm.executeTransactionAsync(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                Climb climb = getClimbFromThread(realm);
                                climb.deleteFromRealm();
                            }
                        }, new Realm.Transaction.OnSuccess() {
                            @Override
                            public void onSuccess() {
                                dismiss();
                            }
                        });
                    }
                }finally {
                    realm.close();
                }
            }
        });

        // try to get the climb
        final Climb climb = getClimbFromThread(mRealm);
        if(climb != null) {
            // grab values from climb and update GUI

            // annoying hack to get selection to update http://stackoverflow.com/questions/7018921/setselection-not-changing-listview-position
            // TODO: doesn't stay selected, might need radio button hack: http://stackoverflow.com/questions/3111354/android-listview-stay-selected
            lv.clearFocus();
            lv.post(new Runnable() {
                @Override
                public void run() {
                    lv.requestFocusFromTouch();
                    lv.setSelection(climb.getGrade());
                    lv.setItemChecked(climb.getGrade(), true);
                    lv.requestFocus();
                }
            });

            Calendar cal = Calendar.getInstance();
            cal.setTime(climb.getDate());

            dp.updateDate(cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH));

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

        final Button savebutton = (Button)v.findViewById(R.id.save_button);
        if(lv.isSelected()) {
            // enable save button
            savebutton.setEnabled(true);
        }else {
            // disable save button
            savebutton.setEnabled(false);
        }

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                savebutton.setEnabled(true);
            }
        });

        savebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // save the climb
                Realm realm = Realm.getDefaultInstance();
                try {
                    realm.executeTransactionAsync(new Realm.Transaction() {

                        @Override
                        public void execute(Realm realm) {
                            // set climb fields
                            Climb climb = getClimbFromThread(realm);
                            if (climb == null) {
                                // this is a new climb, so we need to create it
                                climb = realm.createObject(Climb.class,  UUID.randomUUID().toString());
                                climb.setType(mClimbType.ordinal());
                                climb.setOnwear(false);
                            }

                            // get the date/time from pickers
                            Calendar cal = Calendar.getInstance();
                            // this will reflect the current time, so use it to set "lastedit"
                            climb.setLastedit(cal.getTime());

                            cal.set(Calendar.YEAR, dp.getYear());
                            cal.set(Calendar.MONTH, dp.getMonth());
                            cal.set(Calendar.DAY_OF_MONTH, dp.getDayOfMonth());
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                cal.set(Calendar.HOUR_OF_DAY, tp.getHour());
                                cal.set(Calendar.MINUTE, tp.getMinute());
                            } else {
                                cal.set(Calendar.HOUR_OF_DAY, tp.getCurrentHour());
                                cal.set(Calendar.MINUTE, tp.getCurrentMinute());
                            }
                            Date date = cal.getTime();

                            // se fields based on logic tree in onenote->notes->2/23/2017
                            if (climb.isOnwear()) {
                                // if it is on the wearable, no matter what the date
                                climb.setDirty(true);
                            } else {
                                // if this climb was done today, mark as dirty
                                Date startOfDay = Shared.getStartofDate(null);
                                if (date.after(startOfDay)) {
                                    climb.setDirty(true);
                                } else {
                                    climb.setDirty(false);
                                }
                            }

                            climb.setDelete(false);
                            climb.setDate(date);
                            climb.setGrade(lv.getCheckedItemPosition());
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
        return v;
    }

    private Climb getClimbFromThread(Realm realm) {
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



}
