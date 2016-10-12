package com.example.grant.wearableclimbtracker;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridPagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Grant on 8/1/2016.
 */
public class ContentPagerAdapter extends FragmentGridPagerAdapter {
    private final MainActivity mMainActivity;
    private List<Fragment> mFragmentList;

    public ContentPagerAdapter(MainActivity mainActivity, FragmentManager fragmentManager) {
        super(fragmentManager);
        mMainActivity = mainActivity;
        initiatePages();
    }

    public void initiatePages() {
        mFragmentList = new ArrayList<Fragment>();

        // set climb type of the content fragment
        Fragment fragment = new SessionOverviewFragment();
        Bundle args = new Bundle();
        args.putInt(SessionOverviewFragment.ARG_CLIMB_TYPE, mMainActivity.getClimbType().ordinal());
        fragment.setArguments(args);
        mFragmentList.add(fragment);

        fragment = new BarChartFragment();
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
