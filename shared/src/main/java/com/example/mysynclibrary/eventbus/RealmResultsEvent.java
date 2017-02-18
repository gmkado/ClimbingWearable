package com.example.mysynclibrary.eventbus;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.realm.Climb;

import io.realm.RealmResults;

/**
 * Created by Grant on 10/22/2016.
 */

public class RealmResultsEvent {
    public final RealmResults<Climb> realmResults;
    public final Shared.ClimbType climbType;

    public RealmResultsEvent(RealmResults<Climb> realmResults, Shared.ClimbType climbType){
        this.realmResults = realmResults;
        this.climbType = climbType;
    }
}
