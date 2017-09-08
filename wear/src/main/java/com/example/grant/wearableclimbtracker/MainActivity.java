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

import com.example.mysynclibrary.SyncHelper;
import com.example.mysynclibrary.eventbus.RealmSyncEvent;
import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.realm.ClimbFields;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

public class MainActivity extends WearableActivity implements WearableActionDrawer.OnMenuItemClickListener, SharedPreferences.OnSharedPreferenceChangeListener {


    private static final String TAG = "MainActivity";
    public static final String EXTRA_CLIMBTYPE = "ClimbType";
    private static final String PREF_TYPE = "prefClimbType";
    private static final String PREFS_NAME = "mySharedPreferences";
    private Shared.ClimbType mClimbType;
    private WearableDrawerLayout mDrawerLayout;
    private WearableNavigationDrawer mNavigationDrawer;
    private WearableActionDrawer mActionDrawer;
    private GridViewPager mGridViewPager;
    private ContentPagerAdapter mContentPagerAdapter;
    private Realm mRealm;
    private GestureDetectorCompat mDetector;
    private SyncHelper.ClientSide mClientHelper;

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
                        .equalTo(ClimbFields.TYPE, mClimbType.ordinal())
                        .equalTo(ClimbFields.SYNC_STATE.DELETE, false)
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
                                    results.last().safeDelete();
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
        mClientHelper = new SyncHelper(this).new ClientSide();

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
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
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
                .equalTo(ClimbFields.TYPE, mClimbType.ordinal())
                .equalTo(ClimbFields.SYNC_STATE.DELETE, false)
                .findAll();
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

    @Subscribe
    public void onRealmSyncEvent(RealmSyncEvent event) {
        switch(event.step) {
            case SYNC_REQUESTED:
                mClientHelper.sendRealmDb();
                break;
            case REMOTE_SAVED_TO_TEMP:
                mClientHelper.overwriteLocalWithRemote();
                break;
            case REALM_OBJECT_MERGED:
                // do nothing
                break;
            case REALM_DB_MERGED:
                // TODO: db is merged now, should we take any action?
                break;

        }

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
