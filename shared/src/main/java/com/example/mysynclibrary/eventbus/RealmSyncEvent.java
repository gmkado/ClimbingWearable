package com.example.mysynclibrary.eventbus;


/**
 * Created by Grant on 9/7/2017
 * Called when database is done syncing
 */

public class RealmSyncEvent {
    public final SyncProcessStep step;

    public enum SyncProcessStep{
        // Denotes the step of the sync process
        SYNC_REQUESTED,
        REMOTE_SAVED_TO_TEMP,
        REALM_DB_MERGED,
    }

    public RealmSyncEvent(SyncProcessStep step) {
        this.step = step;
    }

}
