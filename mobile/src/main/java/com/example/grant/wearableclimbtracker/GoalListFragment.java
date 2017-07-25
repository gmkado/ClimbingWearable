package com.example.grant.wearableclimbtracker;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.akexorcist.roundcornerprogressbar.TextRoundCornerProgressBar;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.goalDAO.GoalDAO;
import com.example.mysynclibrary.realm.Goal;
import com.github.clans.fab.FloatingActionButton;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CombinedData;

import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.FormatStyle;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

/**
 * A fragment representing a list of Items.
 */
public class GoalListFragment extends Fragment {
    private Realm mRealm;
    private RealmResults<Goal> mResult;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public GoalListFragment() {
    }

    public static GoalListFragment newInstance() {
        GoalListFragment fragment = new GoalListFragment();
        //Bundle args = new Bundle();
        //args.putInt(ARG_COLUMN_COUNT, columnCount);
        //fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_goal_list, container, false);

        mRealm = Realm.getDefaultInstance();

        // Set the adapter
        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));

        mResult = mRealm.where(Goal.class).findAll();
        // Add changelistener so if we add a goal, this view updates
        mResult.addChangeListener(new RealmChangeListener<RealmResults<Goal>>() {
            @Override
            public void onChange(RealmResults<Goal> element) {
                recyclerView.setAdapter(new MyGoalRecyclerViewAdapter(getGoalList(element)));
            }
        });
        recyclerView.setAdapter(new MyGoalRecyclerViewAdapter(getGoalList(mResult)));

        // add goal button listener
        FloatingActionButton addGoalButton = (FloatingActionButton) view.findViewById(R.id.fab_add_goal);
        addGoalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditGoalDialog(null);
            }
        });
        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mResult!=null) {
            mResult.removeAllChangeListeners();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRealm.close();
    }

    private List<GoalDAO> getGoalList(RealmResults<Goal> result) {
        ArrayList<GoalDAO> goallist = new ArrayList<>();
        for(Goal goal:result) {
            goallist.add(new GoalDAO(goal));
        }

        return goallist;
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }

    /**
     * {@link RecyclerView.Adapter} that can display a {@link GoalDAO}
     */
    public class MyGoalRecyclerViewAdapter extends RecyclerView.Adapter<MyGoalRecyclerViewAdapter.ViewHolder> {

        private final List<GoalDAO> mValues;

        public MyGoalRecyclerViewAdapter(List<GoalDAO> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.listitem_goal_mobile, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = mValues.get(position);

            holder.mTitleTextView.setText(holder.mItem.getSummary());

            // TODO: rethink how important it is to keep different goal types

            holder.mMenu.setVisibility(View.VISIBLE);
            // populate mMenu and add onclicklistener
            holder.mMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu menu = new PopupMenu(getContext(), holder.mMenu);
                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            int id = item.getItemId();
                            switch (id) {
                                case R.id.item_edit:
                                    showEditGoalDialog(holder.mItem.getID());
                                    break;
                            }
                            return true;
                        }
                    });
                    menu.inflate(R.menu.menu_goal_listitem);
                    menu.show();
                }
            });
            holder.mNonrecurringView.getLayoutTransition().setAnimateParentHierarchy(false); // this fixed weird animate overlap issues https://stackoverflow.com/questions/36064424/animate-layout-changes-broken-in-nested-layout-with-collapsingtoolbarlayout
            holder.mNonrecurringProgressBar.setProgressColor(Color.parseColor("#56d2c2"));
            holder.mNonrecurringProgressBar.setProgressBackgroundColor(Color.parseColor("#757575"));
            //holder.mNonrecurringProgressBar.setIconBackgroundColor(Color.parseColor("#38c0ae"));
            holder.refreshNonrecurringProgressBar();

            holder.refreshNonrecurringDateText();
            holder.mItem.setGoalListener(new GoalDAO.GoalDAOListener() {
                @Override
                public void onRecurringStatsChange() {
                    holder.refreshRecurringChart();
                    holder.refreshRecurringProgressBar();
                }

                @Override
                public void onNonrecurringStatsChanged() {
                    holder.refreshNonRecurringChart();
                    holder.refreshNonrecurringProgressBar();
                }

                @Override
                public void onNonrecurringDateRangeChanged() {
                    holder.refreshNonrecurringDateText();
                }

                @Override
                public void onRecurringDateRangeChanged() {
                    holder.refreshRecurringDateRangeText();
                }
            });
            holder.refreshNonRecurringChart();

            if(holder.mItem.isRecurring()) {
                holder.mRecurringView.getLayoutTransition().setAnimateParentHierarchy(false); // this fixed weird animate overlap issues https://stackoverflow.com/questions/36064424/animate-layout-changes-broken-in-nested-layout-with-collapsingtoolbarlayout
                holder.mRecurringProgressBar.setProgressColor(Color.parseColor("#56d2c2"));
                holder.mRecurringProgressBar.setProgressBackgroundColor(Color.parseColor("#757575"));
                //holder.mRecurringProgressBar.setIconBackgroundColor(Color.parseColor("#38c0ae"));
                holder.refreshRecurringProgressBar();
                holder.refreshRecurringDateRangeText();
                //holder.mNonrecurringProgressBar.setIconImageResource(android.R.drawable.btn_plus);
                holder.refreshRecurringChart();
            }else {
                holder.mRecurringView.setVisibility(View.GONE);
            }

            //holder.mTitleTextView.setText(mValues.get(position).id);
            //holder.mNonrecurringProgressBar.setText(mValues.get(position).content);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final View mView;
            final TextView mTitleTextView;
            private ImageButton mMenu;
            TextView mNonrecurringDateRangeTextView;
            TextView mRecurringDateRangeTextView;

            private final ViewGroup mNonrecurringView;
            final TextRoundCornerProgressBar mNonrecurringProgressBar;
            private final CombinedChart mNonrecurringChart;
            private final TextRoundCornerProgressBar mRecurringProgressBar;
            private final CombinedChart mRecurringChart;
            private final ViewGroup mRecurringView;


            GoalDAO mItem;

            ViewHolder(final View view) {
                super(view);
                mView = view;
                mTitleTextView = (TextView) view.findViewById(R.id.textview_title);
                mMenu = (ImageButton) view.findViewById(R.id.imagebutton_menu);
                mNonrecurringView = (ViewGroup) view.findViewById(R.id.layout_nonrecurring);
                mNonrecurringProgressBar = (TextRoundCornerProgressBar) mNonrecurringView.findViewById(R.id.rcprogress);
                mNonrecurringChart = (CombinedChart)mNonrecurringView.findViewById(R.id.chart);
                final View nonrecurringExpandedView = mNonrecurringView.findViewById(R.id.layout_expandableview);
                mNonrecurringDateRangeTextView = (TextView) mNonrecurringView.findViewById(R.id.textview_daterange);

                mRecurringView = (ViewGroup) view.findViewById(R.id.layout_recurring);
                mRecurringProgressBar = (TextRoundCornerProgressBar) mRecurringView.findViewById(R.id.rcprogress);
                mRecurringChart = (CombinedChart)mRecurringView.findViewById(R.id.chart);
                final View recurringExpandedView = mRecurringView.findViewById(R.id.layout_expandableview);
                mRecurringDateRangeTextView = (TextView) mRecurringView.findViewById(R.id.textview_daterange);

                mNonrecurringProgressBar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // toggle the chart
                        if(nonrecurringExpandedView.getVisibility() == View.GONE) {
                            nonrecurringExpandedView.setVisibility(View.VISIBLE);
                            mNonrecurringChart.animateXY(1000, 1000);
                        }else {
                            nonrecurringExpandedView.setVisibility(View.GONE);
                        }

                    }
                });


                mRecurringProgressBar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // toggle the chart
                        if(recurringExpandedView.getVisibility() == View.GONE) {
                            recurringExpandedView.setVisibility(View.VISIBLE);
                            mRecurringChart.animateXY(1000, 1000);
                        }else {
                            recurringExpandedView.setVisibility(View.GONE);
                        }

                    }
                });
            }

            void refreshNonRecurringChart() {
                // ---------  Add data -------------------
                CombinedData data = mItem.getNonrecurringChartData();
                if(data!=null) {
                    data.setHighlightEnabled(false);
                    XAxis xAxis = mNonrecurringChart.getXAxis();
                    xAxis.resetAxisMaxValue(); // reset the axis first so it can be calculated from the data
                    xAxis.resetAxisMinValue();
                    xAxis.setCenterAxisLabels(true);

                    // setup axis
                    xAxis.setValueFormatter(mItem.getNonrecurringXFormatter());
                    xAxis.setGranularity(1f);
                    xAxis.setDrawGridLines(true);
                    xAxis.setTextColor(Color.BLACK);


                    YAxis yAxis = mNonrecurringChart.getAxisLeft();
                    yAxis.setGranularity(1f);
                    yAxis.setAxisMinValue(0);
                    yAxis.setDrawGridLines(false);

                    yAxis.removeAllLimitLines();
                    LimitLine ll = new LimitLine(mItem.getTarget(), "Target");
                    yAxis.addLimitLine(ll);
                    yAxis.setAxisMaxValue(Math.max(data.getYMax(), 1.1f*mItem.getTarget()));

                    yAxis = mNonrecurringChart.getAxisRight();
                    yAxis.setGranularity(1f);
                    yAxis.setAxisMinValue(0);
                    yAxis.setDrawGridLines(false);
                    yAxis.setValueFormatter(mItem.getYFormatter());


                    mNonrecurringChart.setDescription("");
                    mNonrecurringChart.getLegend().setEnabled(false);
                    mNonrecurringChart.setData(data);
                    mNonrecurringChart.animateXY(1000, 1000);
                }
            }

            void refreshRecurringChart() {
                // ---------  Add data -------------------
                CombinedData data = mItem.getRecurringChartData();
                if(data!=null) {
                    data.setHighlightEnabled(false);
                    XAxis xAxis = mRecurringChart.getXAxis();
                    xAxis.resetAxisMaxValue(); // reset the axis first so it can be calculated from the data
                    xAxis.resetAxisMinValue();
                    xAxis.setCenterAxisLabels(true);

                    // setup axis
                    xAxis.setValueFormatter(mItem.getRecurringXFormatter());
                    xAxis.setGranularity(1f);
                    xAxis.setDrawGridLines(true);
                    xAxis.setTextColor(Color.BLACK);


                    YAxis yAxis = mRecurringChart.getAxisLeft();
                    yAxis.setGranularity(1f);
                    yAxis.setAxisMinValue(0);
                    yAxis.setDrawGridLines(false);

                    yAxis.removeAllLimitLines();
                    LimitLine ll = new LimitLine(mItem.getTarget(), "Target");
                    yAxis.addLimitLine(ll);
                    yAxis.setAxisMaxValue(Math.max(data.getYMax(), 1.1f*mItem.getTarget()));

                    mRecurringChart.getAxisRight().setEnabled(false);
                    mRecurringChart.setDescription("");
                    mRecurringChart.getLegend().setEnabled(false);
                    mRecurringChart.setData(data);
                    mRecurringChart.animateXY(1000, 1000);
                }
            }

            public void refreshRecurringProgressBar() {
                mRecurringProgressBar.setMax(1);
                float percent = mItem.getRecurringPercent();
                mRecurringProgressBar.setProgress(percent);
                mRecurringProgressBar.setProgressText(String.format("%d%%", (int)(percent*100)));
                mRecurringProgressBar.invalidate();

            }

            public void refreshRecurringDateRangeText() {
                Pair<ZonedDateTime,ZonedDateTime> dr = mItem.getDateRange(true);
                if(dr == null) {
                    mRecurringDateRangeTextView.setVisibility(View.GONE);
                }else {
                    mRecurringDateRangeTextView.setText(
                            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(dr.first) + " to " +
                                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(dr.second));
                }
            }

            public void refreshNonrecurringDateText() {
                Pair<ZonedDateTime,ZonedDateTime> dr = mItem.getDateRange(false);
                if(dr == null) {
                    mNonrecurringDateRangeTextView.setVisibility(View.GONE); // gym range not applicable
                }else {
                    mNonrecurringDateRangeTextView.setText(
                            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(dr.first) + " to " +
                                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(dr.second));
                }
            }

            public void refreshNonrecurringProgressBar() {
                mNonrecurringProgressBar.setMax(mItem.getTarget());
                int value = mItem.getNonrecurringProgress();
                mNonrecurringProgressBar.setProgress(value);
                mNonrecurringProgressBar.setProgressText(Integer.toString(value));
                mNonrecurringProgressBar.invalidate();
            }
        }
    }

    private void showEditGoalDialog(String selectedGoalUUID) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        Shared.ClimbType type = Shared.ClimbType.bouldering; //TODO: if filtering by climb type, use that climbtype in editclimb
        DialogFragment newFragment = EditGoalDialogFragment.newInstance(type, selectedGoalUUID);
        newFragment.show(ft, "dialog");
    }

}
