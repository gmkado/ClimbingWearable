package com.example.mysynclibrary.realm;

import com.example.mysynclibrary.BuildConfig;
import com.example.mysynclibrary.Shared;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.Index;
import io.realm.annotations.LinkingObjects;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

import static com.example.mysynclibrary.realm.ISyncableRealmObject.SyncState.DIRTY;

// Your model just have to extend RealmObject.
// This will inherit an annotation which produces proxy getters and setters for ALL fields.

@RealmClass
public class Climb extends RealmObject implements ISyncableRealmObject{
    private boolean onRemote = false;
    // All fields are by default persisted.
    @PrimaryKey private String id;
    @Index private int grade;
    private int type;
    private Date createdAt = new Date();

    @Index private int color; // integer representation of color
    private Gym gym;   // gym name
    private Area area; // area name
    private boolean isRemoved;  // has the climb been removed?
    private String notes;   // more distinguishing notes

    @LinkingObjects("climb")
    private final RealmResults<Attempt> attempts = null;

    private String syncState;
    private Date lastEdit;

    public Climb() {
        // NOTE: DON'T USE THIS CONSTRUCTOR!!!
        if(BuildConfig.DEBUG && id == null) {
            throw new AssertionError("Use parameterized constructor");
        }

    }
    public Climb(Shared.ClimbType type, Gym gym, Area area) {
        id = UUID.randomUUID().toString();
        grade = 0;
        color = -1;
        setSyncState(DIRTY);
        onRemote = false;
        this.type = type.ordinal();
        this.gym = gym;
        this.area = area;
    }

    public int getGrade() {
        return grade;
    }

    public void setGrade(int grade) {
        setSyncState(DIRTY);
        this.grade = grade;
    }

    public Shared.ClimbType getType() {
        return Shared.ClimbType.values()[type];
    }

    public void setType(Shared.ClimbType type) {
        setSyncState(DIRTY);
        this.type = type.ordinal();
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        setSyncState(DIRTY);
        this.color = color;
    }

    public Gym getGym() {
        return gym;
    }

    public void setGym(Gym gym) {
        setSyncState(DIRTY);
        this.gym = gym;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        setSyncState(DIRTY);
        this.area = area;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        setSyncState(DIRTY);
        this.notes = notes;
    }

    /********** helper functions for exporting to CSV *******************************/
    public static String[] getTitleRow() {
        return new String[] {"Date", "Type", "Grade"};
    }

    public String[] toStringArray() {
        Shared.ClimbType type = getType();
        // TODO: fix this to include new way of recording attempts -- return new String[] {getSendDate().toString(), type.title, type.grades.get(getGrade())};
        return null;
    }

    /*********** check validity of climb object *****************/
    public boolean isValidClimb() {
        // check all required fields;
        return true;
    }

    public boolean isRemoved() {
        return isRemoved;
    }

    public void setRemoved(boolean removed) {
        setSyncState(DIRTY);
        isRemoved = removed;
    }

    public RealmResults<Attempt> getAttempts() {
        return attempts;
    }

    public Date getDateCreated() {
        return createdAt;
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
            attempts.deleteAllFromRealm();
            //...
            // delete this object
            deleteFromRealm();
        } else {
            // mark for deletion
            setSyncState(SyncState.DELETE);
        }
    }
}