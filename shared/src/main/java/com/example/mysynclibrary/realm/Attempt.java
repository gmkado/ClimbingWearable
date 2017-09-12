package com.example.mysynclibrary.realm;

import com.example.mysynclibrary.BuildConfig;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

import static com.example.mysynclibrary.realm.ISyncableRealmObject.SyncState.DIRTY;

// Your model just have to extend RealmObject.
// This will inherit an annotation which produces proxy getters and setters for ALL fields.
public class Attempt extends RealmObject implements ISyncableRealmObject{

    String syncState;
    // All fields are by default persisted.
    private boolean isSend;
    private boolean onLead;
    private Climb climb;
    private Date datetime;
    @Index private String date;
    private int count;          // number of attempts saved in this object
    private float progress;  // percent done, 0-100
    @PrimaryKey private String id;
    private boolean onRemote;
    private Date lastEdit;

    public Attempt() {
        // NOTE: DON'T USE THIS CONSTRUCTOR!!!
        if(BuildConfig.DEBUG && id == null) {
            throw new AssertionError("Use parameterized constructor");
        }
    }

    public Attempt(Climb climb, float progress, int count, boolean isSend, boolean isLead) {
        //NOTE: Use creators to build attempts without all the parameters
        id = UUID.randomUUID().toString();
        setDate(Calendar.getInstance().getTime());
        setSyncState(DIRTY);
        this.onRemote = false;

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
        setSyncState(DIRTY);
        isSend = send;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        setSyncState(DIRTY);
        this.count = count;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        setSyncState(DIRTY);
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
        setSyncState(DIRTY);
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

        setSyncState(DIRTY);
        this.climb = climb;
    }

    public boolean isOnLead() {
        return onLead;
    }

    public void setOnLead(boolean onLead) {
        setSyncState(DIRTY);
        this.onLead = onLead;
    }


    @Override
    public void safedelete(boolean forceDeletion) {
        // NOTE: this should only be called in a transaction
        if(forceDeletion || !isOnRemote()){
            // delete all child objects

            //...
            // delete this object
            deleteFromRealm();
        } else {
            // mark for deletion
            setSyncState(SyncState.DELETE);
        }
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
}