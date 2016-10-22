package com.example.mysynclibrary.model;

import com.example.mysynclibrary.model.Climb;

import io.realm.RealmResults;

/**
 * Created by Grant on 10/22/2016.
 */

public class RealmResultsEvent {
    public final RealmResults<Climb> realmResults;

    public RealmResultsEvent(RealmResults<Climb> realmResults){
        this.realmResults = realmResults;
    }
}
