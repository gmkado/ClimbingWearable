package com.example.mysynclibrary;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.example.mysynclibrary.eventbus.RealmSyncEvent;
import com.example.mysynclibrary.realm.Area;
import com.example.mysynclibrary.realm.Attempt;
import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.realm.ClimbFields;
import com.example.mysynclibrary.realm.Goal;
import com.example.mysynclibrary.realm.Gym;
import com.example.mysynclibrary.realm.ISyncableRealmObject;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.apache.commons.io.FileUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.exceptions.RealmException;

import static android.R.attr.data;
import static com.example.mysynclibrary.eventbus.RealmSyncEvent.SyncProcessStep.REALM_DB_MERGED;
import static com.example.mysynclibrary.realm.ISyncableRealmObject.SyncState.CLEAN;
import static com.example.mysynclibrary.realm.ISyncableRealmObject.SyncState.DELETE;
import static com.example.mysynclibrary.realm.ISyncableRealmObject.SyncState.DIRTY;

/*
This class was taken from here: https://gist.github.com/tajchert/dc30560891bc6aee76fb#file-filesender-java
 */
public class SyncHelper{
    private static final String REALM_CONTENT_CREATOR_CAPABILITY = "realm_content_creator";//Note: capability name defined in wear module values/wear.xml
    //message api paths
    public static final String REALM_SYNC_PATH = "/sync-data";
    //public static final String REALM_ACK_PATH = "/sync-ack";

    //Asset paths
    public static final String SERVER_DB_PATH = "/serverDatabasePath";
    public static final String CLIENT_DB_PATH = "/remoteDatabasePath";
    public static final String DB_KEY = "realmDatabase";

    //Class variables
    public static final String TEMP_REALM_NAME = "temp.realm";
    private static final String TIMESTAMP_KEY = "timestampkey";
    private final Context context;
    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "SyncHelper";



    public SyncHelper(Context context) {
        this.context = context;
    }




    public void sendMessage(final String path, final byte[] data) {
        // need to find a capable node
        Log.d(TAG, "sending message");
        PendingResult<CapabilityApi.GetCapabilityResult> result = Wearable.CapabilityApi.getCapability(
                mGoogleApiClient, REALM_CONTENT_CREATOR_CAPABILITY,
                CapabilityApi.FILTER_REACHABLE);
        result.setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
            @Override
            public void onResult(@NonNull CapabilityApi.GetCapabilityResult getCapabilityResult) {
                Set<Node> connectedNodes = getCapabilityResult.getCapability().getNodes();

                // for now only anticipate single node with this capability
                // see message api docs if this changes
                if (connectedNodes.size() > 1) {
                    Log.e(TAG, "More than one capable node connected.  This shouldn't happen");
                } else if (connectedNodes.isEmpty()) {
                    Toast.makeText(context, "No wearable found", Toast.LENGTH_LONG).show();
                } else {
                    Log.d(TAG, "setting node id");
                    String nodeId = connectedNodes.iterator().next().getId();
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId,
                            path, data).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                            if (sendMessageResult.getStatus().isSuccess()) {
                                Log.d(TAG, "Message sent");
                            } else {
                                // failed message
                                Log.e(TAG, "Message failed");
                            }
                        }
                    });
                }

            }
        });
    }

    private void sendRealmDb(String path) {
        File writableFolder = context.getFilesDir();
        File realmFile = new File(writableFolder, Realm.DEFAULT_REALM_NAME);
        try {
            Asset realmAsset = Asset.createFromBytes(FileUtils.readFileToByteArray(realmFile));
            if (!mGoogleApiClient.isConnected()) {
                Toast.makeText(context, "Google API Client not connected!", Toast.LENGTH_SHORT).show();
                return;
            }

            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(path);
            byte[] arr = realmAsset.getData();
            DataMap dataMap = dataMapRequest.getDataMap();
            dataMap.putByteArray(DB_KEY, arr);  // for some reason .putAsset wasn't working for me in some cases
            dataMap.putString(TIMESTAMP_KEY, Calendar.getInstance().getTime().toString());
            PutDataRequest request = dataMapRequest.asPutDataRequest().setUrgent();
            PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, request);
            pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                    Log.d(TAG, "PutDataItem result:" + dataItemResult.getStatus());
                }
            });
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void buildGoogleApiClient() {
        Log.d(TAG, "building google api client");
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApiIfAvailable(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        Log.d(TAG, "google api client connected!");
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                        // try to connect again
                        mGoogleApiClient.connect();

                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                }).build();
    }

    private void connnect() {
        if (!mGoogleApiClient.isConnecting() || !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    private void disconnect(){
        if (mGoogleApiClient.isConnecting() || mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    // return true if saved successfully
    public static boolean saveRealmToFile(Context context, byte[] byteArray, String name){
        Log.d(TAG, "saving realm to file:" + name);
        File writableFolder = context.getFilesDir();
        File realmFile = new File(writableFolder, name);
        if (realmFile.exists()) {
            if(!realmFile.delete()) {
                // something went wrong
                Log.e(TAG, "Could not delete old realm file");
                return false;
            }
        }
        try {
            FileUtils.writeByteArrayToFile(realmFile, byteArray);
            return true;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }

    public class ServerSide  {
        private static final String TAG = "SyncHelper.ServerSide";
        private static final int NUM_OBJECTS = 5;
        ConcurrentBitSet syncStatus = new ConcurrentBitSet(NUM_OBJECTS);

        public ServerSide() {
            buildGoogleApiClient();
        }

        public void connect() {
            SyncHelper.this.connnect();
        }

        public void disconnect() {
            SyncHelper.this.disconnect();
        }

        public void sendSyncRequest() {
            Log.d(TAG, "sending sync request");
            if (!mGoogleApiClient.isConnected()) {
                Toast.makeText(context, "Google API Client not connected!", Toast.LENGTH_SHORT).show();
                return;
            }
            sendMessage(REALM_SYNC_PATH, null);
        }

        public void mergeLocalWithRemote() {
            Log.d(TAG, "Merging local with remote");
            syncStatus.clear(); // reset syncstatus

            // sync from the temp realm

            mergeLocalWithRemote(Climb.class);
            mergeLocalWithRemote(Attempt.class);
            mergeLocalWithRemote(Gym.class);
            mergeLocalWithRemote(Area.class);
            mergeLocalWithRemote(Goal.class);
        }

        public void sendRealmDb() {
            Log.d(TAG, "sending realm database");
            SyncHelper.this.sendRealmDb(SERVER_DB_PATH);
        }


        private <T extends RealmObject & ISyncableRealmObject> void mergeLocalWithRemote(final Class<T> clazz) {
            // query the remote realm for
            final List<T> remoteObjects = getObjectsToSync(clazz);
            // convert data to realm and add
            try(Realm realm = Realm.getDefaultInstance()) {
                realm.beginTransaction();
                if (remoteObjects.size() != 0) {
                    for (T remoteObject : remoteObjects) {
                        // get the climb on mobile
                        T localObject = realm.where(clazz).equalTo(ClimbFields.ID, remoteObject.getId()).findFirst();
                        if (localObject != null) {
                            if (remoteObject.getSyncState() == DELETE || localObject.getSyncState() == DELETE) {
                                localObject.safedelete(true);
                            } else if (remoteObject.getLastEdit().after(localObject.getLastEdit())) {
                                // wear climb edited last
                                realm.copyToRealmOrUpdate(remoteObject);
                            }
                        } else {
                            if (remoteObject.getSyncState() != DELETE) {
                                // object doesn't exist on mobile, so add it
                                T managedCopy = realm.copyToRealmOrUpdate(remoteObject); // FIXME: getting errors here using copyToRealm, same suspicion as "overrideLocalWitRemote" fixme. Since I'm pretty sure logic is right, just use copyToRealmOrUpdate

                            }
                        }
                    }
                }
                // now clean up the local side by deleting all from realm and marking dirty as clean and onremote
                RealmResults<T> toDelete = realm.where(clazz)
                        .equalTo(ClimbFields.SYNC_STATE, DELETE.name()).findAll(); // NOTE: this requires all syncstates to have the same name!!!
                for(T object:toDelete) {
                    object.safedelete(true); // use safe delete so deletions are propagated correctly, see note in ISyncableRealmObject method prototype
                }
                RealmResults<T> toClean = realm.where(clazz)
                        .equalTo(ClimbFields.SYNC_STATE, DIRTY.name()).findAll(); // NOTE: this requires all syncstates to have the same name!!!

                for (T object : toClean) {
                    object.setSyncState(CLEAN);
                    object.setOnRemote(true);
                }
                realm.commitTransaction();
                setSyncBitAndCheckCompletion(clazz);
                //
            }
        }

        private <T extends RealmObject & ISyncableRealmObject> void setSyncBitAndCheckCompletion(Class<T> clazz) {
            // notify SyncHelper so we can update status bits
            Log.d(TAG, "Synced " + clazz.getCanonicalName());
            if(clazz == Climb.class) {
                syncStatus.set(0);
            }else if(clazz == Attempt.class) {
                syncStatus.set(1);
            }else if(clazz == Gym.class) {
                syncStatus.set(2);
            }else if(clazz == Area.class) {
                syncStatus.set(3);
            }else if(clazz == Goal.class) {
                syncStatus.set(4);
            }else {
                throw new IllegalArgumentException("Unexpected class type");
            }

            if (syncStatus.cardinality() == NUM_OBJECTS) {
                Log.d(TAG, "Done syncing objects");
                // TODO: at this point we could delete the temp realm file
                EventBus.getDefault().postSticky(new RealmSyncEvent(RealmSyncEvent.SyncProcessStep.REALM_DB_MERGED));
            }
        }

        private <T extends RealmObject & ISyncableRealmObject> List<T> getObjectsToSync(Class<T> clazz) {
            Log.d(TAG, "getting objects to sync: " + clazz.getCanonicalName());
            List<T> unmanagedObjects = null;
            try (Realm tempRealm = Realm.getInstance(Shared.getRealmConfig(TEMP_REALM_NAME))) {
                unmanagedObjects = tempRealm.copyFromRealm(
                        tempRealm
                                .where(clazz)
                                .equalTo(ClimbFields.SYNC_STATE, DIRTY.name())
                                .or()
                                .equalTo(ClimbFields.SYNC_STATE, DELETE.name()).findAll());
            }
            return unmanagedObjects;
        }
    }

    public class ClientSide {
        private static final String TAG = "SyncHelper.ClientSide";

        public ClientSide() {
            buildGoogleApiClient();
        }

        public void connect() {
            SyncHelper.this.connnect();
        }

        public void disconnect() {
            SyncHelper.this.disconnect();
        }

        public void sendRealmDb()
        {
            Log.d(TAG, "sending realm database");
            SyncHelper.this.sendRealmDb(CLIENT_DB_PATH);
        }

        public void overwriteLocalWithRemote() {
            Log.d(TAG, "overwriting local with remote");
            // merge every object in database
            try (Realm tempRealm = Realm.getInstance(Shared.getRealmConfig(TEMP_REALM_NAME));
            Realm realm = Realm.getDefaultInstance();){
                realm.beginTransaction();
                realm.deleteAll();
                // FIXME: using copyToRealmOrUpdate here because I suspect that copying Climbs also copies child "attempts" and is causing "primary key already exists" errors
                realm.copyToRealmOrUpdate(tempRealm.where(Climb.class).findAll());
                realm.copyToRealmOrUpdate(tempRealm.where(Attempt.class).findAll());
                realm.copyToRealmOrUpdate(tempRealm.where(Area.class).findAll());
                realm.copyToRealmOrUpdate(tempRealm.where(Gym.class).findAll());
                realm.copyToRealmOrUpdate(tempRealm.where(Goal.class).findAll());
                realm.commitTransaction();
                EventBus.getDefault().postSticky(new RealmSyncEvent(REALM_DB_MERGED));
            }catch (RealmException e){
                Log.e(TAG, "Failed to overwrite realm:" + e.getMessage());
            }
        }


    }



}