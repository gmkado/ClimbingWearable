package com.example.grant.wearableclimbtracker;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.akexorcist.roundcornerprogressbar.TextRoundCornerProgressBar;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.goalDAO.GoalDAO;
import com.example.mysynclibrary.realm.Attempt;
import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.realm.Goal;
import com.github.clans.fab.FloatingActionButton;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Date;

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
            holder.mItem = mValues.get(position);
            holder.mTitleTextView.setText(holder.mItem.getGoal().getSummary());
            holder.mMenu.setVisibility(View.VISIBLE);
            // populate mMenu and add onclicklistener
            final String goalId = holder.mItem.getGoal().getUUID();
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
                                    showDialogFragment(EditGoalDialogFragment.newInstance(Shared.ClimbType.bouldering, holder.mItem.getGoal().getUUID()));
                                    break;
                                case R.id.item_delete:
                                    mRealm.executeTransactionAsync(new Realm.Transaction() {
                                               @Override
                                               public void execute(Realm realm) {
                                                   Goal goal = realm.where(Goal.class).equalTo("id", goalId).findFirst();
                                                   goal.deleteFromRealm(); // TODO: wearable sync need to check if onwear
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
            holder.mCurrPeriodProgressBar.setProgressColor(Color.parseColor("#56d2c2"));
            holder.mCurrPeriodProgressBar.setProgressBackgroundColor(Color.parseColor("#757575"));
            holder.refreshGoalView();

            holder.mItem.setGoalListener(new GoalDAO.GoalDAOListener() {

                @Override
                public void onGoalResultsChanged() {
                    holder.refreshGoalView();
                }
            });

            if(holder.mItem.getGoal().isRecurring()) {

            }else {
                holder.mPrevPeriodsView.setVisibility(View.GONE);
            }

            //holder.mTitleTextView.setText(mValues.get(position).id);
            //holder.mCurrPeriodProgressBar.setText(mValues.get(position).content);
        }


        @Override
        public int getItemCount() {
            return mValues.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final View mView;
            final TextView mTitleTextView;
            private ImageButton mMenu;
            final TextRoundCornerProgressBar mCurrPeriodProgressBar;
            private final LinearLayout mPrevPeriodsView;


            GoalDAO mItem;

            ViewHolder(final View view) {
                super(view);
                mView = view;
                mTitleTextView = (TextView) view.findViewById(R.id.textview_title);
                mMenu = (ImageButton) view.findViewById(R.id.imagebutton_menu);
                mCurrPeriodProgressBar = (TextRoundCornerProgressBar) view.findViewById(R.id.rcprogress_currperiod);
                mPrevPeriodsView = (LinearLayout) view.findViewById(R.id.layout_prev_periods);
            }

            public void refreshProgressBar(int progress) {
                mCurrPeriodProgressBar.setMax(mItem.getGoal().getTarget());
                mCurrPeriodProgressBar.setProgress(progress);
                mCurrPeriodProgressBar.setProgressText(Integer.toString(progress));
                mCurrPeriodProgressBar.invalidate();
            }

            public void refreshGoalView() {
                // clear the linear layout
                mPrevPeriodsView.removeAllViews();
                if(mItem.getGoal().isRecurring()) {
                    mPrevPeriodsView.setVisibility(View.VISIBLE);
                    /* TODO: fit this in here instead of GoalDAO, show only prev N periods if NEVER
                    // if recurring, start date = N periods prior to today (7 days, 4 weeks, 12 months, 4 years), or start date, whichever is later
                    if (mGoal.isRecurring()) {
                        int incr;
                        switch (mGoal.getPeriod()) {
                            case SESSION:
                                incr = -7;
                                break;
                            case WEEKLY:
                                incr = -4;
                                break;
                            case MONTHLY:
                                incr = -12;
                                break;
                            case YEARLY:
                                incr = -4;
                                break;
                            default:
                                throw new IllegalArgumentException("Illegal period type found");
                        }
                        Date earliestStart = getIncrementedDate(endDate, mGoal.getPeriod().unit, incr);
                        startDate = earliestStart.before(startDate) ? startDate : earliestStart;  // Choose whatever comes later
                    } */
                    for(Map.Entry<Date, RealmResults<Attempt>> entry: mItem.getPeriodResultMap().entrySet()) {
                        // for each entry, add a bar to linear layout
                        Date periodDate = entry.getKey();

                        int progress = mItem.getProgressForPeriod(entry.getValue());
                        float periodPercent = progress*1f/mItem.getGoal().getTarget();
                        TextView tv = getPeriodView(periodPercent);
                        tv.setText(SimpleDateFormat.getDateInstance(DateFormat.SHORT).format(periodDate)); // TODO: format date depending on goal
                        mPrevPeriodsView.addView(tv);

                        // if today is in the daterange, refreshProgressbar with this periods progress
                        Date today = Calendar.getInstance().getTime();
                        if(today.after(periodDate) && today.before(GoalDAO.getIncrementedDate(periodDate, mItem.getGoal().getPeriod().unit, 1))) {
                            refreshProgressBar(progress);
                        }
                    }
                    mPrevPeriodsView.invalidate();
                }else {
                    mPrevPeriodsView.setVisibility(View.GONE);
                    refreshProgressBar(mItem.getProgressForPeriod(mItem.getFullRangeResults()));
                }
            }

            private TextView getPeriodView(float periodPercent) {
                /*<TextView
                android:id="@+id/textView8"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_margin="5dp"
                android:layout_weight="1"
                android:background="@drawable/round"/>*/
                periodPercent = periodPercent<0?0:periodPercent;
                periodPercent = periodPercent>1?1:periodPercent;

                TextView periodView = new TextView(getContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        TableLayout.LayoutParams.WRAP_CONTENT,
                        TableLayout.LayoutParams.WRAP_CONTENT, 1f);
                params.setMargins(5,5,5,5);
                periodView.setLayoutParams(params);
                periodView.setBackgroundResource(R.drawable.round);

                GradientDrawable drawable = (GradientDrawable) periodView.getBackground();

                // set the color as a function of period percent
                int baseColor = Color.RED;
                int adjustedColor = Color.argb((int)(periodPercent*255), Color.red(baseColor), Color.green(baseColor),Color.blue(baseColor));
                drawable.setColor(adjustedColor);
                return periodView;
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
