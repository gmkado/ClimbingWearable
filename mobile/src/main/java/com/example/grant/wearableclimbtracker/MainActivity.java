package com.example.grant.wearableclimbtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.eventbus.WearMessageEvent;
import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.eventbus.RealmResultsEvent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tasks.RuntimeExecutionException;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.honorato.multistatetogglebutton.MultiStateToggleButton;
import org.honorato.multistatetogglebutton.ToggleButton;
import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.Instant;
import org.threeten.bp.temporal.ChronoUnit;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.internal.IOException;

public class MainActivity extends AppCompatActivity{

    private static final String TAG = "MainActivity";
    private static final String REALM_CONTENT_CREATOR_CAPABILITY = "realm_content_creator";//Note: capability name defined in wear module values/wear.xml
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String PREF_LASTSYNC = "prefLastSync";
    private static final String PREF_TYPE = "prefType";
    private static final String PREF_DATERANGE = "prefDateRange";
    private GoogleApiClient mGoogleApiClient;
    private String mNodeId;
    private Realm mRealm;
    private ChartPagerAdapter mChartPagerAdapter;
    private Switch typeToggle;
    private Shared.ClimbType mClimbType;
    private ChronoUnit mDateRange;
    private int mDateOffset;

    private List<String> mDateRangeLabels = Arrays.asList("DAY", "WEEK", "MONTH", "YEAR", "ALL");
    private List<ChronoUnit> mDateRanges = Arrays.asList(ChronoUnit.DAYS, ChronoUnit.WEEKS, ChronoUnit.MONTHS, ChronoUnit.YEARS, ChronoUnit.FOREVER);
    private TextView mDateTextView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        // check last climbtype and set current view
        typeToggle = (Switch)menu.findItem(R.id.type_toggle).getActionView().findViewById(R.id.switch1);
        typeToggle.setChecked(mClimbType == Shared.ClimbType.bouldering);

        typeToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mClimbType = isChecked? Shared.ClimbType.bouldering: Shared.ClimbType.ropes;
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                // save this in shared pref.edit();
                editor.putInt(PREF_TYPE, mClimbType.ordinal());
                editor.commit();

                invalidateRealmResult();
            }
        });
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Log.d(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
            case R.id.sync_db:
                final String tagSub = "Step 1: ";

                // start sync process
                // Query [dirty = true], [onwear=false], [date=not today] and set [dirty = false] (day changed before sync)

                mRealm.executeTransactionAsync(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        Date startOfDay = Shared.getStartofDate(null);
                        RealmResults<Climb> removeDirtyResults = realm.where(Climb.class).lessThan("date", startOfDay).equalTo("dirty", true).findAll();

                        for(Climb climb:removeDirtyResults) {
                            climb.setDirty(false);
                        }
                        // Query [dirty = true] or [delete = true] and send to wear
                        List<Climb> allClimbs = realm.where(Climb.class).equalTo("dirty", true).or().equalTo("delete", true).findAll();


                        // copy the results into a string format
                        Gson gson = Shared.getGson();
                        String json = gson.toJson(realm.copyFromRealm(allClimbs));

                        Log.d(TAG, tagSub+"sending json of length:" + json.length());
                        sendMessageToRealmCreator(Shared.REALM_SYNC_PATH, json.getBytes(StandardCharsets.UTF_8));
                    }
                });


                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mGoogleApiClient!=null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Log.d(TAG, "onCreate");

        mRealm = Realm.getDefaultInstance();

        // setup viewpager and adapter
        mChartPagerAdapter = new ChartPagerAdapter(this,getSupportFragmentManager());
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(mChartPagerAdapter);
        pager.setCurrentItem(1); // start in overview fragment

        // get the shared preferences for type and date range
        SharedPreferences pref = getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE);
        mClimbType = Shared.ClimbType.values()[pref.getInt(PREF_TYPE, Shared.ClimbType.bouldering.ordinal())];
        mDateRange = ChronoUnit.values()[pref.getInt(PREF_DATERANGE, ChronoUnit.FOREVER.ordinal())];

        //  set title to last update date
        getSupportActionBar().setSubtitle("Last sync: " + pref.getString(PREF_LASTSYNC, "never").replace("\"", ""));

        // setup date toggle button
        MultiStateToggleButton button = (MultiStateToggleButton)findViewById(R.id.mstb_daterange);
        button.setElements(mDateRangeLabels);
        button.setValue(mDateRanges.indexOf(mDateRange));

        button.setOnValueChangedListener(new ToggleButton.OnValueChangedListener(){

            @Override
            public void onValueChanged(int position) {
                // change the date range
                mDateRange = mDateRanges.get(position);

                // reset the offset
                mDateOffset = 0;

                // save this in shared pref
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putInt(PREF_DATERANGE, mDateRange.ordinal());
                editor.commit();
                invalidateRealmResult();
            }
        });

        // setup google api
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);

                        // Now request a sync
                        Log.d(TAG, "requesting sync");
                        sendMessageToRealmCreator(Shared.REALM_SYNC_PATH, null);
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                // Request access only to the Wearable API
                .addApi(Wearable.API)
                .build();


        // set up date offset
        mDateOffset = 0;
        (findViewById(R.id.prev_imagebutton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDateOffset--;
                invalidateRealmResult();
            }
        });
        (findViewById(R.id.next_imagebutton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDateOffset<0) {
                    mDateOffset++;
                }
                invalidateRealmResult();
            }
        });
        mDateTextView = (TextView)findViewById(R.id.currentdate_textview);

        invalidateRealmResult();

    }

    private void sendMessageToRealmCreator(final String path, final byte[] data) {
        //Log.d(TAG,"sendMessageToRealmCreator");
        if(mNodeId == null) {
                // need to find a capable node
                PendingResult result = Wearable.CapabilityApi.getCapability(
                        mGoogleApiClient, REALM_CONTENT_CREATOR_CAPABILITY,
                        CapabilityApi.FILTER_REACHABLE);
                result.setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
                @Override
                public void onResult(@NonNull CapabilityApi.GetCapabilityResult result) {
                    Set<Node> connectedNodes = result.getCapability().getNodes();

                    // for now only anticipate single node with this capability
                    // see message api docs if this changes
                    if (connectedNodes.size() > 1) {
                        Log.e(TAG, "More than one capable node connected.  This shouldn't happen");
                    } else if(connectedNodes.isEmpty()) {
                        Toast.makeText(MainActivity.this,"No capable nodes found", Toast.LENGTH_LONG).show();
                    } else{
                        Log.d(TAG, "setting node id");
                        mNodeId = connectedNodes.iterator().next().getId();
                        sendMessage(path, data);
                    }
                }
            });
        }
        else{
            sendMessage(path, data);
        }
    }

    private void sendMessage(String path, byte[] data) {
        Wearable.MessageApi.sendMessage(mGoogleApiClient, mNodeId,
                path, data).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                if(sendMessageResult.getStatus().isSuccess()) {
                    Log.d(TAG, "Message sent");
                }else{
                    // failed message
                    Log.e(TAG, "Message failed");
                }

            }
        });
    }

    @Subscribe (sticky = true, threadMode = ThreadMode.MAIN)
    public void onWearMessageReceived(WearMessageEvent event) {
        switch(event.messageEvent.getPath()) {
            case Shared.REALM_SYNC_PATH:
                final String tagSub = "Step 3: ";
                // received the sync data
                final SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String data = new String(event.messageEvent.getData(), StandardCharsets.UTF_8);

                Log.d(TAG, tagSub + "got message = " + data);

                //get the start of DAY of last data sync
                final Gson gson = Shared.getGson();
                final Climb[] climbList = gson.fromJson(data, Climb[].class);

                // convert data to realm and add
                mRealm.executeTransactionAsync(new Realm.Transaction() {

                    @Override
                    public void execute(Realm realm) {
                        for (Climb wearClimb : climbList) {
                            // get the climb on the wearable
                            Climb mobileClimb = realm.where(Climb.class).equalTo("id", wearClimb.getId()).findFirst();
                            if (wearClimb.isDelete()) {
                                if (mobileClimb != null) {
                                    // delete the climb from mobile
                                    mobileClimb.deleteFromRealm();
                                }
                            } else {
                                // add or update climb
                                realm.copyToRealmOrUpdate(wearClimb);
                            }
                        }
                        // query all marked for deletion and delete
                        RealmResults<Climb> deleteClimbs = realm.where(Climb.class).equalTo("delete", true).findAll();
                        deleteClimbs.deleteAllFromRealm();

                        //Query [dirty=true] and set [dirty=false]
                        RealmResults<Climb> dirtyResults = realm.where(Climb.class).equalTo("dirty", true).findAll();
                        for (Climb climb : dirtyResults) {
                            climb.setDirty(false);
                        }
                        //Query [date = today] and [onwear = false] and set [onwear = true]
                        RealmResults<Climb> nowOnWear = realm.where(Climb.class).equalTo("onwear", false).greaterThanOrEqualTo("date", Shared.getStartofDate(null)).findAll();
                        for (Climb climb : nowOnWear) {
                            climb.setOnwear(true);
                        }
                        //Query [date = not today] and [onwear = true] and set [onwear = false]
                        RealmResults<Climb> nowOffWear = realm.where(Climb.class).equalTo("onwear", true).lessThan("date", Shared.getStartofDate(null)).findAll();
                        for (Climb climb : nowOffWear) {
                            climb.setOnwear(false);
                        }

                        //set today as the new last sync date
                        SharedPreferences.Editor editor = settings.edit();
                        final String newSyncDate = gson.toJson(Calendar.getInstance().getTime()); // TODO: can this be stored as calendar instead of date?
                        editor.putString(PREF_LASTSYNC, newSyncDate); // TODO: change to more condensed format
                        editor.commit();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getSupportActionBar().setSubtitle("Last sync: " + newSyncDate.replace("\"", ""));
                            }
                        });

                        // send back acknowledge message
                        Log.d(TAG, tagSub + "sending sync ack");
                        sendMessageToRealmCreator(Shared.REALM_ACK_PATH, null);
                    }
                }, new Realm.Transaction.OnError() {
                    @Override
                    public void onError(Throwable error) {
                        Log.e(TAG, tagSub + "Failed merging wear data with mobile: " + error.getMessage());
                    }
                });
                break;
            default:
                Log.e(TAG, "onWearMessageReceived: message path not recognized");
                break;
        }

    }

    public void invalidateRealmResult() {
        //Log.d(TAG, "setClimbRealmResult");
        RealmQuery<Climb> realmQuery =  mRealm.where(Climb.class)
                .equalTo("delete", false)
                .equalTo("type", mClimbType.ordinal());

        if(mDateRange != ChronoUnit.FOREVER) {
            Pair<Date, Date> datePair = Shared.getDatesFromRange(mDateRange, mDateOffset);
            Date startDate = datePair.first;
            Date endDate = datePair.second;
            // add the filter
            realmQuery.between("date",startDate,endDate);

            // update textview to show what date range we're looking at
            SimpleDateFormat sdf;
            // special text for if we're showing the current day/week/month
            if(mDateOffset == 0) {
                switch(mDateRange) {
                    case DAYS:
                        mDateTextView.setText("TODAY");
                        break;
                    case WEEKS:
                        mDateTextView.setText("THIS WEEK");
                        break;
                    case MONTHS:
                        mDateTextView.setText("THIS MONTH");
                        break;
                    case YEARS:
                        sdf = new SimpleDateFormat("yyyy");
                        mDateTextView.setText(sdf.format(startDate));
                        break;

                }
            }else {
                // show the date range in the right format
                switch (mDateRange) {
                    case DAYS:
                        sdf = new SimpleDateFormat("MM/dd/yyyy");
                        mDateTextView.setText(sdf.format(startDate));
                        break;
                    case WEEKS:
                        sdf = new SimpleDateFormat("MM/dd/yyyy");
                        mDateTextView.setText(String.format("%s to %s", sdf.format(startDate), sdf.format(endDate)));
                        break;
                    case MONTHS:
                        sdf = new SimpleDateFormat("MMM yyyy");
                        mDateTextView.setText(sdf.format(startDate));
                        break;
                    case YEARS:
                        sdf = new SimpleDateFormat("yyyy");
                        mDateTextView.setText(sdf.format(startDate));
                        break;
                }
            }

        }else {
            mDateTextView.setText("Showing all climbs");
        }
        EventBus.getDefault().postSticky(new RealmResultsEvent(realmQuery.findAllSorted("date", Sort.ASCENDING), mClimbType, mDateRange, mDateOffset)); // send it to ALL subscribers. post sticky so this result stays until we set it again

    }

    private class ChartPagerAdapter extends FragmentPagerAdapter{
        private final MainActivity mMainActivity;
        private ArrayList<Pair<String,Fragment>> mFragmentList; // <title, fragment>

        public ChartPagerAdapter(MainActivity activity, FragmentManager fm) {
            super(fm);
            mMainActivity = activity;
            initiatePages();
        }

        private void initiatePages() {
            //Log.d(TAG, "initiatePages");
            mFragmentList = new ArrayList<>();

            Fragment fragment = new ListViewMobileFragment();
            mFragmentList.add(new Pair("List", fragment));

            // set climb type of the content fragment
            fragment = new OverviewMobileFragment();
            mFragmentList.add(new Pair("Overview", fragment));

            fragment = new GradeChartMobileFragment();
            mFragmentList.add(new Pair("Grades", fragment));

            fragment = new HistoryChartMobileFragment();
            mFragmentList.add(new Pair("History", fragment));
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position).second;
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentList.get(position).first;
        }


    }


}
