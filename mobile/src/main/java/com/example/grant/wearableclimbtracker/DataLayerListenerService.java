package com.example.grant.wearableclimbtracker;

import android.net.Uri;
import android.util.Log;

import com.example.mysynclibrary.Tools;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;

/**
 * Created by Grant on 9/30/2016.
 */

public class DataLayerListenerService extends WearableListenerService {
    private static final String TAG = "DataLayerService";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged:" + dataEvents);

        // freezes a volatile object into an immutable one
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);

        for (DataEvent event: events) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                if(item.getUri().getPath().equals(Tools.REALM_SYNC_PATH)) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

                    Log.d(TAG, "onDataChanged: got datachange -- count = " + dataMap.getInt(Tools.COUNT_KEY));
                }
            }

        }


    }
}
