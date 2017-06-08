package com.example.grant.wearableclimbtracker;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.akexorcist.roundcornerprogressbar.IconRoundCornerProgressBar;
import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;
import com.example.mysynclibrary.goalDAO.CustomGoalDAO;
import com.example.mysynclibrary.goalDAO.GoalDAO;
import com.example.mysynclibrary.goalDAO.ProjectGoalDAO;
import com.example.mysynclibrary.realm.Goal;

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

            holder.mCurrentProgressBar.setProgressColor(Color.parseColor("#56d2c2"));
            holder.mCurrentProgressBar.setProgressBackgroundColor(Color.parseColor("#757575"));
            holder.mCurrentProgressBar.setIconBackgroundColor(Color.parseColor("#38c0ae"));
            holder.mCurrentProgressBar.setMax(holder.mItem.getTarget());
            holder.mCurrentProgressBar.setProgress(holder.mItem.getCurrentProgress());
            holder.mCurrentProgressBar.setIconImageResource(android.R.drawable.btn_plus);
            //holder.mTitleTextView.setText(mValues.get(position).id);
            //holder.mCurrentProgressBar.setText(mValues.get(position).content);

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: open goal view
                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mTitleTextView;
            public final IconRoundCornerProgressBar mCurrentProgressBar;
            public GoalDAO mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mTitleTextView = (TextView) view.findViewById(R.id.textview_title);
                mCurrentProgressBar = (IconRoundCornerProgressBar) view.findViewById(R.id.rcprogress_current);
            }
        }
    }
}
