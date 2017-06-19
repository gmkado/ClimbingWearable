package com.example.grant.wearableclimbtracker;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.akexorcist.roundcornerprogressbar.IconRoundCornerProgressBar;
import com.example.mysynclibrary.ClimbStats;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.eventbus.ChartEntrySelected;
import com.example.mysynclibrary.eventbus.EditGoalDialogEvent;
import com.example.mysynclibrary.goalDAO.CustomGoalDAO;
import com.example.mysynclibrary.goalDAO.GoalDAO;
import com.example.mysynclibrary.goalDAO.ProjectGoalDAO;
import com.example.mysynclibrary.realm.Goal;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.greenrobot.eventbus.EventBus;
import org.threeten.bp.temporal.ChronoUnit;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

import static com.example.mysynclibrary.ClimbStats.StatType.GRADE;

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
            holder.mNonrecurringProgressBar.setIconBackgroundColor(Color.parseColor("#38c0ae"));
            holder.mNonrecurringProgressBar.setMax(holder.mItem.getNonrecurringTarget());
            holder.mNonrecurringProgressBar.setProgress(holder.mItem.getNonrecurringProgress());
            holder.mNonrecurringProgressBar.invalidate();
            //holder.mNonrecurringProgressBar.setIconImageResource(android.R.drawable.btn_plus);

            // populate chart
            setupNonRecurringChart(holder.mNonrecurringChart, holder.mItem);

            if(holder.mItem.isRecurring()) {
                holder.mRecurringView.getLayoutTransition().setAnimateParentHierarchy(false); // this fixed weird animate overlap issues https://stackoverflow.com/questions/36064424/animate-layout-changes-broken-in-nested-layout-with-collapsingtoolbarlayout
                holder.mRecurringProgressBar.setProgressColor(Color.parseColor("#56d2c2"));
                holder.mRecurringProgressBar.setProgressBackgroundColor(Color.parseColor("#757575"));
                holder.mRecurringProgressBar.setIconBackgroundColor(Color.parseColor("#38c0ae"));
                holder.mRecurringProgressBar.setMax(1);
                holder.mRecurringProgressBar.setProgress(holder.mItem.getRecurringPercent());
                //holder.mNonrecurringProgressBar.setIconImageResource(android.R.drawable.btn_plus);
            }else {
                holder.mRecurringView.setVisibility(View.GONE);
            }

            //holder.mTitleTextView.setText(mValues.get(position).id);
            //holder.mNonrecurringProgressBar.setText(mValues.get(position).content);
        }

        private void setupNonRecurringChart(CombinedChart chart, GoalDAO goalDAO) {
            // ---------  Add data -------------------
            CombinedData data = goalDAO.getNonrecurringChartData();
            if(data!=null) {
                data.setHighlightEnabled(false);
                XAxis xAxis = chart.getXAxis();
                xAxis.resetAxisMaxValue(); // reset the axis first so it can be calculated from the data
                xAxis.resetAxisMinValue();
                xAxis.setCenterAxisLabels(true);

                // setup axis
                xAxis.setValueFormatter(goalDAO.getNonrecurringXFormatter());
                xAxis.setGranularity(1f);
                xAxis.setDrawGridLines(true);
                xAxis.setTextColor(Color.BLACK);


                YAxis yAxis = chart.getAxisLeft();
                yAxis.setGranularity(1f);
                yAxis.setAxisMinValue(0);
                yAxis.setDrawGridLines(false);
                yAxis.setValueFormatter(goalDAO.getNonrecurringYFormatter());

                yAxis.removeAllLimitLines();
                LimitLine ll = new LimitLine(goalDAO.getNonrecurringTarget(), "Target");
                yAxis.addLimitLine(ll);

                chart.setDescription("");
                chart.getLegend().setEnabled(false);
                chart.setData(data);
            }
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mTitleTextView;
            private final ViewGroup mNonrecurringView;
            public final IconRoundCornerProgressBar mNonrecurringProgressBar;
            private final CombinedChart mNonrecurringChart;
            private final IconRoundCornerProgressBar mRecurringProgressBar;
            private final CombinedChart mRecurringChart;
            private final ViewGroup mRecurringView;

            public GoalDAO mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mTitleTextView = (TextView) view.findViewById(R.id.textview_title);

                mNonrecurringView = (ViewGroup) view.findViewById(R.id.layout_nonrecurring);
                mNonrecurringProgressBar = (IconRoundCornerProgressBar) mNonrecurringView.findViewById(R.id.rcprogress);
                mNonrecurringChart = (CombinedChart)mNonrecurringView.findViewById(R.id.chart);

                mRecurringView = (ViewGroup) view.findViewById(R.id.layout_recurring);
                mRecurringProgressBar = (IconRoundCornerProgressBar) mRecurringView.findViewById(R.id.rcprogress);
                mRecurringChart = (CombinedChart)mRecurringView.findViewById(R.id.chart);
                view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        // Create and show the dialog.
                        GoalDAO goalDAO = mValues.get(getAdapterPosition());
                        if(goalDAO.getType() == CustomGoalDAO.TYPE) {
                            EventBus.getDefault().post(new EditGoalDialogEvent(((CustomGoalDAO) goalDAO).getID()));
                        }
                        return true;
                    }
                });

                mNonrecurringProgressBar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // toggle the chart
                        if(mNonrecurringChart.getVisibility() == View.GONE) {
                            mNonrecurringChart.setVisibility(View.VISIBLE);
                            mNonrecurringChart.animateXY(1000, 1000);
                        }else {
                            mNonrecurringChart.setVisibility(View.GONE);
                        }

                    }
                });
                mRecurringProgressBar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // toggle the chart
                        mRecurringChart.setVisibility(
                                mRecurringChart.getVisibility()==View.GONE?
                                        View.VISIBLE:View.GONE
                        );
                    }
                });
            }
        }
    }
}
