package com.example.grant.wearableclimbtracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.eventbus.ClimbColorSelectedEvent;
import com.example.mysynclibrary.eventbus.WearMessageEvent;
import com.example.mysynclibrary.realm.Climb;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.opencsv.CSVWriter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ColorChooserDialog.ColorCallback {
    private static final String TAG = "MainActivity";
    private static final String REALM_CONTENT_CREATOR_CAPABILITY = "realm_content_creator";//Note: capability name defined in wear module values/wear.xml
    private static final String PREF_LASTSYNC = "prefLastSync";
    private static final long SYNC_TIMOUT_MS = 3000; // time to wait for response from wearable

    private static final String FILE_AUTHORITY = "com.example.grant.wearableclimbtracker.fileprovider";
    private GoogleApiClient mGoogleApiClient;
    private String mNodeId;
    private Realm mRealm;

    private Handler mTimerHandler = new Handler();
    private Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(MainActivity.this,"No response from wear. Check that wear settings is enabled and app is open on wearable", Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if(mGoogleApiClient!=null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();

        EventBus.getDefault().register(this);
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

        // TODO:set sync menu title to last update
        //getSupportActionBar().setSubtitle("Last sync: " + pref.getString(PREF_LASTSYNC, "never"));

        // setup google api
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);

                        // Now request a sync
                        //sendMessageToRealmCreator(Shared.REALM_SYNC_PATH, null);
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                // Request access only to the Wearable API
                .addApi(Wearable.API)
                .build();

        // Show climbs fragment
        Fragment fragment = ClimbListMobileFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.content_main, fragment).commit();

        MenuItem item = navigationView.getMenu().getItem(0);
        item.setChecked(true);
        setTitle(item.getTitle()); // Set action bar title

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
        switch(item.getItemId()) {
            case R.id.nav_climb:
                // show the climb fragment
                fragment = ClimbListMobileFragment.newInstance();
                getSupportFragmentManager().beginTransaction().replace(R.id.content_main, fragment).commit();

                item.setChecked(true); // Highlight the selected item has been done by NavigationView
                setTitle(item.getTitle()); // Set action bar title
                break;
            case R.id.nav_goal:
                fragment = GoalListFragment.newInstance();
                getSupportFragmentManager().beginTransaction().replace(R.id.content_main, fragment).commit();

                item.setChecked(true); // Highlight the selected item has been done by NavigationView
                setTitle(item.getTitle()); // Set action bar title
                break;
            case R.id.nav_editgyms:
                fragment = GymListFragment.newInstance();
                getSupportFragmentManager().beginTransaction().addToBackStack(null)
                        .replace(R.id.content_main, fragment).commit();  // Add to backstack so back button brings us back to main page
                setTitle(item.getTitle()); // Set action bar title
                break;
            case R.id.nav_sync:
                // start sync process
                Log.d(TAG, "requesting sync");
                sendMessageToRealmCreator(Shared.REALM_SYNC_PATH, null);

                // start a timer to see if we get a response
                mTimerHandler.postDelayed(mTimerRunnable, SYNC_TIMOUT_MS);
                break;
            case R.id.nav_settings:
                // open settings
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

                        RealmResults<Climb> result = mRealm.where(Climb.class).notEqualTo("delete", true).findAll();
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

    private void sendMessageToRealmCreator(final String path, final byte[] data) {
        //Log.d(TAG,"sendMessageToRealmCreator");
        if(mNodeId == null) {
            // need to find a capable node
            PendingResult result = Wearable.CapabilityApi.getCapability(
                    mGoogleApiClient, REALM_CONTENT_CREATOR_CAPABILITY,
                    CapabilityApi.FILTER_REACHABLE);
            result.setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
                @Override
                public void onResult(@NonNull CapabilityApi.GetCapabilityResult result) {
                    Set<Node> connectedNodes = result.getCapability().getNodes();

                    // for now only anticipate single node with this capability
                    // see message api docs if this changes
                    if (connectedNodes.size() > 1) {
                        Log.e(TAG, "More than one capable node connected.  This shouldn't happen");
                    } else if(connectedNodes.isEmpty()) {
                        Toast.makeText(MainActivity.this,"No wearable found", Toast.LENGTH_LONG).show();
                    } else{
                        Log.d(TAG, "setting node id");
                        mNodeId = connectedNodes.iterator().next().getId();
                        sendMessage(path, data);
                    }
                }
            });
        }
        else{
            sendMessage(path, data);
        }
    }

    private void sendMessage(String path, byte[] data) {
        Wearable.MessageApi.sendMessage(mGoogleApiClient, mNodeId,
                path, data).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                if(sendMessageResult.getStatus().isSuccess()) {
                    Log.d(TAG, "Message sent");
                }else{
                    // failed message
                    Log.e(TAG, "Message failed");
                }

            }
        });
    }

    /**************** EVENT BUS LISTENERS ***************************/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWearMessageReceived(WearMessageEvent event) {
        switch(event.messageEvent.getPath()) {
            case Shared.REALM_SYNC_PATH:
                // stop timer since we got a response
                mTimerHandler.removeCallbacks(mTimerRunnable);

                // received the sync data
                final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
                String data =  new String(event.messageEvent.getData(), StandardCharsets.UTF_8);

                Log.d(TAG, "got message = " + data);

                //get the start of DAY of last data sync
                final Gson gson = Shared.getGson();
                final Climb[] climbList = gson.fromJson(data, Climb[].class);

                // convert data to realm and add
                mRealm.executeTransactionAsync(new Realm.Transaction() {

                    @Override
                    public void execute(Realm realm) {
                        for (Climb wearClimb : climbList) {
                            // get the climb on mobile
                            Climb mobileClimb = realm.where(Climb.class).equalTo("id", wearClimb.getId()).findFirst();
                            if(wearClimb.isDelete() && mobileClimb!=null) {
                                // wear climb marked for deletion which takes precedence over mobile climb
                                mobileClimb.deleteFromRealm();
                            }else if(mobileClimb!=null){
                                if(mobileClimb.isDelete()) {
                                    // mobile climb marked for deletion, which takes precedence over wear climb
                                    mobileClimb.deleteFromRealm();
                                }else if (wearClimb.getLastedit().after(mobileClimb.getLastedit())) {
                                    // use wear climb if it was edited last, otherwise keep mobile
                                    realm.copyToRealmOrUpdate(wearClimb);
                                }
                            }else {
                                // climb doesn't exist on mobile, so add it
                                realm.copyToRealm(wearClimb);
                            }
                        }
                        // query all marked for deletion and delete
                        RealmResults<Climb> deleteClimbs = realm.where(Climb.class).equalTo("delete", true).findAll();
                        deleteClimbs.deleteAllFromRealm();


                        //set today as the new last sync gym
                        SharedPreferences.Editor editor = settings.edit();
                        DateFormat df = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

                        final String newSyncDate = df.format(new Date());
                        editor.putString(PREF_LASTSYNC, newSyncDate);
                        editor.commit();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getSupportActionBar().setSubtitle("Last sync: " + newSyncDate);
                            }
                        });
                        List<Climb> nonWear = realm.where(Climb.class).lessThan("gym",Shared.getStartofDate(null)).equalTo("onwear", true).findAll();
                        for (Climb climb: nonWear) {
                            climb.setOnwear(false);
                        }

                        List<Climb> todaysClimbs = realm.where(Climb.class).greaterThanOrEqualTo("gym",Shared.getStartofDate(null)).findAll();

                        //set onwear to true
                        for (Climb climb: todaysClimbs) {
                            climb.setOnwear(true);
                        }
                        List<Climb> todaysClimbsCopy = realm.copyFromRealm(todaysClimbs);

                        // copy the results into a string format
                        String json = gson.toJson(todaysClimbsCopy);
                        Log.d(TAG, "Sending ACK:" + json);
                        sendMessageToRealmCreator(Shared.REALM_ACK_PATH, json.getBytes(StandardCharsets.UTF_8));
                    }
                }, new Realm.Transaction.OnError() {
                    @Override
                    public void onError(Throwable error) {
                        Log.e(TAG, "Failed merging wear data with mobile: " + error.getMessage());
                    }
                });
                break;
            default:
                Log.e(TAG, "onWearMessageReceived: message path not recognized");
                break;
        }

    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, @ColorInt int selectedColor) {
        // notify the edit color dialog
        EventBus.getDefault().post(new ClimbColorSelectedEvent(selectedColor));
    }

    @Override
    public void onColorChooserDismissed(@NonNull ColorChooserDialog dialog) {

    }
}
