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
    public ClimbStats climbstats;
    public final Shared.ClimbType climbType;
    public ChronoUnit dateRange;
    public int dateOffset;

    public RealmResultsEvent(ClimbStats stat, int dateOffset){
        this.climbstats = stat;
        this.climbType = stat.getClimbType();
        this.dateRange = stat.getDateRange();
        this.dateOffset = dateOffset;
    }
}
