package com.example.mysynclibrary.realm;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

// Your model just have to extend RealmObject.
// This will inherit an annotation which produces proxy getters and setters for ALL fields.
public class Attempt extends RealmObject implements ISyncableRealmObject{

    // All fields are by default persisted.
    private boolean isSend;
    private boolean onLead;
    private Climb climb;
    private Date datetime;
    @Index private String date;
    private int count;          // number of attempts saved in this object
    private float progress;  // percent done, 0-100
    @PrimaryKey private String id;

    SyncState syncState;

    public Attempt() {
        //NOTE: DON'T USE THIS, USE CREATOR METHODS INSTEAD
        // set defaults
        id = UUID.randomUUID().toString();
        setDate(Calendar.getInstance().getTime());
        syncState = new SyncState();
    }

    public Attempt(Climb climb, float progress, int count, boolean isSend, boolean isLead) {
        //NOTE: DON'T USE THIS, USE CREATOR METHODS INSTEAD
        super();
        this.climb = climb;
        this.count = count;
        this.isSend = isSend;
        this.setProgress(progress);
    }

    public static Attempt createSend(Climb climb, boolean isLead) {
        // TODO: add check if climb is unmanaged?
        return new Attempt(climb, 100, 1, true, isLead);
    }

    public static Attempt createAttempt(Climb climb, float progress, int count, boolean isLead) {
        return new Attempt(climb, progress, count, false, isLead);
    }

    public boolean isSend() {
        return isSend;
    }

    public void setSend(boolean send) {
        edited();
        isSend = send;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        edited();
        this.count = count;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        edited();
        if(progress > 100) {
            progress = 1;
        }else if(progress < 0) {
            progress = 0;
        }
        this.progress = progress;
    }

    public String getDate() {
        return date;
    }

    public void setDate(Date date) {
        edited();
        this.datetime = date;

        // TODO: is this robust for different timezones?
        DateFormat sdf = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG);
        this.date = sdf.format(date);
    }

    public Date getDatetime() {
        return datetime;
    }

    public Climb getClimb() {
        return climb;
    }

    public void setClimb(Climb climb) {
        edited();
        this.climb = climb;
    }

    public boolean isOnLead() {
        return onLead;
    }

    public void setOnLead(boolean onLead) {
        edited();
        this.onLead = onLead;
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