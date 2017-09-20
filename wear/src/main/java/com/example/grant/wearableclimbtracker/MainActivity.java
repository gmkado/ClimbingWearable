package com.example.grant.wearableclimbtracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.WearableRecyclerView;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.example.mysynclibrary.SyncHelper;
import com.example.mysynclibrary.eventbus.RealmSyncEvent;
import com.example.mysynclibrary.realm.Area;
import com.example.mysynclibrary.realm.AreaFields;
import com.example.mysynclibrary.realm.Attempt;
import com.example.mysynclibrary.realm.AttemptFields;
import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.realm.ClimbFields;
import com.example.mysynclibrary.realm.Gym;
import com.example.mysynclibrary.realm.GymFields;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.UUID;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;
import io.realm.Sort;

import static com.example.mysynclibrary.realm.ISyncableRealmObject.SyncState.DELETE;

public class MainActivity extends WearableActivity implements WearableActionDrawer.OnMenuItemClickListener, SharedPreferences.OnSharedPreferenceChangeListener {


    private static final String TAG = "MainActivity";
    public static final String PREF_FILTER_CLIMBTYPE = "prefClimbType";
    public static final String PREF_FILTER_GYM_ID = "pref_filter_gym_val";
    public static final String PREF_FILTER_AREA_ID = "pref_filter_area_val";

    private static final String PREFS_NAME = "mySharedPreferences";
    private static final int LOCATION_PREF_REQUEST = 0;

    private Climb.ClimbType mClimbType;
    private WearableDrawerLayout mDrawerLayout;
    private WearableNavigationDrawer mNavigationDrawer;
    private WearableActionDrawer mActionDrawer;
    private Realm mRealm;
    private GestureDetectorCompat mDetector;
    private SyncHelper.ClientSide mClientHelper;
    private WearableRecyclerView mListView;
    private Section mLastEmbeddedSection = null;
    private TextView mTitleView;

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
                startActivity(intent);

                mDrawerLayout.closeDrawer(Gravity.BOTTOM);
                return true;
            case R.id.delete_last:
                results = mRealm.where(Climb.class)
                        .equalTo(ClimbFields.TYPE, mClimbType.ordinal())
                        .equalTo(ClimbFields.SYNC_STATE, DELETE.name())
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
                                    results.last().safedelete(false);
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

        mClimbType = Climb.ClimbType.values()[
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(PREF_FILTER_CLIMBTYPE, Climb.ClimbType.bouldering.ordinal())]; // TODO: need to allow switching this somewhere...

        // create the gridviewpager
        /*mGridViewPager = (GridViewPager)findViewById(R.id.pager);

        mContentPagerAdapter = new ContentPagerAdapter(getFragmentManager());

        mGridViewPager.setAdapter(mContentPagerAdapter);*/

        mDrawerLayout = (WearableDrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationDrawer = (WearableNavigationDrawer) findViewById(R.id.top_navigation_drawer);
        mActionDrawer = (WearableActionDrawer) findViewById(R.id.bottom_action_drawer);

        mNavigationDrawer.setAdapter(new NavigationAdapter(this));


        Menu menu = mActionDrawer.getMenu();
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_drawer_menu, menu);
        mActionDrawer.setOnMenuItemClickListener(this);


        // get the realm instance
        mRealm = Realm.getDefaultInstance();
        mClientHelper = new SyncHelper(this).new ClientSide();

        mListView = (WearableRecyclerView) findViewById(R.id.listView);
        mTitleView = (TextView)findViewById(R.id.textview_title);
        // get the screen width
        /*DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        // set height of framelayout to be slightly larger than view
        mListView.setMinimumHeight(metrics.heightPixels+1);*/

        setDelayedViewVisible(false);
        mNavigationDrawer.setCurrentItem(0, false);
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

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        mClientHelper.disconnect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        mClientHelper.connect();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch(key) {
            case Shared.KEY_WEAR_ENABLED:
                updateLayoutBasedOnPreference();
                break;

        }
    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onRealmSyncEvent(RealmSyncEvent event) {
        switch(event.step) {
            case SYNC_REQUESTED:
                mClientHelper.sendRealmDb();
                break;
            case REMOTE_SAVED_TO_TEMP:
                mClientHelper.overwriteLocalWithRemote();
                break;
            case REALM_DB_MERGED:
                break;
        }
        EventBus.getDefault().removeStickyEvent(event);

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

    public enum Section {
        History(R.string.history_title, R.drawable.ic_history_light),
        Goals(R.string.goal_title, R.drawable.ic_goal_light),
        Project(R.string.project_title, R.drawable.ic_project_light),
        Location(R.string.location_title, R.drawable.ic_location_light),
        Settings(R.string.settings_title, R.drawable.ic_settings_light);

        final int titleRes;
        final int drawableRes;

        Section(final int titleRes, final int drawableRes) {
            this.titleRes = titleRes;
            this.drawableRes = drawableRes;

        }
    }

    private class NavigationAdapter extends WearableNavigationDrawer.WearableNavigationDrawerAdapter {
        private Context mContext;
        private Section mCurrentSection;
        NavigationAdapter(Context context) {
            mContext = context;
        }

        @Override
        public String getItemText(int i) {
            return mContext.getString(Section.values()[i].titleRes);
        }

        @Override
        public Drawable getItemDrawable(int i) {
            return mContext.getDrawable(Section.values()[i].drawableRes);
        }

        @Override
        public void onItemSelected(int i) {
            Section selectedSection = Section.values()[i];
            if (mCurrentSection == selectedSection) {
                return;
            }
            mCurrentSection = selectedSection;

            switch(selectedSection) {
                case History:
                    RealmQuery<Climb> query = mRealm.where(Climb.class)
                            .equalTo(ClimbFields.TYPE, mClimbType.ordinal())
                            .notEqualTo(ClimbFields.SYNC_STATE, DELETE.name());

                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    String gymId = pref.getString(PREF_FILTER_GYM_ID, null);
                    if(gymId !=null) {
                        mTitleView.setVisibility(View.VISIBLE);
                        String location = mRealm.where(Gym.class).equalTo(GymFields.ID, gymId).findFirst().getName();
                        query.equalTo(ClimbFields.GYM.ID, gymId);

                        String areaId = pref.getString(PREF_FILTER_AREA_ID, null);
                        if(areaId !=null) {
                            query.equalTo(ClimbFields.AREA.ID, areaId);
                            location += ", "+ mRealm.where(Area.class).equalTo(AreaFields.ID, areaId).findFirst().getName();
                        }
                        mTitleView.setText(location);
                    }else {
                        mTitleView.setVisibility(View.GONE);
                    }

                    mListView.setAdapter(new ClimbListAdapter(query.findAllSorted(ClimbFields.CREATED_AT, Sort.DESCENDING)));

                    mLastEmbeddedSection = selectedSection;
                    break;
                case Goals:

                    mLastEmbeddedSection = selectedSection;
                    break;
                case Project:

                    mLastEmbeddedSection = selectedSection;
                    break;
                case Location:
                    Intent intent = new Intent(MainActivity.this, LocationPickerActivity.class);
                    startActivityForResult(intent, LOCATION_PREF_REQUEST);
                    return;
                case Settings:
                    // TODO: use https://github.com/denley/WearPreferenceActivity
                    return;
                default:
                    return;
            }
        }

        @Override
        public int getCount() {
            return Section.values().length;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == LOCATION_PREF_REQUEST) {
            if (resultCode == RESULT_OK) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();

                if (data.hasExtra(LocationPickerActivity.EXTRA_GYM_ID)) {
                    String gymId = data.getStringExtra(LocationPickerActivity.EXTRA_GYM_ID);
                    editor.putString(PREF_FILTER_GYM_ID, gymId);
                }
                // now try the area
                if (data.hasExtra(LocationPickerActivity.EXTRA_AREA_ID)) {
                    String areaId = data.getStringExtra(LocationPickerActivity.EXTRA_AREA_ID);
                    editor.putString(PREF_FILTER_AREA_ID, areaId);
                }

                editor.apply();
                // reload the last section that was open
                mNavigationDrawer.setCurrentItem(mLastEmbeddedSection.ordinal(), false); // THis will load the new gym/area if it was changed
            }
        }
    }

    private class ClimbListAdapter extends RealmRecyclerViewAdapter<Climb, ClimbListAdapter.ViewHolder> {
        ClimbListAdapter(OrderedRealmCollection<Climb> data) {
            super(data, true);
            setHasStableIds(true);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_climb, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public long getItemId(int index) {
            return UUID.fromString(getItem(index).getId()).getMostSignificantBits();
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Climb climb = getItem(position);
            holder.datetime.setText(SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(climb.getDateCreated()));
            TextDrawable drawable = TextDrawable.builder()
                    .beginConfig()
                    .textColor(Color.BLACK)
                    .useFont(Typeface.DEFAULT)
                    .fontSize(30) /* size in px */
                    .bold()
                    .endConfig()
                    .buildRound(climb.getType().grades.get(climb.getGrade()), climb.getColor());

            holder.grade.setImageDrawable(drawable);
            // sends and attempts
            holder.sends.setCompoundDrawablesWithIntrinsicBounds(Shared.getScaledDrawable(MainActivity.this, R.drawable.ic_check_light,20, 20), null, null, null);


            // attempts
            holder.attempts.setCompoundDrawablesWithIntrinsicBounds(Shared.getScaledDrawable(MainActivity.this, R.drawable.ic_x_light, 20, 20),
                    null, null, null);

            // get attempts for this climb
            RealmResults<Attempt> attempts = mRealm.where(Attempt.class).equalTo(AttemptFields.CLIMB.ID, climb.getId()).findAllSorted("date");


            //holder.leadSend.setVisibility(View.GONE);
            if(!attempts.isEmpty()) {
                holder.attemptsView.setVisibility(View.VISIBLE);
                int numSends = (int) attempts.where().equalTo(AttemptFields.IS_SEND, true).count();
                int numAttempts = attempts.where().notEqualTo(AttemptFields.IS_SEND, true).sum("count").intValue();

                // update status icon
                /*if (attempts.first().isSend()) {
                    // flashed
                    holder.flash.setVisibility(View.VISIBLE);
                }else {
                    holder.flash.setVisibility(View.GONE);
                }*/
                // update progress bar
                if (!attempts.last().isSend() && attempts.last().getProgress()!= 0) {
                    holder.progress.setVisibility(View.VISIBLE);
                    holder.progress.setText(String.format("%d%%", (int)(attempts.last().getProgress())));
                } else {
                    holder.progress.setVisibility(View.GONE);
                }

                /* TODO: deal with lead climbing later.  Should the icon indicate the climb is leadable or the send was onlead?
                if(climb.getType()!= Shared.ClimbType.bouldering &&
                        !attempts.where().equalTo(AttemptFields.IS_SEND, true).equalTo(AttemptFields.ON_LEAD, true).findAll().isEmpty()) {
                        viewHolder.leadSend.setVisibility(View.VISIBLE);
                    }

                }*/

                holder.sends.setText(Integer.toString(numSends));
                holder.attempts.setText(Integer.toString(numAttempts));
            }else {
                // no attempts, hide the entire bottom bar
                holder.attemptsView.setVisibility(View.GONE);
            }

        }

        class ViewHolder extends RecyclerView.ViewHolder{
            TextView progress;
            View mView;
            ImageView grade;
            TextView datetime;
            TextView sends;
            TextView attempts;
            //ImageView flash;
            View attemptsView;

            ViewHolder(View view) {
                super(view);
                mView = view;
                datetime = (TextView) view.findViewById(R.id.textview_datetime);
                grade = (ImageView) view.findViewById(R.id.image_view);
                sends = (TextView) view.findViewById(R.id.textView_sends);
                attempts = (TextView) view.findViewById(R.id.textView_attempts);
                //flash = (ImageView) view.findViewById(R.id.imageview_flash);
                progress = (TextView) view.findViewById(R.id.textView_progress);
                attemptsView = view.findViewById(R.id.layout_attempts);
            }

        }
    }

}
