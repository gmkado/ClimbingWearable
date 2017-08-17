package com.example.mysynclibrary.eventbus;


import com.example.mysynclibrary.realm.Climb;
import com.google.android.gms.wearable.DataEventBuffer;

import io.realm.RealmList;
import io.realm.RealmResults;

/**
 * This event is triggered when the realm result representing the climbs in the list fragment have changed
 */

public class ClimbResultChangedEvent {
    public RealmResults<Climb> climbs;

    public ClimbResultChangedEvent(RealmResults<Climb> climbs){
        this.climbs = climbs;
    }
}
