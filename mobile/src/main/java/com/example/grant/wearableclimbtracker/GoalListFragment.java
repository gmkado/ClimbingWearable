package com.example.grant.wearableclimbtracker;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.akexorcist.roundcornerprogressbar.IconRoundCornerProgressBar;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.goalDAO.GoalDAO;
import com.example.mysynclibrary.realm.Goal;
import com.example.mysynclibrary.realm.GoalFields;
import com.github.clans.fab.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

/**
 * A fragment representing a list of Items.
 */
public class GoalListFragment extends Fragment {
    private static final String TAG = "GoalListFragment";
    private Realm mRealm;
    private RealmResults<Goal> mResult;
    private RecyclerView mRecyclerView;

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
        View rootView = inflater.inflate(R.layout.fragment_goal_list, container, false);

        mRealm = Realm.getDefaultInstance();

        // Set the adapter
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(rootView.getContext()));

        mResult = mRealm.where(Goal.class).findAll();
        // Add changelistener so if we add a goal, this view updates
        mResult.addChangeListener(new RealmChangeListener<RealmResults<Goal>>() {
            @Override
            public void onChange(RealmResults<Goal> element) {
                mRecyclerView.setAdapter(new MyGoalRecyclerViewAdapter(getGoalList(element)));
            }
        });
        mRecyclerView.setAdapter(new MyGoalRecyclerViewAdapter(getGoalList(mResult)));

        FloatingActionButton fabAddProject = (FloatingActionButton) rootView.findViewById(R.id.fab_add_project);
        fabAddProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create and show the dialog.
                showDialogFragment(EditClimbDialogFragment.newInstance(Shared.ClimbType.bouldering, null, EditClimbDialogFragment.EditClimbMode.ADD_PROJECT));
            }
        });
        FloatingActionButton fabAddSend = (FloatingActionButton) rootView.findViewById(R.id.fab_add_send);
        fabAddSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogFragment(EditClimbDialogFragment.newInstance(Shared.ClimbType.bouldering, null, EditClimbDialogFragment.EditClimbMode.ADD_SEND));
            }
        });
        // add goal button listener
        FloatingActionButton fabAddGoal = (FloatingActionButton) rootView.findViewById(R.id.fab_add_goal);
        fabAddGoal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogFragment(EditGoalDialogFragment.newInstance(Shared.ClimbType.bouldering, null));
            }
        });
        return rootView;
    }

    /*
     * It is good practice to null the reference from the view to the adapter when it is no longer needed.
     * Because the <code>RealmRecyclerViewAdapter</code> registers itself as a <code>RealmResult.ChangeListener</code>
     * the view may still be reachable if anybody is still holding a reference to the <code>RealmResult>.
     * https://github.com/realm/realm-android-adapters/blob/master/example/src/main/java/io/realm/examples/adapters/ui/recyclerview/RecyclerViewExampleActivity.java
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(mResult!=null) {
            mResult.removeAllChangeListeners();
        }
        mRealm.close();

        mRecyclerView.setAdapter(null);
        /* TODO: uncomment this if setting adapter to null doesn't stop goalDAO listeners from firing
        // remove goalDAO listeners
        List<GoalDAO> list = ((MyGoalRecyclerViewAdapter) mRecyclerView.getAdapter()).getValues();
        for(GoalDAO goal: list) {
            goal.setGoalListener(null);
        }*/
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

        public List<GoalDAO> getValues() {
            return mValues;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.listitem_goal_mobile, parent, false);
            return new ViewHolder(view);
        }


        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mGoalDAO = mValues.get(position);
            holder.mSummaryTextView.setText(holder.mGoalDAO.getGoal().getSummary());
            // populate mMenu and add onclicklistener
            final String goalId = holder.mGoalDAO.getGoal().getId();
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
                                    showDialogFragment(EditGoalDialogFragment.newInstance(Shared.ClimbType.bouldering, holder.mGoalDAO.getGoal().getId()));
                                    break;
                                case R.id.item_delete:
                                    mRealm.executeTransactionAsync(new Realm.Transaction() {
                                               @Override
                                               public void execute(Realm realm) {
                                                   Goal goal = realm.where(Goal.class).equalTo(GoalFields.ID, goalId).findFirst();
                                                   goal.safeDelete();
                                               }
                                           }, null, // NOTE: since we changed a goal, the listener gets called which refreshes the list adapter, so we don't need to invalidate realm results
                                            new Realm.Transaction.OnError() {
                                                @Override
                                                public void onError(Throwable error) {
                                                    // Transaction failed and was automatically canceled.
                                                    Log.e(TAG, error.getMessage());
                                                }
                                            });
                                    break;
                            }
                            return true;
                        }
                    });
                    menu.inflate(R.menu.menu_goal_listitem);
                    menu.show();
                }
            });
            holder.mNameTextView.setText(holder.mGoalDAO.getGoal().getName());
            holder.mCurrProgressBar.setProgressColor(Color.parseColor("#56d2c2"));
            holder.mCurrProgressBar.setProgressBackgroundColor(Color.parseColor("#757575"));
            holder.mCurrProgressBar.setIconImageResource(holder.mGoalDAO.getGoal().getGoalUnit().getDrawableId());
            holder.refreshGoalView();

            holder.mGoalDAO.setGoalListener(new GoalDAO.GoalDAOListener() {

                @Override
                public void onGoalResultsChanged() {
                    holder.refreshGoalView();
                }
            });
        }


        @Override
        public int getItemCount() {
            return mValues.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View mView;
            TextView mSummaryTextView;
            TextView mNameTextView;
            TextView mCurrProgressTextView;
            TextView mPastProgressTextView;

            ImageButton mMenu;
            IconRoundCornerProgressBar mCurrProgressBar;
            GoalDAO mGoalDAO;


            ViewHolder(final View view) {
                super(view);
                mView = view;
                mSummaryTextView = (TextView) view.findViewById(R.id.textview_summary);
                mNameTextView = (TextView) view.findViewById(R.id.textview_name);
                mMenu = (ImageButton) view.findViewById(R.id.imagebutton_menu);
                mCurrProgressBar = (IconRoundCornerProgressBar) view.findViewById(R.id.rcprogress_currperiod);
                mPastProgressTextView = (TextView) view.findViewById(R.id.textview_pastprogress);
                mCurrProgressTextView = (TextView) view.findViewById(R.id.textview_currentprogress);
            }

            public void refreshProgressBar(int progress) {
                mCurrProgressBar.setMax(mGoalDAO.getGoal().getTarget());
                mCurrProgressBar.setProgress(progress);
                mCurrProgressBar.invalidate();
            }

            public void refreshGoalView() {
                if(mGoalDAO.getGoal().isRecurring()) {
                    mPastProgressTextView.setVisibility(View.VISIBLE);
                    mPastProgressTextView.setText(mGoalDAO.getPastProgressText());
                }else {
                    mPastProgressTextView.setVisibility(View.GONE);
                }

                mCurrProgressTextView.setText(mGoalDAO.getCurrentProgressText());
                refreshProgressBar(mGoalDAO.getCurrentProgress());
            }
        }
    }

    private void showDialogFragment(DialogFragment newFragment) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        newFragment.show(ft, "dialog");
    }
}
