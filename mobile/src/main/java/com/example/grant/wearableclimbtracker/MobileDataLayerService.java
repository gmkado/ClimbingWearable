package com.example.grant.wearableclimbtracker;

import android.util.Log;

import com.example.mysynclibrary.eventbus.WearDataEvent;
import com.example.mysynclibrary.eventbus.WearMessageEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.greenrobot.eventbus.EventBus;

public class MobileDataLayerService extends WearableListenerService {
    private static final String TAG = "MobileDataLayerService";

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.d(TAG, "onDataChanged");

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived");  // this is just a check if service is working but ondatachanged isn't
        EventBus.getDefault().postSticky(new WearMessageEvent(messageEvent)); // send it to ALL subscribers. post sticky so this result stays until we set it again
    }
}
