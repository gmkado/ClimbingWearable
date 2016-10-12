package com.example.grant.wearableclimbtracker;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
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

import com.example.grant.wearableclimbtracker.model.Climb;
import com.example.mysynclibrary.Tools;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Calendar;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmResults;

public class MainActivity extends WearableActivity implements WearableActionDrawer.OnMenuItemClickListener, MessageApi.MessageListener {

    private static final String TAG = "MainActivity";
    public static final String EXTRA_CLIMBTYPE = "ClimbType";
    private TextView mTextView;
    private ClimbType mSelectedClimbType;
    private WearableDrawerLayout mDrawerLayout;
    private WearableNavigationDrawer mNavigationDrawer;
    private WearableActionDrawer mActionDrawer;
    private GridViewPager mGridViewPager;
    private ContentPagerAdapter mContentPagerAdapter;
    private Realm mRealm;
    private int count = 0;
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived");
        if (messageEvent.getPath().equals(Tools.REALM_SYNC_PATH)) {
            syncRealmDb();
        }
    }


    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        Log.d(TAG, "onMenuItemClick():" + menuItem);

        int itemId = menuItem.getItemId();

        final RealmResults<Climb> results;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startOfDay = cal.getTime();

        switch(itemId) {
            case R.id.add_climb:
                // start addclimbactivity
                Intent intent = new Intent(this, AddClimbActivity.class);
                intent.putExtra(EXTRA_CLIMBTYPE, mSelectedClimbType);
                startActivity(intent);

                mDrawerLayout.closeDrawer(Gravity.BOTTOM);
                return true;
            case R.id.delete_last:
                results = mRealm.where(Climb.class)
                        .greaterThan("date", startOfDay)
                        .equalTo("type", mSelectedClimbType.ordinal())
                        .findAll();
                mRealm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        results.deleteLastFromRealm();
                    }
                });

                mContentPagerAdapter.notifyDataSetChanged();
                // TODO: delayed confirmation
                mDrawerLayout.closeDrawer(Gravity.BOTTOM);
                return true;
            case R.id.clear_climbs:
                results = mRealm.where(Climb.class)
                        .greaterThan("date",startOfDay)
                        .equalTo("type", mSelectedClimbType.ordinal())
                        .findAll();
                mRealm.executeTransaction(new Realm.Transaction(){

                    @Override
                    public void execute(Realm realm) {
                        results.deleteAllFromRealm();
                    }
                });
                // TODO: delayed confirmation
                mContentPagerAdapter.notifyDataSetChanged();
                mDrawerLayout.closeDrawer(Gravity.BOTTOM);
                return true;
        }
        return false;
    }


    public void syncRealmDb() {
        Log.d(TAG, "syncRealmDb");
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Tools.REALM_SYNC_PATH);
        putDataMapRequest.getDataMap().putInt(Tools.COUNT_KEY, count++);

        PutDataRequest putDataRequest= putDataMapRequest.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, putDataRequest);
    }



    public enum ClimbType {
        bouldering("Bouldering", R.drawable.icon_boulder),
        ropes("Ropes", R.drawable.icon_ropes);

        public String title;
        int icon;

        ClimbType(String title, int icon){
            this.title = title;
            this.icon = icon;
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
        setAmbientEnabled();

        mSelectedClimbType = ClimbType.bouldering;

        // create the gridviewpager
        mGridViewPager = (GridViewPager)findViewById(R.id.pager);
        mContentPagerAdapter = new ContentPagerAdapter(this, getFragmentManager());
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

                        Wearable.MessageApi.addListener(mGoogleApiClient, MainActivity.this);
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

    }

    @Override
    protected void onStop() {
        super.onStop();

        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }



    public ClimbType getClimbType() {
        return mSelectedClimbType;
    }

    private class NavigationAdapter extends WearableNavigationDrawer.WearableNavigationDrawerAdapter {
        @Override
        public String getItemText(int i) {
            return ClimbType.values()[i].title;
        }

        @Override
        public Drawable getItemDrawable(int i) {
//            return null;
            return getDrawable(ClimbType.values()[i].icon);
        }

        @Override
        public void onItemSelected(int i) {
            mSelectedClimbType = ClimbType.values()[i];

            // TODO: do I need to call initiatePages again?
            mContentPagerAdapter.initiatePages();
            mContentPagerAdapter.notifyDataSetChanged();


        }


        @Override
        public int getCount() {
            return ClimbType.values().length;
        }
    }
}
