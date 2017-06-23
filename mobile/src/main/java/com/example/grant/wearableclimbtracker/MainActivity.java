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
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
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
import com.example.mysynclibrary.eventbus.EditClimbDialogEvent;
import com.example.mysynclibrary.eventbus.EditGoalDialogEvent;
import com.example.mysynclibrary.eventbus.ListScrollEvent;
import com.example.mysynclibrary.eventbus.RealmResultsEvent;
import com.example.mysynclibrary.eventbus.WearMessageEvent;
import com.example.mysynclibrary.realm.Climb;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
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
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.temporal.ChronoUnit;

import java.io.File;
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

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String REALM_CONTENT_CREATOR_CAPABILITY = "realm_content_creator";//Note: capability name defined in wear module values/wear.xml
    private static final String PREF_LASTSYNC = "prefLastSync";
    private static final String PREF_TYPE = "prefType";
    private static final String PREF_DATERANGE = "prefDateRange";
    private static final String PREF_SHOWDATERANGE = "prefShowDateRange";
    private static final long SYNC_TIMOUT_MS = 3000; // time to wait for response from wearable

    private static final int MAIN_INTRO_SHOT_ID = 1;
    private static final int LIST_SHOT_ID = 2;
    private static final int CHART_SHOT_ID = 3;
    private static final String FILE_AUTHORITY = "com.example.grant.wearableclimbtracker.fileprovider";

    private GoogleApiClient mGoogleApiClient;
    private String mNodeId;
    private Realm mRealm;
    private ChartPagerAdapter mChartPagerAdapter;
    private ViewPager mViewPager;
    private Switch typeToggle;
    private Shared.ClimbType mClimbType;
    private int mDateOffset;

    private TextView mDateTextView;
    private FloatingActionButton mAddClimbButton;
    private FloatingActionButton mAddGoalButton;


    private int mShowCaseIndex;
    private ShowcaseView mShowCaseView;

    private Handler mTimerHandler = new Handler();
    private Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(MainActivity.this,"No response from wear. Check that wear settings is enabled and app is open on wearable", Toast.LENGTH_LONG).show();
        }
    };
    private RealmResults<Climb> mResult;

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
                File imagePath = new File(getApplicationContext().getFilesDir(), "exports");
                boolean dirExists = imagePath.exists();
                if(!dirExists) {
                    dirExists = imagePath.mkdirs(); // create the directory if it doesn't exist already
                }
                if(dirExists) {
                    File newFile = new File(imagePath, "myclimbs.csv"); // create the csv file
                    try {
                        CSVWriter writer = new CSVWriter(new FileWriter(newFile), ',');  // write to the csv file
                        writer.writeNext(Climb.getTitleRow());

                        RealmResults<Climb> result = mRealm.where(Climb.class).notEqualTo("delete", true).findAll();
                        for (Climb climb : result) {
                            writer.writeNext(climb.toStringArray());
                        }
                        writer.close();
                    }catch(IOException e) {
                        Log.e(TAG, "error during csv write:" + e.getMessage());
                        return true;
                    }

                    Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                    intentShareFile.setAction(Intent.ACTION_SEND);

                    // NOTE: using https://medium.com/google-developers/sharing-content-between-android-apps-2e6db9d1368b
                    Uri contentUri;
                    try {
                        contentUri = FileProvider.getUriForFile(this, FILE_AUTHORITY, newFile);
                    }catch (IllegalArgumentException e){
                        Log.e(TAG, "The selected file can't be shared: " + newFile.getName());
                        return true;
                    }

                    if(contentUri!= null) {
                        Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                                .setStream(contentUri)
                                .setType("text/csv")
                                .createChooserIntent();
                        // Provide read access
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        if(shareIntent.resolveActivity(getPackageManager()) != null){
                            Log.d(TAG, "Successfully created share intent.  starting activity");
                            startActivity(shareIntent);
                        }else {
                            Log.e(TAG, "No activity available to handle MIME type ");
                        }
                    }else {
                        Log.e(TAG, "failed to get content URI");
                    }

                }else {
                    Log.e(TAG, "Could not create directory");
                }
                return true;
            case R.id.feedback:
                intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto","gkadokura+climbapp@gmail.com", null));
                intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback for climbing app"); // TODO: put correct name here
                //intent.putExtra(Intent.EXTRA_TEXT, message);
                startActivity(Intent.createChooser(intent, "Choose an Email client :"));
                return true;
            case R.id.showHelp:
                mShowCaseIndex = 0;
                resetShot(LIST_SHOT_ID);
                resetShot(CHART_SHOT_ID);
                mViewPager.setCurrentItem(1); // start in overview fragment
                showNextShowCaseView();
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
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mChartPagerAdapter);
        mViewPager.setCurrentItem(1); // start in overview fragment
        /*mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if(position == 0 || position == 1) {
                    mAddClimbButton.show(true);
                }else {
                    mAddClimbButton.hide(true);
                }
                showPagerShowcase(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });*/

        // get the shared preferences for type and date range
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        mClimbType = Shared.ClimbType.values()[pref.getInt(PREF_TYPE, Shared.ClimbType.bouldering.ordinal())];

        if(!hasShot(MAIN_INTRO_SHOT_ID)) {
            showNextShowCaseView();
            storeShot(MAIN_INTRO_SHOT_ID);
        }

        //  set title to last update date
        getSupportActionBar().setSubtitle("Last sync: " + pref.getString(PREF_LASTSYNC, "never"));

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

        final FloatingActionMenu menu = (FloatingActionMenu)findViewById(R.id.fab_menu);
        mAddClimbButton = (FloatingActionButton) findViewById(R.id.fab_add_climb);
        mAddClimbButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // add a climb, so open popup
                showEditClimbDialog(null);
                menu.close(true);

            }
        });
        mAddGoalButton = (FloatingActionButton) findViewById(R.id.fab_add_goal);
        mAddGoalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // add a climb, so open popup
                showEditGoalDialog(null);
                menu.close(true);
            }
        });

        invalidateRealmResult();

    }

    private void showPagerShowcase(int position) {
        if (position == 0 && !hasShot(LIST_SHOT_ID)) {
            mShowCaseView = new ShowcaseView.Builder(this)
                    .setTarget(new ViewTarget(R.id.pager_title_strip, this))
                    .setContentTitle("List View")
                    .setContentText("This is a list of climbs within the selected date range.  You can edit and delete climbs by long pressing on them.")
                    .setStyle(R.style.CustomShowcaseTheme)
                    .build();
            storeShot(LIST_SHOT_ID);
        } else if (position == 2 && !hasShot(CHART_SHOT_ID)) {
            mShowCaseView = new ShowcaseView.Builder(this)
                    .setTarget(new ViewTarget(R.id.pager_title_strip, this))
                    .setContentTitle("Chart View")
                    .setContentText("This is a chart of session stats within the selected date range.  Click on a bar to see the details of that session.")
                    .setStyle(R.style.CustomShowcaseTheme)
                    .build();
            storeShot(CHART_SHOT_ID);
        }
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
        showEditClimbDialog(event.climbUUID);
    }

    @Subscribe
    public void onEditGoalEvent(EditGoalDialogEvent event) {
        showEditGoalDialog(event.goalUUID);
    }

    @Subscribe
    public void onListScrollEvent(ListScrollEvent event) {
        switch (event.type) {
            case up:
                mAddClimbButton.show(true);
                break;
            case down:
                mAddClimbButton.hide(true);
                break;
        }
    }


    public void invalidateRealmResult() {
        //Log.d(TAG, "setClimbRealmResult");
        RealmQuery<Climb> realmQuery =  mRealm.where(Climb.class)
                .equalTo("delete", false)
                .equalTo("type", mClimbType.ordinal());

        /*ZonedDateTime startZDT = ZonedDateTime.now();
        startZDT = startZDT.truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime endZDT = startZDT.plus(1, ChronoUnit.DAYS);

        // add the filter
        realmQuery.between("date",Shared.ZDTToDate(startZDT),Shared.ZDTToDate(endZDT));*/


        // see http://stackoverflow.com/questions/43956135/realmresults-not-being-destroyed-on-new-eventbus-post/43982767#43982767
        // ADDED THIS TO AVOID CALLING MULTIPLE LISTENERS
        // When watching memory monitor (Android monitor -> monitors -> Memory), force GC removes additional instances of realm result, so this seems okay
        if(mResult!=null) {
            mResult.removeAllChangeListeners();
        }

        mResult = realmQuery.findAllSorted("date", Sort.ASCENDING);
        RealmChangeListener listener = new RealmChangeListener<RealmResults<Climb>>() {
            @Override
            public void onChange(RealmResults<Climb> element) {
                Log.d(TAG, "Realmresult onchange");
                EventBus.getDefault().postSticky(new RealmResultsEvent(element));
            }
        };
        mResult.addChangeListener(listener);
        EventBus.getDefault().postSticky(new RealmResultsEvent(mResult));

    }


    private void showNextShowCaseView() {
        // iterate to next showcase view and show it
        View menuView;
        ShowcaseView.Builder builder;
        switch(mShowCaseIndex) {
            case 0:
                String title = "Add Climb";
                String content = "Click this button to record a new climb";
                builder = new ShowcaseView.Builder(this)
                    .setTarget(new ViewTarget(R.id.fab_add_climb, this))
                    .setContentTitle(title)
                    .setContentText(content)
                    .setStyle(R.style.CustomShowcaseTheme)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showNextShowCaseView();
                        }
                    });
                mShowCaseView = builder.build();
                mShowCaseView.setButtonText("Next");
                break;

            case 4:
                mShowCaseView.setContentTitle("Your climbing goals");
                mShowCaseView.setContentText("This area helps you keep track of your goals, which you can change in the settings menu:\n" +
                        "\u2022 " + "Points*: I want to send 30 points per session\n" +
                        "\u2022 " + "Climbs: I want to send 10 climbs per session\n" +
                        "\u2022 " + "Grade: I want to be a V5 climber"+
                        "\n\n *Harder grades are worth more points. V0/5.6 = 0 pts, V1/5.7 = 1 pt, etc");
                mShowCaseView.setShowcase(new ViewTarget(R.id.pager_title_strip,this), true);
                break;
            case 5:
                EventBus.getDefault().post(new ShowcaseEvent(mShowCaseView, ShowcaseEvent.ShowcaseEventType.goals));
                break;
            case 6:
                mShowCaseView.hide();  // hide the previous views and start showcasing menu items
                builder = new ShowcaseView.Builder(this)
                        .setStyle(R.style.CustomShowcaseTheme)
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
                    mShowCaseView.setButtonText("Next");
                }else{
                    mShowCaseView = builder.setContentTitle(MenuDescription.getCondensedShowcaseTitle())
                            .setContentText(MenuDescription.getCondensedShowcaseText(MenuDescription.type))
                            .build();
                }
                break;
            case 7:
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
                    mShowCaseView.setButtonText("CLOSE");
                }
                break;
            case 8:
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
                    mShowCaseView.setButtonText("CLOSE");
                }
                break;
            case 9:
                // if we got here then the previous menu item was okay, so mShowCaseView was properly initialized
                menuView = findViewById(R.id.showHelp);
                if (menuView!=null) {
                    mShowCaseView.setContentTitle(MenuDescription.help.title);
                    mShowCaseView.setContentText(MenuDescription.help.text);
                    mShowCaseView.setShowcase(new ViewTarget(menuView), true);
                }else{
                    mShowCaseView.setShowcase(Target.NONE, true);
                    mShowCaseView.setContentTitle(MenuDescription.getCondensedShowcaseTitle());
                    mShowCaseView.setContentText(MenuDescription.getCondensedShowcaseText(MenuDescription.help));
                    mShowCaseView.overrideButtonClick(null);
                    mShowCaseView.setButtonText("CLOSE");
                }
                break;
            case 10:
                // if we got here then the previous menu item was okay, so mShowCaseView was properly initialized
                menuView = findViewById(R.id.export);
                if (menuView!=null) {
                    mShowCaseView.setContentTitle(MenuDescription.feedback.title);
                    mShowCaseView.setContentText(MenuDescription.feedback.text);
                    mShowCaseView.setShowcase(new ViewTarget(menuView), true);
                }else{
                    mShowCaseView.setShowcase(Target.NONE, true);
                    mShowCaseView.setContentTitle(MenuDescription.getCondensedShowcaseTitle());
                    mShowCaseView.setContentText(MenuDescription.getCondensedShowcaseText(MenuDescription.feedback));
                    mShowCaseView.overrideButtonClick(null);
                    mShowCaseView.setButtonText("CLOSE");
                }
                break;
            default:
                mShowCaseView.hide();
        }
        mShowCaseIndex++;
    }

    enum MenuDescription {
        type("Climb Type", "Toggle between bouldering and rope climbing"),
        settings("Settings", "Enable/disable wear, goal and warmup settings"),
        export("Export", "Export climbs to CSV"),
        help("Help", "Show this intro again"),
        feedback("Feedback", "Send an email to the developer.  \n\nPLEASE let me know what you think, what features you like and dislike, and what motivates you to climb in general!");

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
            mFragmentList.add(new Pair("LIST", fragment));

            // set climb type of the content fragment
            fragment = new OverviewMobileFragment();
            mFragmentList.add(new Pair("TODAY", fragment));

            /*fragment = new GradeChartMobileFragment();
            mFragmentList.add(new Pair("Grades", fragment));

            fragment = new HistoryChartMobileFragment();
            mFragmentList.add(new Pair("History", fragment));*/

            /*fragment = new CombinedChartMobileFragment();
            mFragmentList.add(new Pair("CHART", fragment));*/

            fragment = new GoalListFragment();
            mFragmentList.add(new Pair<>("GOALS", fragment));
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



    private void showEditClimbDialog(String selectedClimbUUID) {
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

    private void showEditGoalDialog(String selectedGoalUUID) {
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
        DialogFragment newFragment = EditGoalDialogFragment.newInstance(mClimbType.ordinal(), selectedGoalUUID);
        newFragment.show(ft, "dialog");
    }

    /******************** THIS IS FOR SINGLESHOT TRACKING OF SHOWCASE VIEW ******************************/
    boolean hasShot(int shotId) {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("hasShot" + shotId, false);
    }

    void storeShot(int shotId) {
        SharedPreferences internal = PreferenceManager.getDefaultSharedPreferences(this);
        internal.edit().putBoolean("hasShot" + shotId, true).apply();
    }

    void resetShot(int shotId) {
        SharedPreferences internal = PreferenceManager.getDefaultSharedPreferences(this);
        internal.edit().putBoolean("hasShot" + shotId, false).apply();
    }

}
