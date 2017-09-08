package com.example.mysynclibrary.eventbus;


import com.example.mysynclibrary.realm.SyncState;

import java.security.InvalidParameterException;

import io.realm.RealmObject;

/**
 * Created by Grant on 9/7/2017
 * Called when database is done syncing
 */

public class RealmSyncEvent {
    public final SyncProcessStep step;
    public SyncObjectBit bit;

    public enum SyncObjectBit{
        climb,
        attempt,
        gym,
        area,
        goal;
    }

    public enum SyncProcessStep{
        // Denotes the step of the sync process
        SYNC_REQUESTED,
        REMOTE_SAVED_TO_TEMP,
        REALM_DB_MERGED,
        REALM_OBJECT_MERGED;
    }

    public RealmSyncEvent(SyncProcessStep step) {
        if(step == SyncProcessStep.REALM_OBJECT_MERGED) {
            throw new InvalidParameterException("Wrong constructor");
        }
        this.step = step;
    }

    public RealmSyncEvent(SyncProcessStep step, SyncObjectBit bit) {
        if(step != SyncProcessStep.REALM_OBJECT_MERGED) {
            throw new InvalidParameterException("Wrong constructor");
        }
        this.bit = bit;
        this.step = step;
    }
}
