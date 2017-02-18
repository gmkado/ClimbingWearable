package com.example.grant.wearableclimbtracker;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.example.mysynclibrary.eventbus.WearDataEvent;
import com.example.mysynclibrary.eventbus.WearMessageEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.WearableListenerService;

import org.greenrobot.eventbus.EventBus;

public class MobileDataLayerService extends WearableListenerService {
    private static final String TAG = "MobileDataLayerService";

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.d(TAG, "onDataChanged");
        EventBus.getDefault().postSticky(new WearDataEvent(dataEventBuffer)); // send it to ALL subscribers. post sticky so this result stays until we set it again
    }
}
