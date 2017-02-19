package com.example.grant.wearableclimbtracker;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
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
import android.widget.TextView;
import android.widget.Toast;

import com.example.mysynclibrary.eventbus.WearMessageEvent;
import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.eventbus.RealmResultsEvent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmResults;

import static org.greenrobot.eventbus.ThreadMode.MAIN;

public class MainActivity extends WearableActivity implements WearableActionDrawer.OnMenuItemClickListener {

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
                        .greaterThan("date", Shared.getStartOfDateRange(Shared.DateRange.DAY))
                        .equalTo("type", mClimbType.ordinal())
                        .findAll().sort("date");
                mRealm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        results.deleteLastFromRealm();
                    }
                });

                // TODO: delayed confirmation
                mDrawerLayout.closeDrawer(Gravity.BOTTOM);
                return true;
            case R.id.clear_climbs:
                results = mRealm.where(Climb.class)
                        .greaterThan("date",Shared.getStartOfDateRange(Shared.DateRange.DAY))
                        .equalTo("type", mClimbType.ordinal())
                        .findAll();
                mRealm.executeTransaction(new Realm.Transaction(){

                    @Override
                    public void execute(Realm realm) {
                        results.deleteAllFromRealm();
                    }
                });
                // TODO: delayed confirmation
                mDrawerLayout.closeDrawer(Gravity.BOTTOM);
                return true;
        }
        return false;
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
        setAmbientEnabled();

        mClimbType = Shared.ClimbType.values()[
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(PREF_TYPE, Shared.ClimbType.bouldering.ordinal())];

        // create the gridviewpager
        mGridViewPager = (GridViewPager)findViewById(R.id.pager);
        mContentPagerAdapter = new ContentPagerAdapter(getFragmentManager());
        mGridViewPager.setAdapter(mContentPagerAdapter);


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
    }

    @Override
    protected void onStop() {
        super.onStop();

        mGoogleApiClient.disconnect();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        EventBus.getDefault().register(this);
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
                .greaterThan("date",Shared.getStartOfDateRange(Shared.DateRange.DAY))
                .findAll();
        EventBus.getDefault().postSticky(new RealmResultsEvent(results, mClimbType)); // send it to ALL subscribers. post sticky so this result stays until we set it again

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

            fragment = new BarChartWearFragment();
            mFragmentList.add(fragment);

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

    @Subscribe(sticky = true, threadMode = MAIN) // need to do this in the same thread that realm was created
    public void onMobileMessageEvent(WearMessageEvent event) {
        switch(event.messageEvent.getPath()) {
            case Shared.REALM_SYNC_PATH:
                Log.d(TAG, "sync received -- sending data");

                // query for the data to sync
                List<Climb> allClimbs = mRealm.where(Climb.class).findAll().sort("date");
                List<Climb> allClimbsCopy = mRealm.copyFromRealm(allClimbs);

                // copy the results into a string format
                Gson gson = Shared.getGson();
                String json = gson.toJson(allClimbsCopy);
                Log.d(TAG, "sending json:" + json);

                // TODO: this is just to try sending as a message
                sendMessageToRealmDisplayer(Shared.REALM_SYNC_PATH, json.getBytes(StandardCharsets.UTF_8));

                /* // package the data into datamap and "put" it
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Shared.REALM_SYNC_PATH);
                putDataMapRequest.getDataMap().putString(Shared.DB_DATA_KEY, json);
                putDataMapRequest.getDataMap().putLong("Time",System.currentTimeMillis()); // add this to ensure data is always changed
                PutDataRequest putDataRequest= putDataMapRequest.asPutDataRequest().setUrgent();
                PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, putDataRequest);

                // check if the dataitem was updated
                pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(final DataApi.DataItemResult result) {
                        if(result.getStatus().isSuccess()) {
                            Log.d(TAG, "Data item set: " + result.getDataItem().getUri());
                        }
                    }
                });*/
                break;
            case Shared.REALM_ACK_PATH:
                Log.d(TAG, "ack received -- deleting old data");

                // the data was received on the other end, so anything older than a day can be deleted from the wearable
                final RealmResults<Climb> results = mRealm.where(Climb.class)
                        .lessThan("date", Shared.getStartOfDateRange(Shared.DateRange.DAY))
                        .findAll();
                mRealm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        results.deleteAllFromRealm();
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
}
