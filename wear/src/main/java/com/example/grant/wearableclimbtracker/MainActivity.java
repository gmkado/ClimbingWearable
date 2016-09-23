package com.example.grant.wearableclimbtracker;

import android.app.FragmentManager;
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

public class MainActivity extends WearableActivity implements WearableActionDrawer.OnMenuItemClickListener {

    private static final String TAG = "MainActivity";
    public static final String EXTRA_CLIMBTYPE = "ClimbType";
    private TextView mTextView;
    private ClimbType mSelectedClimbType;
    private WearableDrawerLayout mDrawerLayout;
    private WearableNavigationDrawer mNavigationDrawer;
    private WearableActionDrawer mActionDrawer;
    private GridViewPager mGridViewPager;
    private ContentPagerAdapter mContentPagerAdapter;

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        Log.d(TAG, "onMenuItemClick():" + menuItem);

        int itemId = menuItem.getItemId();

        DbHelper dbHelper = new DbHelper(this);
        switch(itemId) {
            case R.id.add_climb:
                // start addclimbactivity
                Intent intent = new Intent(this, AddClimbActivity.class);
                intent.putExtra(EXTRA_CLIMBTYPE, mSelectedClimbType);
                startActivity(intent);

                mDrawerLayout.closeDrawer(Gravity.BOTTOM);
                return true;
            case R.id.delete_last:
                String deleteQuery = DbHelper.ClimbEntry.COLUMN_CLIMB_TYPE + " =  " + Integer.toString(mSelectedClimbType.ordinal()) +
                        " AND " + DbHelper.ClimbEntry._ID + " = (SELECT MAX(" + DbHelper.ClimbEntry._ID +
                        ") FROM " + DbHelper.ClimbEntry.TABLE_NAME + ")";

                dbHelper.getWritableDatabase().delete(DbHelper.ClimbEntry.TABLE_NAME, deleteQuery, null);
                mContentPagerAdapter.notifyDataSetChanged();
                // TODO: delayed confirmation

                mDrawerLayout.closeDrawer(Gravity.BOTTOM);
                return true;
            case R.id.clear_climbs:
                String clearQuery =  DbHelper.ClimbEntry.COLUMN_CLIMB_TYPE + " =  " + Integer.toString(mSelectedClimbType.ordinal()) +
                        " AND " + DbHelper.ClimbEntry.COLUMN_DATETIME + " >= datetime('now', 'localtime', 'start of day')";
                dbHelper.getWritableDatabase().delete(DbHelper.ClimbEntry.TABLE_NAME, clearQuery, null);
                mContentPagerAdapter.notifyDataSetChanged();
                // TODO: delayed confirmation

                mDrawerLayout.closeDrawer(Gravity.BOTTOM);
                return true;
        }
        return false;
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
