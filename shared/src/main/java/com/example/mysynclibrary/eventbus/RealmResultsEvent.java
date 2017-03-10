package com.example.mysynclibrary.eventbus;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.realm.Climb;

import org.threeten.bp.temporal.ChronoUnit;

import io.realm.RealmResults;

/**
 * Created by Grant on 10/22/2016.
 */

public class RealmResultsEvent {
    public final RealmResults<Climb> realmResults;
    public final Shared.ClimbType climbType;
    public ChronoUnit dateRange;
    public int dateOffset;

    public RealmResultsEvent(RealmResults<Climb> realmResults, Shared.ClimbType climbType, ChronoUnit dateRange, int dateOffset){
        this.realmResults = realmResults;
        this.climbType = climbType;
        this.dateRange = dateRange;
        this.dateOffset = dateOffset;
    }
}
