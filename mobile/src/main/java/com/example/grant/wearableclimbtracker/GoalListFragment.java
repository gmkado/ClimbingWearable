package com.example.grant.wearableclimbtracker;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.akexorcist.roundcornerprogressbar.IconRoundCornerProgressBar;
import com.akexorcist.roundcornerprogressbar.TextRoundCornerProgressBar;
import com.example.mysynclibrary.eventbus.EditGoalDialogEvent;
import com.example.mysynclibrary.goalDAO.CustomGoalDAO;
import com.example.mysynclibrary.goalDAO.GoalDAO;
import com.example.mysynclibrary.goalDAO.ProjectGoalDAO;
import com.example.mysynclibrary.realm.Goal;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CombinedData;

import org.greenrobot.eventbus.EventBus;
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
        View view = inflater.inflate(R.layout.fragment_goaldao_list, container, false);

        mRealm = Realm.getDefaultInstance();
        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            final RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));

            mResult = mRealm.where(Goal.class).findAll();
            // Add changelistener so if we add a goal, this view updates
            mResult.addChangeListener(new RealmChangeListener<RealmResults<Goal>>() {
                @Override
                public void onChange(RealmResults<Goal> element) {
                    recyclerView.setAdapter(new MyGoalRecyclerViewAdapter(getGoalList(element)));
                }
            });
            recyclerView.setAdapter(new MyGoalRecyclerViewAdapter(getGoalList(mResult)));
        }

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        mResult.removeAllChangeListeners();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRealm.close();
    }

    private List<GoalDAO> getGoalList(RealmResults<Goal> result) {
        ArrayList<GoalDAO> goallist = new ArrayList<>();
        //goallist.add(new GoalOfGoalsDAO());  TODO: this seems difficult to implement
        goallist.add(new ProjectGoalDAO());

        for(Goal goal:result) {
            goallist.add(new CustomGoalDAO(goal));
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
    public static class MyGoalRecyclerViewAdapter extends RecyclerView.Adapter<MyGoalRecyclerViewAdapter.ViewHolder> {

        private final List<GoalDAO> mValues;

        public MyGoalRecyclerViewAdapter(List<GoalDAO> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_goaldao, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = mValues.get(position);

            holder.mTitleTextView.setText(holder.mItem.getSummary());
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

                view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        GoalDAO goalDAO = mValues.get(getAdapterPosition());

                        // Create and show the dialog.
                        if(goalDAO.getType().equals(CustomGoalDAO.TYPE)) {
                            EventBus.getDefault().post(new EditGoalDialogEvent(((CustomGoalDAO) goalDAO).getID()));
                        }
                        return true;
                    }
                });

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
                    mNonrecurringDateRangeTextView.setVisibility(View.GONE); // date range not applicable
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
}
