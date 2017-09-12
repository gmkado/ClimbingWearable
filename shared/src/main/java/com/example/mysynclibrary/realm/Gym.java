package com.example.mysynclibrary.realm;

import android.support.annotation.NonNull;

import com.example.mysynclibrary.BuildConfig;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.LinkingObjects;
import io.realm.annotations.PrimaryKey;

import static com.example.mysynclibrary.realm.ISyncableRealmObject.SyncState.DIRTY;

// Your model just have to extend RealmObject.
// This will inherit an annotation which produces proxy getters and setters for ALL fields.
public class Gym extends RealmObject implements ISyncableRealmObject{

    // All fields are by default persisted.
    private String name;
    @PrimaryKey private String id;
    @LinkingObjects("gym")
    private final RealmResults<Area> areas = null;
    //private String dummy; // use this to clear database
    String syncState;
    private Date lastEdit;
    private boolean onRemote;

    public Gym() {

        // NOTE: DON'T USE THIS CONSTRUCTOR!!!
        if(BuildConfig.DEBUG && id == null) {
            throw new AssertionError("Use parameterized constructor");
        }
    }

    public Gym(String name) {
        id = UUID.randomUUID().toString();
        setSyncState(DIRTY);
        this.name = name;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(String name) {
        setSyncState(DIRTY);
        this.name = name;
    }

    public RealmResults<Area> getAreas() {
        return areas;
    }

    @Override
    public boolean isOnRemote() {
        return onRemote;
    }

    @Override
    public void setOnRemote(boolean onRemote) {
        this.onRemote = onRemote;
    }

    @Override
    public SyncState getSyncState() {
        return (syncState !=null) ? SyncState.valueOf(syncState):null;
    }

    @Override
    public void setSyncState(SyncState state) {
        if(state == DIRTY) {
            lastEdit = Calendar.getInstance().getTime();
        }
        this.syncState = state.name();
    }

    @Override
    public Date getLastEdit() {
        return lastEdit;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void safedelete(boolean forceDeletion) {
        // NOTE: this should only be called in a transaction
        if(forceDeletion || !isOnRemote()){
            // delete all child objects
            areas.deleteAllFromRealm();
            //...
            // delete this object
            deleteFromRealm();
        } else {
            // mark for deletion
            setSyncState(SyncState.DELETE);
        }
    }
}