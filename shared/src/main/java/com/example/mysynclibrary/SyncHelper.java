package com.example.mysynclibrary;

import android.content.Context;
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
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.apache.commons.io.FileUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;

import static com.example.mysynclibrary.eventbus.RealmSyncEvent.SyncProcessStep.REALM_DB_MERGED;

/*
This class was taken from here: https://gist.github.com/tajchert/dc30560891bc6aee76fb#file-filesender-java
 */
public class SyncHelper{
    //message api paths
    public static final String REALM_SYNC_PATH = "/sync-data";
    //public static final String REALM_ACK_PATH = "/sync-ack";

    //Asset paths
    public static final String DB_PATH = "/ourAppDatabase";
    public static final String DB_KEY = "realmDatabase";

    //Class variables
    public static final String TEMP_REALM_NAME = "temp.realm";
    private final Context context;
    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "SyncHelper";

    abstract class GoogleApiManagedAsyncTask extends AsyncTask<Void, Void, Void> {
        // NOTE: this is just to manage the google api client and ensure we close it
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // remember to disconnect google api
            mGoogleApiClient.disconnect();
        }
    }

    public SyncHelper(Context context) {
        this.context = context;
    }

    private class FileSender extends GoogleApiManagedAsyncTask {
        private String path;
        private String key;
        private Asset asset;
        private static final String TAG = "AssetsSender";

        FileSender(Asset asset, String path, String key) {
            this.asset = asset;
            this.path = path;
            this.key = key;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (asset == null) {
                return null;
            }

            // Send the file
            PutDataMapRequest dataMap = PutDataMapRequest.create(path);
            byte[] arr = asset.getData();
            dataMap.getDataMap().putByteArray(key, arr);  // for some reason .putAsset wasn't working for me in some cases
            PutDataRequest request = dataMap.asPutDataRequest();
            PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, request);
            pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult dataItemResult) {
                    Log.d(TAG, "onResult result:" + dataItemResult.getStatus());
                }
            });

            return null;
        }
    }

    private class MessageSender extends GoogleApiManagedAsyncTask {
        private String path;
        private byte[] data;
        private static final String REALM_CONTENT_CREATOR_CAPABILITY = "realm_content_creator";//Note: capability name defined in wear module values/wear.xml

        private static final String TAG = "AssetsSender";

        MessageSender(String path, byte[] data) {
            this.path = path;
            this.data = data;
        }

        @Override
        protected Void doInBackground(Void... params) {
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
                        Toast.makeText(context,"No wearable found", Toast.LENGTH_LONG).show();
                    } else{
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

            return null;
        }
    }

    private void sendRealmDb() {
        File writableFolder = context.getFilesDir();
        File realmFile = new File(writableFolder, Realm.DEFAULT_REALM_NAME);
        try {
            Asset realmAsset = Asset.createFromBytes(FileUtils.readFileToByteArray(realmFile));
            connectToGoogleApiAndRunTask(new FileSender(realmAsset, DB_PATH, DB_KEY));
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void connectToGoogleApiAndRunTask(final GoogleApiManagedAsyncTask task) {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApiIfAvailable(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        task.execute();
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
                }).build();
        mGoogleApiClient.connect();
    }

    // return true if saved successfully
    public static boolean saveRealmToFile(Context context, byte[] byteArray, String name){
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

    public class ServerSide {
        BitSet syncStatus = new BitSet(5);

        public void sendSyncRequest() {
            connectToGoogleApiAndRunTask(new MessageSender(REALM_SYNC_PATH, null));
        }

        public void mergeLocalWithRemote() {
            syncStatus.clear(); // reset syncstatus

            // sync from the temp realm
            mergeLocalWithRemote(Climb.class, RealmSyncEvent.SyncObjectBit.climb);
            mergeLocalWithRemote(Attempt.class, RealmSyncEvent.SyncObjectBit.attempt);
            mergeLocalWithRemote(Gym.class, RealmSyncEvent.SyncObjectBit.gym);
            mergeLocalWithRemote(Area.class, RealmSyncEvent.SyncObjectBit.area);
            mergeLocalWithRemote(Goal.class, RealmSyncEvent.SyncObjectBit.goal);
        }

        public void sendRealmDb() {
            SyncHelper.this.sendRealmDb();
        }


        private <T extends RealmObject & ISyncableRealmObject> void mergeLocalWithRemote(final Class<T> clazz, final RealmSyncEvent.SyncObjectBit syncBit) {
            // query the remote realm for
            final List<T> remoteObjects = getObjectsToSync(clazz);
            if (remoteObjects.size() != 0) {
                // convert data to realm and add
                try(Realm realm = Realm.getDefaultInstance()) {
                    realm.executeTransactionAsync(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            for (T remoteObject : remoteObjects) {
                                // get the climb on mobile
                                if (remoteObject.isOnRemote()) {
                                    // should exist on the local db since remote.onRemote is true
                                    T localObject = realm.where(clazz).equalTo(ClimbFields.ID, remoteObject.getId()).findFirst();
                                    if (remoteObject.isDelete()) {
                                        RealmObject.deleteFromRealm(localObject);
                                    } else if (!localObject.isDelete()) {
                                        // mobile climb not marked for deletion
                                        if (remoteObject.getLastEdit().after(localObject.getLastEdit())) {
                                            // wear climb edited last
                                            realm.copyToRealmOrUpdate(remoteObject);
                                        }
                                    }
                                } else {
                                    if (!remoteObject.isDelete()) {
                                        // object doesn't exist on mobile, so add it
                                        realm.copyToRealm(remoteObject);
                                    }
                                }
                            }

                            // now clean up the local side by deleting all from realm and marking dirty as clean and onremote
                            RealmResults<T> toDelete = realm.where(clazz)
                                    .equalTo(ClimbFields.SYNC_STATE.DELETE, true).findAll(); // NOTE: this requires all syncstates to have the same name!!!
                            toDelete.deleteAllFromRealm();

                            RealmResults<T> toClean = realm.where(clazz)
                                    .equalTo(ClimbFields.SYNC_STATE.DIRTY, true).findAll(); // NOTE: this requires all syncstates to have the same name!!!

                            for (T object : toClean) {
                                object.synced();
                            }
                        }
                    }, new Realm.Transaction.OnSuccess() {
                        @Override
                        public void onSuccess() {
                            // notify SyncHelper so we can update status bits
                            Log.d(TAG, "Synced " + syncBit.name());
                            EventBus.getDefault().post(syncBit);
                        }
                    });
                }

            }
        }

        public void setSyncBit(RealmSyncEvent.SyncObjectBit bitIndex) {
            syncStatus.set(bitIndex.ordinal());
            if (syncStatus.length() == syncStatus.nextClearBit(0)) {
                Log.d(TAG, "Done syncing!!!");
                // TODO: at this point we could delete the temp realm file
                EventBus.getDefault().post(new RealmSyncEvent(RealmSyncEvent.SyncProcessStep.REMOTE_SAVED_TO_TEMP));
            }
        }

        private <T extends RealmObject & ISyncableRealmObject> List<T> getObjectsToSync(Class<T> clazz) {
            List<T> unmanagedObjects = null;
            try (Realm tempRealm = Realm.getInstance(Shared.getRealmConfig(TEMP_REALM_NAME))) {
                unmanagedObjects = tempRealm.copyFromRealm(
                        tempRealm
                                .where(clazz)
                                .equalTo(ClimbFields.SYNC_STATE.DIRTY, true)
                                .or()
                                .equalTo(ClimbFields.SYNC_STATE.DELETE, true).findAll());
            }
            return unmanagedObjects;
        }
    }

    public class ClientSide {
        public void sendRealmDb() {
            SyncHelper.this.sendRealmDb();
        }
        public void overwriteLocalWithRemote() {
            // merge every object in database
            try (Realm tempRealm = Realm.getInstance(Shared.getRealmConfig(TEMP_REALM_NAME));
                Realm realm = Realm.getDefaultInstance();) {
                realm.deleteAll();
                realm.copyToRealm(tempRealm.where(Climb.class).findAll());
                realm.copyToRealm(tempRealm.where(Attempt. class).findAll());
                realm.copyToRealm(tempRealm.where(Area.class).findAll());
                realm.copyToRealm(tempRealm.where(Gym.class).findAll());
                realm.copyToRealm(tempRealm.where(Goal.class).findAll());
            }finally {
                EventBus.getDefault().post(new RealmSyncEvent(REALM_DB_MERGED));
            }
        }
    }


}