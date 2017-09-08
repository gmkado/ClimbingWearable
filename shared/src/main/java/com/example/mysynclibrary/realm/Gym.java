package com.example.mysynclibrary.realm;

import android.support.annotation.NonNull;

import java.util.Date;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.LinkingObjects;
import io.realm.annotations.PrimaryKey;

// Your model just have to extend RealmObject.
// This will inherit an annotation which produces proxy getters and setters for ALL fields.
public class Gym extends RealmObject implements ISyncableRealmObject{

    // All fields are by default persisted.
    private String name;
    @PrimaryKey private String id;
    @LinkingObjects("gym")
    private final RealmResults<Area> areas = null;

    SyncState syncState;

    public Gym() {
        // NOTE: DON'T USE THIS CONSTRUCTOR!!!
        id = UUID.randomUUID().toString();

        syncState = new SyncState();
    }

    public Gym(String name) {
        super();
        this.name = name;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(String name) {
        edited();
        this.name = name;
    }

    public RealmResults<Area> getAreas() {
        return areas;
    }

    @Override
    public void edited() {
        syncState.edited();
    }

    @Override
    public void synced() {
        syncState.synced();
    }

    @Override
    public void safeDelete() {
        syncState.safeDelete(this);
    }
    @Override
    public boolean isOnRemote() {
        return syncState.isOnRemote();
    }

    @Override
    public boolean isDelete() {
        return syncState.isDelete();
    }

    @Override
    public Date getLastEdit() {
        return syncState.getLastEdit();
    }

    @Override
    public void setDirty(boolean dirty) {
        syncState.setDirty(dirty);
    }

    @Override
    public String getId() {
        return id;
    }
}