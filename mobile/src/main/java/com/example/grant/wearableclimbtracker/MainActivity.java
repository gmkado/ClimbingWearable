package com.example.grant.wearableclimbtracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
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

import com.example.mysynclibrary.ClimbStats;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.eventbus.DaySelectedEvent;
import com.example.mysynclibrary.eventbus.EditClimbDialogEvent;
import com.example.mysynclibrary.eventbus.ListScrollEvent;
import com.example.mysynclibrary.eventbus.RealmResultsEvent;
import com.example.mysynclibrary.eventbus.WearMessageEvent;
import com.example.mysynclibrary.realm.Climb;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.github.clans.fab.FloatingActionButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.opencsv.CSVWriter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.honorato.multistatetogglebutton.MultiStateToggleButton;
import org.honorato.multistatetogglebutton.ToggleButton;
import org.threeten.bp.temporal.ChronoUnit;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "MainActivity";
    private static final String REALM_CONTENT_CREATOR_CAPABILITY = "realm_content_creator";//Note: capability name defined in wear module values/wear.xml
    private static final String PREF_LASTSYNC = "prefLastSync";
    private static final String PREF_TYPE = "prefType";
    private static final String PREF_DATERANGE = "prefDateRange";
    private static final long SYNC_TIMOUT_MS = 3000; // time to wait for response from wearable
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
    private FloatingActionButton mAddButton;
    private MultiStateToggleButton mDateRangeButton;

    private int mShowCaseIndex;
    private ShowcaseView mShowCaseView;
    private Menu mActionBarMenu;
    private ClimbStats mClimbStat;

    private Handler mTimerHandler = new Handler();
    private Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(MainActivity.this,"No response from wear. Check that wear settings is enabled and app is open on wearable", Toast.LENGTH_LONG).show();
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        mActionBarMenu = menu; // get a handle to the menu for showcaseview
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        // check last climbtype and set current view
        typeToggle = (Switch)menu.findItem(R.id.type_toggle).getActionView().findViewById(R.id.switch1);
        typeToggle.setChecked(mClimbType == Shared.ClimbType.bouldering);

        typeToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mClimbType = isChecked? Shared.ClimbType.bouldering: Shared.ClimbType.ropes;
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                // save this in shared pref.edit();
                editor.putInt(PREF_TYPE, mClimbType.ordinal());
                editor.commit();

                invalidateRealmResult();
            }
        });

        // if wear preference is turned off, hide sync button
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean wearEnabled = sharedPref.getBoolean(Shared.KEY_WEAR_ENABLED, false);
        if(wearEnabled) {
            menu.findItem(R.id.sync_db).setVisible(true);
            getSupportActionBar().setSubtitle("Last sync: " + sharedPref.getString(PREF_LASTSYNC, "never"));
        }else {
            menu.findItem(R.id.sync_db).setVisible(false);
            getSupportActionBar().setSubtitle("");
        }

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Log.d(TAG, "onOptionsItemSelected");
        Intent intent;
        switch (item.getItemId()) {
            case R.id.sync_db:
                // start sync process
                Log.d(TAG, "requesting sync");
                sendMessageToRealmCreator(Shared.REALM_SYNC_PATH, null);

                // start a timer to see if we get a response
                mTimerHandler.postDelayed(mTimerRunnable, SYNC_TIMOUT_MS);
                return true;
            case R.id.settings:
                // open settings
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.export:
                // query all sends and export to CSV
                try {
                    String filename = getExternalFilesDir(null) + "/myclimbs.csv";
                    CSVWriter writer = new CSVWriter(new FileWriter(filename), ',');
                    writer.writeNext(Climb.getTitleRow());

                    RealmResults<Climb> result = mRealm.where(Climb.class).notEqualTo("delete", true).findAll();
                    for(Climb climb: result) {
                        writer.writeNext(climb.toStringArray());
                    }
                    writer.close();
                    Toast.makeText(this, "Saved climbs to " + filename, Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return true;
            case R.id.feedback:
                intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto","gkadokura@gmail.com", null));
                intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback for climbing app"); // TODO: put correct name here
                //intent.putExtra(Intent.EXTRA_TEXT, message);
                startActivity(Intent.createChooser(intent, "Choose an Email client :"));
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
    protected void onResume() {
        super.onResume();
        // register preference listener
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregister preference listener
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
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
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if(position == 0 || position == 1) {
                    mAddButton.show(true);
                }else {
                    mAddButton.hide(true);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // get the shared preferences for type and date range
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        mClimbType = Shared.ClimbType.values()[pref.getInt(PREF_TYPE, Shared.ClimbType.bouldering.ordinal())];
        mDateRange = ChronoUnit.values()[pref.getInt(PREF_DATERANGE, ChronoUnit.FOREVER.ordinal())];

        showNextShowCaseView();

        //  set title to last update date
        getSupportActionBar().setSubtitle("Last sync: " + pref.getString(PREF_LASTSYNC, "never"));

        // setup date toggle button
        mDateRangeButton = (MultiStateToggleButton)findViewById(R.id.mstb_daterange);
        mDateRangeButton.setElements(mDateRangeLabels);
        mDateRangeButton.setValue(mDateRanges.indexOf(mDateRange));

        mDateRangeButton.setOnValueChangedListener(new ToggleButton.OnValueChangedListener(){

            @Override
            public void onValueChanged(int position) {
                // change the date range
                mDateRange = mDateRanges.get(position);

                // reset the offset
                mDateOffset = 0;

                // save this in shared pref
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
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
                        //sendMessageToRealmCreator(Shared.REALM_SYNC_PATH, null);
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

        mAddButton = (FloatingActionButton) findViewById(R.id.fab_add_climb);
        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // add a climb, so open popup
                showAddClimbDialog(null);
            }
        });

        TextView currentDateView = (TextView) findViewById(R.id.currentdate_textview);
        currentDateView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // toggle view of dateranges
                if(mDateRangeButton.getVisibility() == View.GONE) {
                    mDateRangeButton.setVisibility(View.VISIBLE);
                }else {
                    mDateRangeButton.setVisibility(View.GONE);
                }
            }
        });
        mDateRangeButton.setVisibility(View.GONE);

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
                        Toast.makeText(MainActivity.this,"No wearable found", Toast.LENGTH_LONG).show();
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

    /**************** EVENT BUS LISTENERS ***************************/
    @Subscribe (threadMode = ThreadMode.MAIN)
    public void onWearMessageReceived(WearMessageEvent event) {
        switch(event.messageEvent.getPath()) {
            case Shared.REALM_SYNC_PATH:
                // stop timer since we got a response
                mTimerHandler.removeCallbacks(mTimerRunnable);

                // received the sync data
                final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
                String data =  new String(event.messageEvent.getData(), StandardCharsets.UTF_8);

                Log.d(TAG, "got message = " + data);

                //get the start of DAY of last data sync
                final Gson gson = Shared.getGson();
                final Climb[] climbList = gson.fromJson(data, Climb[].class);

                // convert data to realm and add
                mRealm.executeTransactionAsync(new Realm.Transaction() {

                    @Override
                    public void execute(Realm realm) {
                        for (Climb wearClimb : climbList) {
                            // get the climb on mobile
                            Climb mobileClimb = realm.where(Climb.class).equalTo("id", wearClimb.getId()).findFirst();
                            if(wearClimb.isDelete() && mobileClimb!=null) {
                                // wear climb marked for deletion which takes precedence over mobile climb
                                mobileClimb.deleteFromRealm();
                            }else if(mobileClimb!=null){
                                if(mobileClimb.isDelete()) {
                                    // mobile climb marked for deletion, which takes precedence over wear climb
                                    mobileClimb.deleteFromRealm();
                                }else if (wearClimb.getLastedit().after(mobileClimb.getLastedit())) {
                                    // use wear climb if it was edited last, otherwise keep mobile
                                    realm.copyToRealmOrUpdate(wearClimb);
                                }
                            }else {
                                // climb doesn't exist on mobile, so add it
                                realm.copyToRealm(wearClimb);
                            }
                        }
                        // query all marked for deletion and delete
                        RealmResults<Climb> deleteClimbs = realm.where(Climb.class).equalTo("delete", true).findAll();
                        deleteClimbs.deleteAllFromRealm();


                        //set today as the new last sync date
                        SharedPreferences.Editor editor = settings.edit();
                        DateFormat df = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

                        final String newSyncDate = df.format(new Date());
                        editor.putString(PREF_LASTSYNC, newSyncDate);
                        editor.commit();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getSupportActionBar().setSubtitle("Last sync: " + newSyncDate);
                            }
                        });
                        List<Climb> nonWear = realm.where(Climb.class).lessThan("date",Shared.getStartofDate(null)).equalTo("onwear", true).findAll();
                        for (Climb climb: nonWear) {
                            climb.setOnwear(false);
                        }

                        List<Climb> todaysClimbs = realm.where(Climb.class).greaterThanOrEqualTo("date",Shared.getStartofDate(null)).findAll();

                        //set onwear to true
                        for (Climb climb: todaysClimbs) {
                            climb.setOnwear(true);
                        }
                        List<Climb> todaysClimbsCopy = realm.copyFromRealm(todaysClimbs);

                        // copy the results into a string format
                        String json = gson.toJson(todaysClimbsCopy);
                        Log.d(TAG, "Sending ACK:" + json);
                        sendMessageToRealmCreator(Shared.REALM_ACK_PATH, json.getBytes(StandardCharsets.UTF_8));
                    }
                }, new Realm.Transaction.OnError() {
                    @Override
                    public void onError(Throwable error) {
                        Log.e(TAG, "Failed merging wear data with mobile: " + error.getMessage());
                    }
                });
                break;
            default:
                Log.e(TAG, "onWearMessageReceived: message path not recognized");
                break;
        }

    }

    @Subscribe
    public void onEditClimbDialogEvent(EditClimbDialogEvent event) {
        switch (event.type) {
            case OPEN_REQUEST:
                showAddClimbDialog(event.climbUUID);
                break;
            case DISMISSED:
                invalidateRealmResult();
                break;
        }
    }
    @Subscribe
    public void onListScrollEvent(ListScrollEvent event) {
        switch (event.type) {
            case up:
                mAddButton.show(true);
                break;
            case down:
                mAddButton.hide(true);
                break;
        }
    }

    @Subscribe
    public void onDaySelectedEvent(DaySelectedEvent event) {
        mDateRange = ChronoUnit.DAYS;
        mDateRangeButton.setValue(mDateRanges.indexOf(mDateRange));

        mDateOffset = Shared.getOffsetFromDate(event.date, mDateRange);

        invalidateRealmResult();
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
        RealmResults<Climb> result = realmQuery.findAllSorted("date", Sort.ASCENDING);
        result.addChangeListener(new RealmChangeListener<RealmResults<Climb>>() {
            @Override
            public void onChange(RealmResults<Climb> element) {
                mClimbStat = new ClimbStats(element,  mClimbType, mDateRange, PreferenceManager.getDefaultSharedPreferences(MainActivity.this));
                postRealmResult();
            }
        });
        mClimbStat = new ClimbStats(result,  mClimbType, mDateRange, PreferenceManager.getDefaultSharedPreferences(this));
        postRealmResult();
    }

    private void postRealmResult() {
        EventBus.getDefault().postSticky(new RealmResultsEvent(mClimbStat, mDateOffset));
    }

    private void showNextShowCaseView() {
        // iterate to next showcase view and show it
        View menuView;
        switch(mShowCaseIndex) {
            case 0:
                mShowCaseView = new ShowcaseView.Builder(this)
                        .setTarget(new ViewTarget(R.id.fab_add_climb, this))
                        .setContentTitle("Add Climb")
                        .setContentText("This is for adding new climbs")
                        .setStyle(R.style.CustomShowcaseTheme)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                showNextShowCaseView();
                            }
                        })
                        .singleShot(1)
                        .build();
                break;
            case 1:
                mShowCaseView.setContentTitle("Date range");
                mShowCaseView.setContentText("Tap to select a new range");
                mShowCaseView.setShowcase(new ViewTarget(R.id.layout_daterange, this), true);
                break;
            case 2:
                mShowCaseView.setContentTitle("Previous");
                mShowCaseView.setContentText("Go back in time");
                mShowCaseView.setShowcase(new ViewTarget(R.id.prev_imagebutton, this), true);
                break;
            case 3:
                mShowCaseView.setContentTitle("Next");
                mShowCaseView.setContentText("Go forward in time");
                mShowCaseView.setShowcase(new ViewTarget(R.id.next_imagebutton, this), true);
                break;
            case 4:
                mShowCaseView.setContentTitle("Your climb history");
                mShowCaseView.setContentText("This area shows information of all climbs done in the selected range.  Swipe left and right to see other views.");
                break;
            case 5:
                mShowCaseView.hide();  // hide the previous views and start showcasing menu items

                ShowcaseView.Builder builder  = new ShowcaseView.Builder(this)
                        .setStyle(R.style.CustomShowcaseTheme)
                        .singleShot(2)
                        .useDecorViewAsParent();
                menuView = findViewById(R.id.type_toggle);
                if (menuView!=null) {
                    mShowCaseView = builder.setContentTitle(MenuDescription.type.title)
                            .setContentText(MenuDescription.type.text)
                            .setTarget(new ViewTarget(menuView))
                            .setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    showNextShowCaseView();
                                }
                            }).build();
                }else{
                    mShowCaseView = builder.setContentTitle(MenuDescription.getCondensedShowcaseTitle())
                            .setContentText(MenuDescription.getCondensedShowcaseText(MenuDescription.type))
                            .build();
                }
                break;
            case 6:
                // if we got here then the previous menu item was okay, so mShowCaseView was properly initialized
                menuView = findViewById(R.id.settings);
                if (menuView!=null) {
                    mShowCaseView.setContentTitle(MenuDescription.settings.title);
                    mShowCaseView.setContentText(MenuDescription.settings.text);
                    mShowCaseView.setShowcase(new ViewTarget(menuView), true);
                }else{
                    mShowCaseView.setShowcase(Target.NONE, true);
                    mShowCaseView.setContentTitle(MenuDescription.getCondensedShowcaseTitle());
                    mShowCaseView.setContentText(MenuDescription.getCondensedShowcaseText(MenuDescription.settings));
                    mShowCaseView.overrideButtonClick(null);
                }
                break;
            case 7:
                // if we got here then the previous menu item was okay, so mShowCaseView was properly initialized
                menuView = findViewById(R.id.export);
                if (menuView!=null) {
                    mShowCaseView.setContentTitle(MenuDescription.export.title);
                    mShowCaseView.setContentText(MenuDescription.export.text);
                    mShowCaseView.setShowcase(new ViewTarget(menuView), true);
                }else{
                    mShowCaseView.setShowcase(Target.NONE, true);
                    mShowCaseView.setContentTitle(MenuDescription.getCondensedShowcaseTitle());
                    mShowCaseView.setContentText(MenuDescription.getCondensedShowcaseText(MenuDescription.export));
                    mShowCaseView.overrideButtonClick(null);
                }
                break;
            default:
                mShowCaseView.hide();
        }
        mShowCaseIndex++;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // update the preferences in the stats object
        //TODO: this is inefficient, essentially redrawing the entire app whenever a pref is changed
        mClimbStat.updatePreferences(sharedPreferences);
        postRealmResult();
    }

    enum MenuDescription {
        type("Climb StatType", "Toggle between bouldering and rope climbing"),
        settings("Settings", "Enable/disable wear, warmup settings"),
        export("Export", "Export climbs to CSV"),
        feedback("Feedback", "Send an email to the developer");

        private final String title;
        private final String text;

        MenuDescription(String title, String text){
            this.title = title;
            this.text = text;
        }

        public String getBulletPoint() {
            return "\u2022 " + this.title + ": " + this.text;
        }

        public static String getCondensedShowcaseTitle(){return "Other menu options:";}

        public static String getCondensedShowcaseText(MenuDescription item) {
            // return a bulletpoint list of all menu items after item
            List<MenuDescription> list = Arrays.asList(MenuDescription.values()).subList(item.ordinal(), MenuDescription.values().length);
            String showcaseText="";
            for (MenuDescription desc: list) {
                showcaseText = showcaseText+desc.getBulletPoint()+"\n";
            }
            return showcaseText;
        }
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

            /*fragment = new GradeChartMobileFragment();
            mFragmentList.add(new Pair("Grades", fragment));

            fragment = new HistoryChartMobileFragment();
            mFragmentList.add(new Pair("History", fragment));*/

            fragment = new CombinedChartMobileFragment();
            mFragmentList.add(new Pair("Combined", fragment));
        }

        @Override
        public Fragment getItem(int position) {
            // hide fab button for graph views
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



    private void showAddClimbDialog(String selectedClimbUUID) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = EditClimbDialogFragment.newInstance(mClimbType.ordinal(), selectedClimbUUID);
        newFragment.show(ft, "dialog");
    }


}
