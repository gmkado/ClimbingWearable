package com.example.mysynclibrary.eventbus;


import com.google.android.gms.wearable.DataEventBuffer;

/**
 * Created by Grant on 10/22/2016.
 * this utilizes EventBus to get around the issues discussed here: http://stackoverflow.com/questions/25987014/messageapi-messagelistener-in-wear-activity-not-triggering
 * MessageListener wasn't working, so we need to create a WearableListenerService and forward events through the eventbus to listeners
 */

public class WearDataEvent {
    public DataEventBuffer dataEvents;
            
    public WearDataEvent(DataEventBuffer dataEvents){
        this.dataEvents = dataEvents;
    }
}
