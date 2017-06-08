package com.example.mysynclibrary.eventbus;

import com.example.mysynclibrary.ClimbStats;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.realm.Climb;

import org.threeten.bp.temporal.ChronoUnit;

import io.realm.RealmResults;

/**
 * Created by Grant on 10/22/2016.
 */

public class RealmResultsEvent {
    public RealmResults<Climb> mResult;

    public RealmResultsEvent(RealmResults<Climb> result){
        this.mResult = result;
    }
}
