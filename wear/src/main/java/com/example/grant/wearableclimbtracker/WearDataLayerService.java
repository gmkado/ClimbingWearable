package com.example.grant.wearableclimbtracker;

import android.util.Log;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.eventbus.RealmResultsEvent;
import com.example.mysynclibrary.eventbus.WearMessageEvent;
import com.example.mysynclibrary.realm.Climb;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.greenrobot.eventbus.EventBus;

import io.realm.Realm;
import io.realm.RealmResults;

public class WearDataLayerService extends WearableListenerService {
    private static final String TAG = "WearDataLayerService";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived");
        EventBus.getDefault().postSticky(new WearMessageEvent(messageEvent)); // send it to ALL subscribers. post sticky so this result stays until we set it again
    }
}

