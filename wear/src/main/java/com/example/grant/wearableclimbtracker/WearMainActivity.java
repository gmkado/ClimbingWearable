package com.example.grant.wearableclimbtracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WearableRecyclerView;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableDrawerView;
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

import com.example.mysynclibrary.login.WearUserManager;
import com.example.mysynclibrary.realm.Area;
import com.example.mysynclibrary.realm.AreaFields;
import com.example.mysynclibrary.realm.Attempt;
import com.example.mysynclibrary.realm.AttemptFields;
import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.realm.ClimbFields;
import com.example.mysynclibrary.realm.Gym;
import com.example.mysynclibrary.realm.GymFields;
import com.example.mysynclibrary.textdrawable.TextDrawable;

import org.greenrobot.eventbus.EventBus;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.UUID;

import io.realm.ObjectServerError;
import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.SyncUser;


public class WearMainActivity extends WearableActivity implements WearableActionDrawer.OnMenuItemClickListener, SharedPreferences.OnSharedPreferenceChangeListener {


    private static final String TAG = "WearMainActivity";

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

    private WearableRecyclerView mListView;
    private Section mLastEmbeddedSection = null;
    private TextView mTitleView;
    private TextView mNotEnabledTextView;

    public WearMainActivity() {
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        Log.d(TAG, "onMenuItemClick():" + menuItem);
        int itemId = menuItem.getItemId();
        final RealmResults<Climb> results;

        switch(itemId) {
            case R.id.add_climb:
                // start addclimbactivity
                startActivity(EditClimbActivity.newAddInstance(this));
                mDrawerLayout.closeDrawer(Gravity.BOTTOM);
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
        if(mRealm!=null) {
            mRealm.close();
            mRealm = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        setContentView(R.layout.activity_main);

        setAmbientEnabled();

        mClimbType = Climb.ClimbType.values()[
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(PREF_FILTER_CLIMBTYPE, Climb.ClimbType.bouldering.ordinal())]; // TODO: need to allow switching this somewhere...

        mDrawerLayout = (WearableDrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationDrawer = (WearableNavigationDrawer) findViewById(R.id.top_navigation_drawer);
        mActionDrawer = (WearableActionDrawer) findViewById(R.id.bottom_action_drawer);

        mNavigationDrawer.setAdapter(new NavigationAdapter(this));

        mNotEnabledTextView = (TextView)findViewById(R.id.notenabled_textview);
        mDrawerLayout.setDrawerStateCallback(new WearableDrawerLayout.DrawerStateCallback() {
            @Override
            public void onDrawerOpened(View view) {

            }

            @Override
            public void onDrawerClosed(View view) {
                // menu may have changed from clicking indiv climb, so resetup with default menu
                setupActionDrawerMenu(R.menu.action_drawer_menu, WearMainActivity.this);
            }

            @Override
            public void onDrawerStateChanged(@WearableDrawerView.DrawerState int i) {

            }
        });
        setupActionDrawerMenu(R.menu.action_drawer_menu, this);


        mListView = (WearableRecyclerView) findViewById(R.id.listView);
        mTitleView = (TextView)findViewById(R.id.textview_title);
        // get the screen width
        /*DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        // set height of framelayout to be slightly larger than view
        mListView.setMinimumHeight(metrics.heightPixels+1);*/

        setDelayedViewVisible(false);
        checkUserStatus();

    }

    private void checkUserStatus() {
        // check if user is logged in on mobile
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(preferences.getBoolean(Shared.KEY_SYNC_USER, false)) {
            // user is logged in on mobile, so attempt login on wear
            WearUserManager.restoreCredentialPrefs(this);
            WearUserManager.loginAsync(new SyncUser.Callback(){

                @Override
                public void onSuccess(SyncUser user) {
                    WearUserManager.setActiveUser(user);
                    mRealm = Realm.getDefaultInstance();
                    updateLayoutBasedOnPreference();
                }

                @Override
                public void onError(ObjectServerError error) {
                    String errorMsg;
                    switch (error.getErrorCode()) {
                        case UNKNOWN_ACCOUNT:
                            errorMsg = "Account does not exists.";
                            break;
                        case INVALID_CREDENTIALS:
                            errorMsg = "The provided credentials are invalid!"; // This message covers also expired account token
                            break;
                        default:
                            errorMsg = error.toString();
                    }
                    Toast.makeText(WearMainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            });

        }else {
            // not logged in, so attempt logout
            if(SyncUser.currentUser() !=null) {
                WearUserManager.logoutActiveUser();
            }
            if(mRealm!=null) {
                mRealm.close();
                mRealm = null;
            }
            mNotEnabledTextView.setText("User login required");
            mNotEnabledTextView.setVisibility(View.VISIBLE);
            mDrawerLayout.setVisibility(View.GONE);
        }
    }

    private void setupActionDrawerMenu(int menuResId, WearableActionDrawer.OnMenuItemClickListener listener) {
        // NOTE: used to populate the action drawer with different context based menus
        Menu menu = mActionDrawer.getMenu();
        menu.clear(); // clear previous items
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(menuResId, menu);
        mActionDrawer.setOnMenuItemClickListener(listener);
    }

    private void updateLayoutBasedOnPreference() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(preferences.getBoolean(Shared.KEY_WEAR_ENABLED, false)) {
            // ready to show data
            mNotEnabledTextView.setVisibility(View.GONE);
            mDrawerLayout.setVisibility(View.VISIBLE);
            reloadLastSection();
        }else{
            // need to enable wear
            mNotEnabledTextView.setText("Wear not enabled");
            mNotEnabledTextView.setVisibility(View.VISIBLE);
            mDrawerLayout.setVisibility(View.GONE);
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
        //EventBus.getDefault().unregister(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        //EventBus.getDefault().register(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch(key) {
            case Shared.KEY_WEAR_ENABLED:
                updateLayoutBasedOnPreference();
                break;
            case Shared.KEY_SYNC_USER:
                checkUserStatus();
                break;
        }
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
            if(mRealm != null) {
                Section selectedSection = Section.values()[i];
                if (mCurrentSection == selectedSection) {
                    return;
                }
                mCurrentSection = selectedSection;

                switch (selectedSection) {
                    case History:
                        RealmQuery<Climb> query = mRealm.where(Climb.class)
                                .equalTo(ClimbFields.TYPE, mClimbType.ordinal());

                        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(WearMainActivity.this);
                        String gymId = pref.getString(PREF_FILTER_GYM_ID, null);
                        if (gymId != null) {
                            mTitleView.setVisibility(View.VISIBLE);
                            String location = mRealm.where(Gym.class).equalTo(GymFields.ID, gymId).findFirst().getName();
                            query.equalTo(ClimbFields.GYM.ID, gymId);

                            String areaId = pref.getString(PREF_FILTER_AREA_ID, null);
                            if (areaId != null) {
                                query.equalTo(ClimbFields.AREA.ID, areaId);
                                location += ", " + mRealm.where(Area.class).equalTo(AreaFields.ID, areaId).findFirst().getName();
                            }
                            mTitleView.setText(location);
                        } else {
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
                        Intent intent = new Intent(WearMainActivity.this, LocationPickerActivity.class);
                        startActivityForResult(intent, LOCATION_PREF_REQUEST);
                        return;
                    case Settings:
                        // TODO: use https://github.com/denley/WearPreferenceActivity
                        return;
                    default:
                        return;
                }
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
                reloadLastSection();
            }
        }
    }

    private void reloadLastSection() {
        // reload the last section that was open

        if (mLastEmbeddedSection != null) {
            mNavigationDrawer.setCurrentItem(mLastEmbeddedSection.ordinal(), false); // THis will load the new gym/area if it was changed
        } else {
            mNavigationDrawer.setCurrentItem(0, false);
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
            final Climb climb = getItem(position);
            holder.datetime.setText(SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(climb.getDateCreated()));

            float progress = 0;
            RealmResults<Attempt> attempts = climb.getAttempts();
            if(attempts!= null && !attempts.isEmpty()) {
                progress = attempts.last().getProgress();
                holder.attemptsView.setVisibility(View.VISIBLE);
                int numSends = (int) attempts.where().equalTo(AttemptFields.IS_SEND, true).count();
                int numAttempts = attempts.where().notEqualTo(AttemptFields.IS_SEND, true).sum("count").intValue();

                if (progress != 100 && progress != 0) {
                    holder.progress.setVisibility(View.VISIBLE);
                    holder.progress.setText(String.format("%d%%", (int)(progress)));
                } else {
                    // not interesting, so hide progress indicator
                    holder.progress.setVisibility(View.GONE);
                }

                /* TODO: deal with flash and lead climbing.  Should the icon indicate the climb is leadable or the send was onlead?
                if (attempts.first().isSend()) {
                    // flashed
                    holder.flash.setVisibility(View.VISIBLE);
                }else {
                    holder.flash.setVisibility(View.GONE);
                }
                holder.leadSend.setVisibility(View.GONE);
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


            TextDrawable drawable = TextDrawable.builder()
                    .beginConfig()
                    .textColor(Color.BLACK)
                    .useFont(Typeface.DEFAULT)
                    .withBorder(10)
                    .setStrikethrough(climb.isRemoved())
                    .fontSize(30) /* size in px */
                    .bold()
                    .setSweepAngle(progress*360/100) // progress in degrees
                    .endConfig()
                    .buildRound(climb.getType().grades.get(climb.getGrade()), climb.getColor());

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setupActionDrawerMenu(R.menu.indiv_climb_drawer_menu, new WearableActionDrawer.OnMenuItemClickListener(){
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch(item.getItemId()) {
                                case R.id.add_send:
                                    mRealm.executeTransactionAsync(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            realm.copyToRealm(Attempt.createSend(
                                                    realm.where(Climb.class).equalTo(ClimbFields.ID, climb.getId()).findFirst(),
                                                    false));// TODO: need a UI element or dialog box for if climb was on lead
                                        }
                                    });

                                    return true;
                                case R.id.add_attempt:
                                    // TODO: launch activity for attempt
                                    return true;
                                case R.id.work_project:
                                    // TODO: launch activity to work project
                                    return true;
                                case R.id.edit_climb:
                                    startActivity(EditClimbActivity.newEditInstance(WearMainActivity.this, climb));
                                    return true;
                            }
                            return false;
                        }
                    });
                    mActionDrawer.openDrawer();
                }
            });
            holder.grade.setImageDrawable(drawable);
            // sends and attempts
            holder.sends.setCompoundDrawablesWithIntrinsicBounds(Shared.getScaledDrawable(WearMainActivity.this, R.drawable.ic_check_light,20, 20), null, null, null);


            // attempts
            holder.attempts.setCompoundDrawablesWithIntrinsicBounds(Shared.getScaledDrawable(WearMainActivity.this, R.drawable.ic_attempt_light, 20, 20),
                    null, null, null);



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
