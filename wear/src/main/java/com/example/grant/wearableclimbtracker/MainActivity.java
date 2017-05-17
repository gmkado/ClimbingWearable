package com.example.grant.wearableclimbtracker;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mysynclibrary.ClimbStats;
import com.example.mysynclibrary.eventbus.RealmResultsEvent;
import com.example.mysynclibrary.eventbus.WearMessageEvent;
import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.Shared;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.threeten.bp.temporal.ChronoUnit;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

import static org.greenrobot.eventbus.ThreadMode.MAIN;

public class MainActivity extends WearableActivity implements WearableActionDrawer.OnMenuItemClickListener, SharedPreferences.OnSharedPreferenceChangeListener {


    private static final String TAG = "MainActivity";
    public static final String EXTRA_CLIMBTYPE = "ClimbType";
    private static final String PREF_TYPE = "prefClimbType";
    private static final String PREFS_NAME = "mySharedPreferences";
    private static final String REALM_CONTENT_DISPLAYER_CAPABILITY = "realm_content_displayer"; //Note: capability name defined in mobile module values/wear.xml
    private TextView mTextView;
    private Shared.ClimbType mClimbType;
    private WearableDrawerLayout mDrawerLayout;
    private WearableNavigationDrawer mNavigationDrawer;
    private WearableActionDrawer mActionDrawer;
    private GridViewPager mGridViewPager;
    private ContentPagerAdapter mContentPagerAdapter;
    private Realm mRealm;
    private GoogleApiClient mGoogleApiClient;
    private String mNodeId;
    private GestureDetectorCompat mDetector;
    private ClimbStats mClimbStat;

    public MainActivity() {
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        Log.d(TAG, "onMenuItemClick():" + menuItem);
        int itemId = menuItem.getItemId();
        final RealmResults<Climb> results;

        switch(itemId) {
            case R.id.add_climb:
                // start addclimbactivity
                Intent intent = new Intent(this, AddClimbActivity.class);
                intent.putExtra(EXTRA_CLIMBTYPE, mClimbType);
                startActivity(intent);

                mDrawerLayout.closeDrawer(Gravity.BOTTOM);
                return true;
            case R.id.delete_last:
                results = mRealm.where(Climb.class)
                        .greaterThanOrEqualTo("date", Shared.getStartofDate(null))
                        .equalTo("type", mClimbType.ordinal())
                        .equalTo("delete", false)
                        .findAllSorted("date");

                if(results.size()>0) {
                    // show delayed confirmation
                    setDelayedViewVisible(true);
                    final DelayedConfirmationView delayedView = (DelayedConfirmationView) findViewById(R.id.deleteDelayedConfirmationView);
                    delayedView.setListener(new DelayedConfirmationView.DelayedConfirmationListener() {
                        @Override
                        public void onTimerFinished(View view) {
                            setDelayedViewVisible(false);
                            mRealm.executeTransaction(new Realm.Transaction() {
                                @Override
                                public void execute(Realm realm) {
                                    results.last().setDelete(true);
                                }
                            });
                        }

                        @Override
                        public void onTimerSelected(View view) {
                            delayedView.setPressed(true);

                            // Prevent onTimerFinished from being heard.
                            delayedView.setListener(null);
                            setDelayedViewVisible(false);
                        }
                    });
                    // Two seconds to cancel the action
                    delayedView.setTotalTimeMs(3000);
                    // Start the timer
                    delayedView.start();
                    mDrawerLayout.closeDrawer(Gravity.BOTTOM);
                }else {
                    Toast.makeText(MainActivity.this, "No climbs found", Toast.LENGTH_LONG).show();
                }
                return true;
        }
        return false;
    }

    private void setDelayedViewVisible(boolean delayedViewVisible) {
        Log.d(TAG, String.format("setDelayedViewVisible(%b)",delayedViewVisible));
        if(delayedViewVisible){
            findViewById(R.id.main_layout).setVisibility(View.GONE);
            findViewById(R.id.deleteConfirmationLayout).setVisibility(View.VISIBLE);
        }else{

            findViewById(R.id.main_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.deleteConfirmationLayout).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        setContentView(R.layout.activity_main);

        updateLayoutBasedOnPreference();
        setAmbientEnabled();

        mClimbType = Shared.ClimbType.values()[
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(PREF_TYPE, Shared.ClimbType.bouldering.ordinal())];

        // create the gridviewpager
        /*mGridViewPager = (GridViewPager)findViewById(R.id.pager);

        mContentPagerAdapter = new ContentPagerAdapter(getFragmentManager());

        mGridViewPager.setAdapter(mContentPagerAdapter);*/
        setupFragments();

        mDrawerLayout = (WearableDrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationDrawer = (WearableNavigationDrawer) findViewById(R.id.top_navigation_drawer);
        mActionDrawer = (WearableActionDrawer) findViewById(R.id.bottom_action_drawer);

        mNavigationDrawer.setAdapter(new NavigationAdapter());


        Menu menu = mActionDrawer.getMenu();
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_drawer_menu, menu);
        mActionDrawer.setOnMenuItemClickListener(this);


        // get the realm instance
        mRealm = Realm.getDefaultInstance();

        // create and connect the api client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        // Now you can use the Data Layer API
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

        invalidateRealmResult();

        setDelayedViewVisible(false);

    }

    private void updateLayoutBasedOnPreference() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(preferences.getBoolean(Shared.KEY_WEAR_ENABLED, false)) {
            findViewById(R.id.notenabled_textview).setVisibility(View.GONE);
            findViewById(R.id.drawer_layout).setVisibility(View.VISIBLE);
        }else{
            findViewById(R.id.notenabled_textview).setVisibility(View.VISIBLE);
            findViewById(R.id.drawer_layout).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    private void setupFragments() {
        Log.d(TAG, "setupFragments");
        addFragmentOnlyOnce(new OverviewWearFragment(), "overview");
        //addFragmentOnlyOnce(new BarChartWearFragment(), "barchart");
    }

    public void addFragmentOnlyOnce(Fragment fragment, String tag) {
        // Make sure the current transaction finishes first
        FragmentManager fm = getFragmentManager();
        fm.executePendingTransactions();

        // If there is no fragment yet with this tag...
        if (fm.findFragmentByTag(tag) == null) {
            // Add it
            FragmentTransaction transaction = fm.beginTransaction();
            transaction.add(R.id.content_frame, fragment, tag);
            transaction.commit();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(preferences.getBoolean(Shared.KEY_WEAR_ENABLED, false)) {
            mGoogleApiClient.disconnect();

        }
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(preferences.getBoolean(Shared.KEY_WEAR_ENABLED, false)) {

            mGoogleApiClient.connect();
        }

        EventBus.getDefault().register(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch(key) {
            case Shared.KEY_WEAR_ENABLED:
                updateLayoutBasedOnPreference();
                break;

        }
    }


    private class NavigationAdapter extends WearableNavigationDrawer.WearableNavigationDrawerAdapter {
        @Override
        public String getItemText(int i) {
            return Shared.ClimbType.values()[i].title;
        }

        @Override
        public Drawable getItemDrawable(int i) {
//            return null;
            return getDrawable(Shared.ClimbType.values()[i].icon);
        }

        @Override
        public void onItemSelected(int i) {
            mClimbType = Shared.ClimbType.values()[i];
            invalidateRealmResult();
        }

        @Override
        public int getCount() {
            return Shared.ClimbType.values().length;
        }
    }

    private void invalidateRealmResult() {
        // run a query for today
        RealmResults<Climb> results = mRealm.where(Climb.class)
                .equalTo("type", mClimbType.ordinal())
                .equalTo("delete", false)
                .greaterThan("date",Shared.getStartofDate(null))
                .findAll();
        results.addChangeListener(new RealmChangeListener<RealmResults<Climb>>() {
            @Override
            public void onChange(RealmResults<Climb> element) {
                mClimbStat = new ClimbStats(element,  mClimbType, ChronoUnit.DAYS, PreferenceManager.getDefaultSharedPreferences(MainActivity.this));
                EventBus.getDefault().postSticky(new RealmResultsEvent(mClimbStat, 0));
            }
        });
        mClimbStat = new ClimbStats(results,  mClimbType, ChronoUnit.DAYS, PreferenceManager.getDefaultSharedPreferences(this));
        EventBus.getDefault().postSticky(new RealmResultsEvent(mClimbStat, 0));
    }

    private class ContentPagerAdapter extends FragmentGridPagerAdapter {
        private List<Fragment> mFragmentList;

        public ContentPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
            initiatePages();
        }

        public void initiatePages() {
            mFragmentList = new ArrayList<>();

            // set climb type of the content fragment
            Fragment fragment = new OverviewWearFragment();
            mFragmentList.add(fragment);

            //fragment = new BarChartWearFragment();
            //mFragmentList.add(fragment);
        }

        @Override
        public int getRowCount() {
            return 1;
        }

        @Override
        public int getColumnCount(int row) {
            return mFragmentList.size();
        }

        @Override
        public Fragment getFragment(int row, int col) {
            return mFragmentList.get(col);
        }

    }

    @Subscribe(threadMode = MAIN) // need to do this in the same thread that realm was created
    public void onMobileMessageEvent(WearMessageEvent event) {
        final Gson gson = Shared.getGson();
        switch(event.messageEvent.getPath()) {
            case Shared.REALM_SYNC_PATH:
                //get the start of DAY of last data sync
                List<Climb> allClimbs = mRealm.where(Climb.class).findAll();
                List<Climb> allClimbsCopy = mRealm.copyFromRealm(allClimbs);

                // copy the results into a string format
                String json = gson.toJson(allClimbsCopy);
                Log.d(TAG, "Sending json:" + json);
                sendMessageToRealmDisplayer(Shared.REALM_SYNC_PATH, json.getBytes(StandardCharsets.UTF_8));
                break;
            case Shared.REALM_ACK_PATH:
                String data = new String(event.messageEvent.getData(), StandardCharsets.UTF_8);
                Log.d(TAG, "ACK received: " + data);
                final Climb[] climbList = gson.fromJson(data, Climb[].class);

                // the data was received on the other end, so anything older than a day or marked for deletion can be deleted from the wearable
                mRealm.executeTransactionAsync(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        realm.deleteAll();
                        realm.copyToRealm(Arrays.asList(climbList));
                    }
                }, new Realm.Transaction.OnError() {
                    @Override
                    public void onError(Throwable error) {
                        Log.e(TAG, "failed adding updated climblist: " + error.getMessage());
                    }
                });
                break;
            default:
                Log.e (TAG, "Unrecognized message");
        }
    }



    private void sendMessageToRealmDisplayer(final String path, final byte[] data) {
        if(mNodeId == null) {
            // need to find a capable node
            PendingResult result = Wearable.CapabilityApi.getCapability(
                    mGoogleApiClient, REALM_CONTENT_DISPLAYER_CAPABILITY,
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
                        Log.e(TAG, "No capable nodes found. This shouldn't happen");
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
        Log.d(TAG, "sendMessage");
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

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        // Make appropriate UI changes
        Log.d(TAG, "exiting ambient");
        EventBus.getDefault().post(new AmbientEvent(false));

    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        Log.d(TAG, "entering ambient");
        // Make appropriate UI changes
        EventBus.getDefault().post(new AmbientEvent(true));

    }


}
