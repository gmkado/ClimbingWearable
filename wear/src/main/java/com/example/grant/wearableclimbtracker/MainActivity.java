package com.example.grant.wearableclimbtracker;

import android.app.Fragment;
import android.app.FragmentManager;
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

import com.example.mysynclibrary.model.Climb;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.model.RealmResultsEvent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

public class MainActivity extends WearableActivity implements WearableActionDrawer.OnMenuItemClickListener, MessageApi.MessageListener {

    private static final String TAG = "MainActivity";
    public static final String EXTRA_CLIMBTYPE = "ClimbType";
    private TextView mTextView;
    private Shared.ClimbType mSelectedClimbType;
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
        switch(messageEvent.getPath()) {
            case Shared.REALM_SYNC_PATH:
                Log.d(TAG, "sync received -- sending data");
                syncRealmDb();
                break;
            case Shared.REALM_ACK_PATH:
                Log.d(TAG, "ack received -- deleting old data");
                // delete all older than one day
                final RealmResults<Climb> results = mRealm.where(Climb.class)
                        .lessThan("date", Shared.getStartOfDay())
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


    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        Log.d(TAG, "onMenuItemClick():" + menuItem);
        int itemId = menuItem.getItemId();
        final RealmResults<Climb> results;

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
                        .greaterThan("date", Shared.getStartOfDay())
                        .equalTo("type", mSelectedClimbType.ordinal())
                        .findAll().sort("date");
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
                        .greaterThan("date",Shared.getStartOfDay())
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
        // query for the data to sync
        List<Climb> allClimbs = mRealm.where(Climb.class).findAll().sort("date");
        List<Climb> allClimbsCopy = mRealm.copyFromRealm(allClimbs);

        Gson gson = Shared.getGson();
        String json = gson.toJson(allClimbsCopy);

        Log.d(TAG, "sending json:" + json);
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Shared.REALM_SYNC_PATH);

        putDataMapRequest.getDataMap().putString(Shared.DB_DATA_KEY, json);

        PutDataRequest putDataRequest= putDataMapRequest.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, putDataRequest);

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

        mSelectedClimbType = Shared.ClimbType.bouldering;

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
            mSelectedClimbType = Shared.ClimbType.values()[i];
            setRealmResult();
            mContentPagerAdapter.notifyDataSetChanged();


        }


        @Override
        public int getCount() {
            return Shared.ClimbType.values().length;
        }
    }

    private void setRealmResult() {
        // run a query for today
        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        RealmResults<Climb> results = mRealm.where(Climb.class)
                .equalTo("type", mSelectedClimbType.ordinal())
                .greaterThan("date",cal.getTime())
                .findAll();
        EventBus.getDefault().postSticky(new RealmResultsEvent(results)); // send it to all subscribers. post sticky so this result stays until we set it again

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
            Fragment fragment = new SessionOverviewFragment();
            Bundle args = new Bundle();
            args.putInt(SessionOverviewFragment.ARG_CLIMB_TYPE, mSelectedClimbType.ordinal());
            fragment.setArguments(args);
            mFragmentList.add(fragment);

            fragment = new BarChartWearFragment();
            fragment.setArguments(args);
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
}
