package com.example.grant.wearableclimbtracker;

import android.net.Uri;
import android.util.Log;

import com.example.mysynclibrary.SyncHelper;
import com.example.mysynclibrary.eventbus.RealmSyncEvent;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import static com.example.mysynclibrary.SyncHelper.CLIENT_DB_PATH;
import static com.example.mysynclibrary.SyncHelper.DB_KEY;
import static com.example.mysynclibrary.SyncHelper.TEMP_REALM_NAME;

public class MobileDataLayerService extends WearableListenerService {
    private static final String TAG = "MobileDataLayerService";

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.d(TAG, "onDataChanged");

        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEventBuffer);
        dataEventBuffer.release();
        for (DataEvent event : events) {
            Uri uri = event.getDataItem().getUri();
            String path = uri.getPath();
            if (CLIENT_DB_PATH.equals(path)) {
                Log.d(TAG, "Database change received");
                DataMapItem item = DataMapItem.fromDataItem(event.getDataItem());
                byte[] realmAsset = item.getDataMap().getByteArray(DB_KEY);
                if(realmAsset != null){
                    if(SyncHelper.saveRealmToFile(this, realmAsset, TEMP_REALM_NAME)) {
                        Log.d(TAG, "Remote saved to temp");
                        EventBus.getDefault().postSticky(new RealmSyncEvent(RealmSyncEvent.SyncProcessStep.REMOTE_SAVED_TO_TEMP));
                    }
                }
            }
        }
    }



    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived");  // this is just a check if service is working but ondatachanged isn't
        // do nothing
    }
}
