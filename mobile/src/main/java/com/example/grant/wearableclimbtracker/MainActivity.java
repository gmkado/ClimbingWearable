package com.example.grant.wearableclimbtracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.example.mysynclibrary.SimpleSpanBuilder;
import com.example.mysynclibrary.SyncHelper;
import com.example.mysynclibrary.eventbus.ClimbColorSelectedEvent;
import com.example.mysynclibrary.eventbus.LocationFilterEvent;
import com.example.mysynclibrary.eventbus.RealmSyncEvent;
import com.example.mysynclibrary.realm.Area;
import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.realm.ClimbFields;
import com.example.mysynclibrary.realm.Gym;
import com.example.mysynclibrary.realm.GymFields;
import com.opencsv.CSVWriter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import io.realm.Realm;
import io.realm.RealmResults;

import static com.example.grant.wearableclimbtracker.FilterLocationDialogFragment.PREF_FILTER_AREA_ID;
import static com.example.grant.wearableclimbtracker.FilterLocationDialogFragment.PREF_FILTER_CLIMBTYPE;
import static com.example.grant.wearableclimbtracker.FilterLocationDialogFragment.PREF_FILTER_GYM_ID;
import static com.example.mysynclibrary.realm.ISyncableRealmObject.SyncState.DELETE;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ColorChooserDialog.ColorCallback {
    private static final String TAG = "MainActivity";
    private static final String PREF_LASTSYNC = "prefLastSync";
    private static final String PREF_NAV_CURRID = "prefCurrNavID"; // the last opened nav view id
    private static final long SYNC_TIMOUT_MS = 3000; // time to wait for response from wearable
    private int mCurrNavId; // keep track of current view id so we can save it on exiting

    private static final String FILE_AUTHORITY = "com.example.grant.wearableclimbtracker.fileprovider";
    private Realm mRealm;
    private SyncHelper.ServerSide mServerHelper;

    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        mServerHelper.disconnect();

        // Save the current page
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(PREF_NAV_CURRID, mCurrNavId);
        editor.apply();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        mServerHelper.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mRealm = Realm.getDefaultInstance();

        // TODO: set sync menu title to last update
        //getSupportActionBar().setSubtitle("Last sync: " + pref.getString(PREF_LASTSYNC, "never"));
        mServerHelper = new SyncHelper(this).new ServerSide();

        // Show climbs fragment
        Fragment fragment = ClimbListMobileFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.content_main, fragment).commit();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        int currItemId = preferences.getInt(PREF_NAV_CURRID, -1);
        MenuItem item;
        if(currItemId == -1) {
            item = navigationView.getMenu().getItem(0);
        }else {
            item = navigationView.getMenu().findItem(currItemId);
        }
        onNavigationItemSelected(item); // NOTE: https://stackoverflow.com/questions/31233279/navigation-drawer-how-do-i-set-the-selected-item-at-startup
        //item.setChecked(true);
        //setTitle(item.getTitle()); // Set action bar title

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        Fragment fragment;
        Intent intent;

        // reset the title
        getSupportActionBar().setTitle("");
        getSupportActionBar().setSubtitle("");
        mCurrNavId = item.getItemId();
        switch(item.getItemId()) {
            case R.id.nav_climb:
                // show the climb fragment
                fragment = ClimbListMobileFragment.newInstance();
                getSupportFragmentManager().beginTransaction().replace(R.id.content_main, fragment).commit();

                item.setChecked(true); // Highlight the selected item has been done by NavigationView
                setClimbFragmentTitle();
                break;
            case R.id.nav_goal:
                fragment = GoalListMobileFragment.newInstance();
                getSupportFragmentManager().beginTransaction().replace(R.id.content_main, fragment).commit();

                item.setChecked(true); // Highlight the selected item has been done by NavigationView
                getSupportActionBar().setTitle(item.getTitle()); // Set action bar title
                break;
            case R.id.nav_editgyms:
                fragment = GymListFragment.newInstance();
                getSupportFragmentManager().beginTransaction().addToBackStack(null)
                        .replace(R.id.content_main, fragment).commit();  // Add to backstack so back button brings us back to main page
                getSupportActionBar().setTitle(item.getTitle()); // Set action bar title
                break;
            case R.id.nav_sync:
                // start sync process
                Log.d(TAG, "requesting sync");
                mServerHelper.sendSyncRequest();

                // start a timer to see if we get a response
                //mTimerHandler.postDelayed(mTimerRunnable, SYNC_TIMOUT_MS);
                break;
            case R.id.nav_settings:
                // open settings
                //TODO: can't get out of here once its clicked
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_export:
                // query all sends and export to CSV
                File imagePath = new File(getApplicationContext().getFilesDir(), "exports");
                boolean dirExists = imagePath.exists();
                if (!dirExists) {
                    dirExists = imagePath.mkdirs(); // create the directory if it doesn't exist already
                }
                if (dirExists) {
                    File newFile = new File(imagePath, "myclimbs.csv"); // create the csv file
                    try {
                        CSVWriter writer = new CSVWriter(new FileWriter(newFile), ',');  // write to the csv file
                        writer.writeNext(Climb.getTitleRow());

                        RealmResults<Climb> result = mRealm.where(Climb.class).notEqualTo(ClimbFields.SYNC_STATE, DELETE.name()).findAll();
                        for (Climb climb : result) {
                            writer.writeNext(climb.toStringArray());
                        }
                        writer.close();
                    } catch (IOException e) {
                        Log.e(TAG, "error during csv write:" + e.getMessage());
                        return true;
                    }

                    Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                    intentShareFile.setAction(Intent.ACTION_SEND);

                    // NOTE: using https://medium.com/google-developers/sharing-content-between-android-apps-2e6db9d1368b
                    Uri contentUri;
                    try {
                        contentUri = FileProvider.getUriForFile(this, FILE_AUTHORITY, newFile);
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "The selected file can't be shared: " + newFile.getName());
                        return true;
                    }

                    if (contentUri != null) {
                        Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                                .setStream(contentUri)
                                .setType("text/csv")
                                .createChooserIntent();
                        // Provide read access
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        if (shareIntent.resolveActivity(getPackageManager()) != null) {
                            Log.d(TAG, "Successfully created share intent.  starting activity");
                            startActivity(shareIntent);
                        } else {
                            Log.e(TAG, "No activity available to handle MIME type ");
                        }
                    } else {
                        Log.e(TAG, "failed to get content URI");
                    }

                } else {
                    Log.e(TAG, "Could not create directory");
                }
                break;
            case R.id.nav_feedback:
                intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "gkadokura+climbapp@gmail.com", null));
                intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback for climbing app"); // TODO: put correct name here
                //intent.putExtra(Intent.EXTRA_TEXT, message);
                startActivity(Intent.createChooser(intent, "Choose an Email client :"));
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }




    /**************** EVENT BUS LISTENERS ***************************/
    /*@Subscribe(threadMode = ThreadMode.MAIN)
    public void onWearMessageReceived(WearMessageEvent event) {
        String data;
        switch(event.messageEvent.getPath()) {
            case Shared.REALM_SYNC_PATH:
                // stop timer since we got a response
                mTimerHandler.removeCallbacks(mTimerRunnable);

                // received the sync data
                data =  new String(event.messageEvent.getData(), StandardCharsets.UTF_8);
                Log.d(TAG, "got sync = " + data);
                Shared.deserializeAndMerge(mRealm, data);

                // now send dirty and deleted
                data = Shared.serializeObjectsToSync(mRealm);
                Log.d(TAG, "Sending ack:" + data);
                sendMessageToRealmCreator(Shared.REALM_ACK_PATH, data.getBytes(StandardCharsets.UTF_8));

                //set today as the new last sync gym
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                DateFormat df = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

                final String newSyncDate = df.format(new Date());
                editor.putString(PREF_LASTSYNC, newSyncDate);
                editor.apply();
                getSupportActionBar().setSubtitle("Last sync: " + newSyncDate);
                break;
            default:
                Log.e(TAG, "onWearMessageReceived: message path not recognized");
                break;
        }

    }*/

    private void setClimbFragmentTitle() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        Climb.ClimbType filterClimbType = Climb.ClimbType.values()[pref.getInt(PREF_FILTER_CLIMBTYPE, Climb.ClimbType.bouldering.ordinal())];
        String filterGymId = pref.getString(PREF_FILTER_GYM_ID, null);
        String filterAreaId = pref.getString(PREF_FILTER_AREA_ID, null);
        getSupportActionBar().setTitle(filterClimbType.title);

        SimpleSpanBuilder ssb = new SimpleSpanBuilder();
        if(filterGymId!=null) {
            ssb.append(mRealm.where(Gym.class).equalTo(GymFields.ID, filterGymId).findFirst().getName() + "\n");
            if (filterAreaId != null) {
                ssb.append(mRealm.where(Area.class).equalTo(GymFields.ID, filterAreaId).findFirst().getName() + "\n");
            }
        }
        getSupportActionBar().setSubtitle(ssb.build().toString());
    }

    @Subscribe
    public void onLocationFilterEvent(LocationFilterEvent event) {
        // we've sorted the location, so update the title
        setClimbFragmentTitle();
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, @ColorInt int selectedColor) {
        // FIXME: Cannot give colorchooser a callback in editclimbdialogfragment, so MainACtivity implements  ColorChooserDialog.ColorCallback and fires an event so we can handle it there
        EventBus.getDefault().post(new ClimbColorSelectedEvent(selectedColor));
    }

    @Override
    public void onColorChooserDismissed(@NonNull ColorChooserDialog dialog) {

    }

    @Subscribe
    public void onRealmSyncEvent(RealmSyncEvent event) {
        switch (event.step) {
            case SYNC_REQUESTED:
                // do nothing
                break;
            case REMOTE_SAVED_TO_TEMP:
                mServerHelper.mergeLocalWithRemote();
                break;
            case REALM_DB_MERGED:
                mServerHelper.sendRealmDb();
                break;
        }
        EventBus.getDefault().removeStickyEvent(event);
    }

}
