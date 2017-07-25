package com.example.grant.wearableclimbtracker;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.akexorcist.roundcornerprogressbar.TextRoundCornerProgressBar;
import com.example.grant.wearableclimbtracker.FilterClimbDialogFragment.SortByField;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.realm.Attempt;
import com.example.mysynclibrary.realm.Climb;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.polyak.iconswitch.IconSwitch;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

import static com.example.grant.wearableclimbtracker.FilterClimbDialogFragment.PREF_FILTER_AREA;
import static com.example.grant.wearableclimbtracker.FilterClimbDialogFragment.PREF_FILTER_AREA_VAL;
import static com.example.grant.wearableclimbtracker.FilterClimbDialogFragment.PREF_FILTER_GYM;
import static com.example.grant.wearableclimbtracker.FilterClimbDialogFragment.PREF_FILTER_GYM_VAL;
import static com.example.grant.wearableclimbtracker.FilterClimbDialogFragment.PREF_FILTER_PROJECTS;
import static com.example.grant.wearableclimbtracker.FilterClimbDialogFragment.PREF_FILTER_SET;
import static com.example.grant.wearableclimbtracker.FilterClimbDialogFragment.PREF_SORT_BY;

/**
 * Created by Grant on 10/17/2016.
 */
public class ClimbListMobileFragment extends Fragment {
    private final String TAG = "ClimbListMobileFragment";
    private ListView mListView;
    private ClimbListAdapter mAdapter;
    private RealmResults<Climb> mResult;
    private Realm mRealm;
    private final String PREF_CLIMBTYPE = "prefClimbType";
    private Shared.ClimbType mClimbType;

    public ClimbListMobileFragment() {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.item_filter:
                // show the filter dialog
                FilterClimbDialogFragment fragment = FilterClimbDialogFragment.newInstance();
                fragment.setOnDialogClosedListener(new FilterClimbDialogFragment.OnDialogClosedListener() {
                    @Override
                    public void onClose() {
                        invalidateRealmResult();  // invalidate to include new filter/sort requirments
                    }
                });
                showDialogFragment(fragment);

                return true;
        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.climblist_menu,menu);

        /* ********************* Climbtype *************************/
        MenuItem item = menu.findItem(R.id.item_switch_climbtype);
        View view = item.getActionView();
        IconSwitch typeSwitch = (IconSwitch) view.findViewById(R.id.switch_climbtype);
        // set to last checked
        typeSwitch.setChecked(mClimbType == Shared.ClimbType.bouldering? IconSwitch.Checked.LEFT: IconSwitch.Checked.RIGHT);
        typeSwitch.setCheckedChangeListener(new IconSwitch.CheckedChangeListener() {
            @Override
            public void onCheckChanged(IconSwitch.Checked current) {
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                if(current== IconSwitch.Checked.LEFT) {
                    mClimbType = Shared.ClimbType.bouldering;
                    pref.edit().putInt(PREF_CLIMBTYPE, Shared.ClimbType.bouldering.ordinal()).apply();
                }else {
                    mClimbType = Shared.ClimbType.ropes;
                    pref.edit().putInt(PREF_CLIMBTYPE, Shared.ClimbType.ropes.ordinal()).apply();
                }
                invalidateRealmResult();
            }
        });
    }

    public static ClimbListMobileFragment newInstance() {
        ClimbListMobileFragment fragment = new ClimbListMobileFragment();

        return fragment;
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mClimbType = Shared.ClimbType.values()[pref.getInt(PREF_CLIMBTYPE, Shared.ClimbType.bouldering.ordinal())];

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_climb_list, container, false);

        // Add fab listeners
        final FloatingActionMenu fabMenu = (FloatingActionMenu) rootView.findViewById(R.id.fab_menu);
        FloatingActionButton fabAddProject = (FloatingActionButton) rootView.findViewById(R.id.fab_add_project);
        fabAddProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create and show the dialog.
                showDialogFragment(EditClimbDialogFragment.newInstance(mClimbType, null, EditClimbDialogFragment.EditClimbMode.ADD_PROJECT));
            }
        });
        FloatingActionButton fabAddSend = (FloatingActionButton) rootView.findViewById(R.id.fab_add_send);
        fabAddSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogFragment(EditClimbDialogFragment.newInstance(mClimbType, null, EditClimbDialogFragment.EditClimbMode.ADD_SEND));
            }
        });

        mListView = (ListView)rootView.findViewById(R.id.list);
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            private int mLastFirstVisibleItem;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if(mLastFirstVisibleItem<firstVisibleItem)
                {
                    fabMenu.setVisibility(View.GONE);
                }
                if(mLastFirstVisibleItem>firstVisibleItem)
                {
                    fabMenu.setVisibility(View.VISIBLE);
                }
                mLastFirstVisibleItem=firstVisibleItem;

            }
        });

        // populate the listview
        mRealm = Realm.getDefaultInstance();
        invalidateRealmResult();

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    private void invalidateRealmResult() {
        // get filter results
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean filterGym = pref.getBoolean(PREF_FILTER_GYM, false);
        String filterGymVal = pref.getString(PREF_FILTER_GYM_VAL, null);
        boolean filterArea = pref.getBoolean(PREF_FILTER_AREA, false);
        String filterAreaVal = pref.getString(PREF_FILTER_AREA_VAL, null);
        boolean filterProjects = pref.getBoolean(PREF_FILTER_PROJECTS, false);
        boolean filterSet = pref.getBoolean(PREF_FILTER_SET, false);

        RealmQuery<Climb> query = mRealm.where(Climb.class).equalTo("type", mClimbType.ordinal());
        if(filterGym) {
            query.equalTo("gym", filterGymVal);
        }

        if(filterArea) {
            query.equalTo("area", filterAreaVal);
        }
        if(filterProjects) {
            query.beginGroup()
                    .equalTo("attempts.isSend", false).or().isEmpty("attempts")
                    .endGroup();
        }
        if(filterSet) {
            query.equalTo("isRemoved", false);
        }

        mResult = query.findAll();
        mResult.addChangeListener(new RealmChangeListener<RealmResults<Climb>>() {
            @Override
            public void onChange(RealmResults<Climb> climbs) {
                sortResultAndUpdateAdapter(climbs);
            }
        });
        sortResultAndUpdateAdapter(mResult);


    }

    private void sortResultAndUpdateAdapter(RealmResults<Climb> realmResults) {
        // TODO: had to change ClimbListAdapter to an array adapter instead of RealmBaseAdapter, janky way to do this, this is tracked here: https://github.com/realm/realm-java/issues/4501
        // TODO: also here https://github.com/realm/realm-java/issues/2313
        // Need to make sure that we call this when mResult is updated, since list adapter is no longer automatically watching for changes
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SortByField sortByField = SortByField.values()[pref.getInt(PREF_SORT_BY, SortByField.lastadded.ordinal())];

        ArrayList<Climb> sortedResults = new ArrayList<>();
        switch(sortByField) {
            case lastadded:
                sortedResults.addAll(mRealm.copyFromRealm(realmResults.sort("createdAt", Sort.DESCENDING)));
                break;
            case lastclimbed:
                sortedResults.addAll(mRealm.copyFromRealm(realmResults));
                Collections.sort(sortedResults, new Comparator<Climb>() {
                    @Override
                    public int compare(Climb o1, Climb o2) {
                        /*a negative integer, zero, or a positive integer as the
                                *         first argument is less than, equal to, or greater than the
                                *         second.*/
                        // first see if they even have attempts
                        Attempt o1Attempt = mRealm.where(Attempt.class).equalTo("climb.id", o1.getId()).findAllSorted("datetime", Sort.ASCENDING).last(null);
                        Attempt o2Attempt = mRealm.where(Attempt.class).equalTo("climb.id", o2.getId()).findAllSorted("datetime", Sort.ASCENDING).last(null);

                        Date minDate = new Date(Long.MIN_VALUE);
                        Date o1LastClimbDate = o1Attempt == null? minDate:o1Attempt.getDatetime();
                        Date o2LastClimbDate = o2Attempt == null? minDate:o2Attempt.getDatetime();
                        return o2LastClimbDate.compareTo(o1LastClimbDate);
                    }
                });
                break;
            case mostclimbed:
                sortedResults.addAll(mRealm.copyFromRealm(realmResults));
                Collections.sort(sortedResults, new Comparator<Climb>() {
                    @Override
                    public int compare(Climb o1, Climb o2) {
                        /*a negative integer, zero, or a positive integer as the
                                *         first argument is less than, equal to, or greater than the
                                *         second.*/
                        // first see if they even have attempts
                        int o1Attempts = mRealm.where(Attempt.class).equalTo("climb.id", o1.getId()).sum("count").intValue();
                        int o2Attempts = mRealm.where(Attempt.class).equalTo("climb.id", o2.getId()).sum("count").intValue();
                        return Integer.compare(o2Attempts, o1Attempts);
                    }
                });
                break;
            case progress:
                sortedResults.addAll(mRealm.copyFromRealm(realmResults));
                Collections.sort(sortedResults, new Comparator<Climb>() {
                    @Override
                    public int compare(Climb o1, Climb o2) {
                        /*a negative integer, zero, or a positive integer as the
                                *         first argument is less than, equal to, or greater than the
                                *         second.*/
                        // get progress of last attempt
                        Attempt o1Attempt = mRealm.where(Attempt.class).equalTo("climb.id", o1.getId()).findAllSorted("datetime", Sort.ASCENDING).last(null);
                        Attempt o2Attempt = mRealm.where(Attempt.class).equalTo("climb.id", o2.getId()).findAllSorted("datetime", Sort.ASCENDING).last(null);

                        float o1Progress = o1Attempt == null? 0f:o1Attempt.isSend()? 0f:o1Attempt.getProgress();
                        float o2Progress = o2Attempt == null? 0f:o2Attempt.isSend()? 0f:o2Attempt.getProgress();

                        return Float.compare(o2Progress, o1Progress);
                    }
                });
                break;
            case grade_asc:
                sortedResults.addAll(mRealm.copyFromRealm(realmResults.sort("grade", Sort.ASCENDING)));
                break;
            case grade_desc:
                sortedResults.addAll(mRealm.copyFromRealm(realmResults.sort("grade", Sort.DESCENDING)));
                break;
        }
        mAdapter = new ClimbListAdapter(sortedResults);
        mListView.setAdapter(mAdapter);
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }

    @Override
    public void onStop() {
        super.onStop();
        //EventBus.getDefault().unregister(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        //EventBus.getDefault().register(this);
    }



    private class ClimbListAdapter extends ArrayAdapter<Climb> implements ListAdapter {
        private static final int MENU_EDIT = 0;
        private static final int MENU_SEND = 1;
        private static final int MENU_ATTEMPT = 2;
        private static final int MENU_REMOVE = 3;

        private class ViewHolder {
            TextView grade;
            TextView gym;
            TextView area;
            TextView attempts;
            TextView sends;
            TextView dateAttempted;
            TextView dateCreated;
            View card;
            ImageButton menu;
            ImageView status;
            TextRoundCornerProgressBar progress;
            View expandable;
        }

        ClimbListAdapter(ArrayList<Climb> data) {
            super(ClimbListMobileFragment.this.getContext(), 0 , data);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            final ViewHolder viewHolder;
            if(convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_climb_mobile, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.card = convertView.findViewById(R.id.card);
                viewHolder.grade = (TextView) convertView.findViewById(R.id.textview_grade);
                viewHolder.gym = (TextView) convertView.findViewById(R.id.textview_gym);
                viewHolder.area = (TextView) convertView.findViewById(R.id.textview_area);
                viewHolder.sends = (TextView) convertView.findViewById(R.id.textview_sends);
                viewHolder.dateAttempted = (TextView) convertView.findViewById(R.id.textview_lastAttempt);
                viewHolder.dateCreated = (TextView) convertView.findViewById(R.id.textview_createdAt);
                viewHolder.attempts = (TextView) convertView.findViewById(R.id.textview_attempts);
                viewHolder.menu = (ImageButton) convertView.findViewById(R.id.imagebutton_menu);
                viewHolder.expandable = convertView.findViewById(R.id.layout_climbchart);
                viewHolder.progress = (TextRoundCornerProgressBar) viewHolder.expandable.findViewById(R.id.rcprogress);
                viewHolder.progress.setMax(100);
                viewHolder.status = (ImageView) convertView.findViewById(R.id.imageView_status);

                convertView.setTag(viewHolder);
            }else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            final Climb unmanagedClimb = getItem(position);

            int gradeInd = unmanagedClimb.getGrade();
            Shared.ClimbType type = unmanagedClimb.getType();

            viewHolder.grade.setText(type.grades.get(gradeInd));
            if(unmanagedClimb.isRemoved()) {
                 viewHolder.grade.setPaintFlags(viewHolder.grade.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }else {
                viewHolder.grade.setPaintFlags(viewHolder.grade.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            }
            viewHolder.gym.setText(unmanagedClimb.getGym()==null?"":unmanagedClimb.getGym());
            viewHolder.area.setText(unmanagedClimb.getArea()==null? "" : unmanagedClimb.getArea());
            viewHolder.dateCreated.setText("Created: "+ SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(unmanagedClimb.getDateCreated()));

            if (unmanagedClimb.getColor() != -1) {
                viewHolder.card.setBackgroundColor(
                        ColorUtils.setAlphaComponent(unmanagedClimb.getColor(), (int) (0.5*255)));
            }else {
                viewHolder.card.setBackgroundColor(Color.WHITE);
            }

            // populate menu and add onclicklistener
            viewHolder.menu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu menu = new PopupMenu (getContext(), viewHolder.menu);
                    menu.setOnMenuItemClickListener (new PopupMenu.OnMenuItemClickListener ()
                    {

                        @Override
                        public boolean onMenuItemClick (MenuItem item)
                        {
                            int id = item.getItemId();
                            switch (id)
                            {
                                case MENU_EDIT:
                                    showDialogFragment(EditClimbDialogFragment.newInstance(mClimbType, unmanagedClimb.getId(), EditClimbDialogFragment.EditClimbMode.EDIT));
                                    break;
                                case MENU_ATTEMPT:
                                    FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                                    Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag("dialog");
                                    if (prev != null) {
                                        ft.remove(prev);
                                    }
                                    ft.addToBackStack(null);

                                    // Create and show the dialog.
                                    DialogFragment newFragment = EditAttemptsDialogFragment.newInstance(unmanagedClimb.getId(), null);
                                    newFragment.show(ft, "dialog");
                                    break;
                                case MENU_SEND:
                                    mRealm.executeTransactionAsync(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            Climb managedClimb = realm.where(Climb.class).equalTo("id", unmanagedClimb.getId()).findFirst();
                                            Attempt send = realm.createObject(Attempt.class,UUID.randomUUID().toString());
                                            send.setSend(true);
                                            send.setDate(Calendar.getInstance().getTime());
                                            send.setCount(1);
                                            send.setProgress(100);
                                            send.setClimb(managedClimb);
                                        }
                                    }, new Realm.Transaction.OnSuccess() {
                                        @Override
                                        public void onSuccess() {
                                            // Transaction was a success.
                                            invalidateRealmResult();
                                        }
                                    }, new Realm.Transaction.OnError() {
                                        @Override
                                        public void onError(Throwable error) {
                                            // Transaction failed and was automatically canceled.
                                            Log.e(TAG, error.getMessage());
                                        }
                                    });
                                    break;
                                case MENU_REMOVE:
                                    mRealm.executeTransactionAsync(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            Climb managedClimb = realm.where(Climb.class).equalTo("id", unmanagedClimb.getId()).findFirst();
                                            managedClimb.setRemoved(!managedClimb.isRemoved());
                                        }
                                    }, null, new Realm.Transaction.OnError() {
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
                    menu.getMenu().add(Menu.NONE, MENU_EDIT, 0, "Edit");
                    menu.getMenu().add(Menu.NONE, MENU_SEND, 2, "Add Send");
                    menu.getMenu().add(Menu.NONE, MENU_ATTEMPT, 3, "Add Attempt");
                    menu.getMenu().add(Menu.NONE, MENU_REMOVE, 4, unmanagedClimb.isRemoved()?"Set as not removed":"Set as removed");
                    menu.show();
                }
            });

            // get attempts for this climb
            RealmResults<Attempt> attempts = mRealm.where(Attempt.class).equalTo("climb.id", unmanagedClimb.getId()).findAllSorted("date");
            if(!attempts.isEmpty()) {
                viewHolder.dateAttempted.setVisibility(View.VISIBLE);
                viewHolder.dateAttempted.setText("Last Go: "+ SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(attempts.last().getDatetime()));
                int numSends = (int) attempts.where().equalTo("isSend", true).count();
                int numAttempts = attempts.where().notEqualTo("isSend", true).sum("count").intValue();

                if(numSends == 0) {
                    viewHolder.sends.setVisibility(View.INVISIBLE);
                }else {
                    viewHolder.sends.setVisibility(View.VISIBLE);
                    viewHolder.sends.setText(Integer.toString(numSends) + (numSends == 1?" send": " sends"));
                }
                if(numAttempts == 0) {
                    viewHolder.attempts.setVisibility(View.INVISIBLE);
                }else {
                    viewHolder.attempts.setVisibility(View.VISIBLE);
                    viewHolder.attempts.setText(Integer.toString(numAttempts)+ (numAttempts == 1? " attempt":" attempts"));
                }

                // update status icon
                if (attempts.first().isSend()) {
                    // flashed
                    viewHolder.status.setVisibility(View.VISIBLE);
                    viewHolder.status.setImageDrawable(getContext().getDrawable(R.drawable.flash));
                } else if(numSends > 0) {
                    viewHolder.status.setVisibility(View.VISIBLE);
                    viewHolder.status.setImageDrawable(getContext().getDrawable(R.drawable.checked));
                } else {
                    viewHolder.status.setVisibility(View.INVISIBLE);
                }

                // update progress bar
                if (attempts.last().isSend()) {
                    // last go was a send, so don't show progress
                    viewHolder.expandable.setVisibility(View.GONE);
                } else {
                    viewHolder.expandable.setVisibility(View.VISIBLE);
                    viewHolder.progress.setProgress(attempts.last().getProgress());
                }
            }else {
                viewHolder.status.setVisibility(View.GONE);
                viewHolder.expandable.setVisibility(View.GONE);
                viewHolder.sends.setVisibility(View.INVISIBLE);
                viewHolder.attempts.setVisibility(View.INVISIBLE);
                viewHolder.dateAttempted.setVisibility(View.INVISIBLE);
            }
            return convertView;
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
