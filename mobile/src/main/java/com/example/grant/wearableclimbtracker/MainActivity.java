package com.example.grant.wearableclimbtracker;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.model.Climb;
import com.example.mysynclibrary.model.RealmResultsEvent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tasks.RuntimeExecutionException;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.internal.IOException;

public class MainActivity extends AppCompatActivity implements DataApi.DataListener{

    private static final String TAG = "MainActivity";
    private static final String REALM_CONTENT_CREATOR_CAPABILITY = "realm_content_creator";//Note: capability name defined in wear module values/wear.xml
    private static final String PREFS_NAME = "MyPrefsFile";
    private GoogleApiClient mGoogleApiClient;
    private String mNodeId;
    private Realm mRealm;
    private ChartPagerAdapter mChartPagerAdapter;
    private Switch typeToggle;
    private Shared.ClimbType mType;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        typeToggle = (Switch)menu.findItem(R.id.type_toggle).getActionView().findViewById(R.id.switch1);
        setType(typeToggle.isChecked()? Shared.ClimbType.bouldering: Shared.ClimbType.ropes);

        typeToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setType(isChecked? Shared.ClimbType.bouldering: Shared.ClimbType.ropes);
            }
        });
        return true;
    }

    private void setType(Shared.ClimbType type) {
        //Log.d(TAG, "setType");
        mType = type;
        if(type == Shared.ClimbType.bouldering) {
            typeToggle.setText("B");
        }else{
            typeToggle.setText("TR");
        }
        setRealmResult();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Log.d(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
            case R.id.sync_db:
                // request sync from wear
                Log.d(TAG, "requesting sync");
                sendMessageToRealmCreator(Shared.REALM_SYNC_PATH, null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mGoogleApiClient!=null && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
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

        //Log.d(TAG, "onCreate");

        mRealm = Realm.getDefaultInstance();

        // setup viewpager and adapter
        mChartPagerAdapter = new ChartPagerAdapter(this,getSupportFragmentManager());
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(mChartPagerAdapter);

        // setup google api
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        // add the listener for incoming realm data
                        Wearable.DataApi.addListener(mGoogleApiClient, MainActivity.this);

                        // Now request a sync
                        Log.d(TAG, "requesting sync");
                        sendMessageToRealmCreator(Shared.REALM_SYNC_PATH, null);
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
                        Toast.makeText(MainActivity.this,"No capable nodes found", Toast.LENGTH_LONG).show();
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


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        for (DataEvent event: dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                if(item.getUri().getPath().equals(Shared.REALM_SYNC_PATH)) {
                    final DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

                    Log.d(TAG, "onDataChanged: got datachange -- string = " + dataMap.getString(Shared.DB_DATA_KEY));

                    //get the start of day of last data sync
                    final Gson gson = Shared.getGson();
                    Date lastSyncDate = gson.fromJson(settings.getString("lastSync", gson.toJson(Shared.getStartOfDay())), Date.class);

                    final Climb[] climbList = gson.fromJson(dataMap.getString(Shared.DB_DATA_KEY), Climb[].class);


                    // delete all climbs after last sync day
                    final RealmResults results = mRealm.where(Climb.class)
                            .greaterThan("date", lastSyncDate)
                            .findAll();
                    mRealm.executeTransaction(new Realm.Transaction(){

                        @Override
                        public void execute(Realm realm) {
                            results.deleteAllFromRealm();
                        }
                    });


                    // convert data to realm and add
                    mRealm.executeTransaction(new Realm.Transaction() {

                        @Override
                        public void execute(Realm realm) {
                            try{
                                mRealm.copyToRealmOrUpdate(Arrays.asList(climbList));

                                long count = mRealm.where(Climb.class).count();
                                Log.d(TAG, "database now has " + count + " elements");

                                //set today as the new last sync date
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putString("lastSync", gson.toJson(Shared.getStartOfDay()));
                                editor.commit();


                                // send back acknowledge message
                                Log.d(TAG, "sending sync ack");
                                sendMessageToRealmCreator(Shared.REALM_ACK_PATH, null);
                            } catch(IOException e) {
                                throw new RuntimeExecutionException(e);
                            }
                        }
                    });

                }
            }

        }
    }

    public void setRealmResult() {
        //Log.d(TAG, "setClimbRealmResult");
        RealmResults<Climb> realmResult =  mRealm.where(Climb.class)
                .equalTo("type", mType.ordinal()).findAll();
        EventBus.getDefault().postSticky(new RealmResultsEvent(realmResult)); // send it to all subscribers. post sticky so this result stays until we set it again

    }


    private class ChartPagerAdapter extends FragmentPagerAdapter{
        private final MainActivity mMainActivity;
        private ArrayList<Pair<String,Fragment>> mFragmentList; // <title, fragment>

        public ChartPagerAdapter(MainActivity activity, FragmentManager fm) {
            super(fm);
            mMainActivity = activity;
            initiatePages();
        }

        private void initiatePages() {
            //Log.d(TAG, "initiatePages");
            mFragmentList = new ArrayList<>();

            // set climb type of the content fragment
            Fragment fragment = new OverviewFragment();
            mFragmentList.add(new Pair("Overview", fragment));

            fragment = new BarChartMobileFragment();
            mFragmentList.add(new Pair("Bar Chart", fragment));
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position).second;
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentList.get(position).first;
        }


    }


}
