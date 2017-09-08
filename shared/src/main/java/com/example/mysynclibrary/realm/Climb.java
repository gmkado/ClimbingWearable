package com.example.mysynclibrary.realm;

import com.example.mysynclibrary.Shared;

import java.util.Date;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.Index;
import io.realm.annotations.LinkingObjects;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

// Your model just have to extend RealmObject.
// This will inherit an annotation which produces proxy getters and setters for ALL fields.

@RealmClass
public class Climb extends RealmObject implements ISyncableRealmObject{
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

    private SyncState syncState;

    public Climb() {
        // NOTE: DON'T USE THIS CONSTRUCTOR!!!
        id = UUID.randomUUID().toString();
        grade = 0;
        color = -1;
        syncState = new SyncState();
    }
    public Climb(Shared.ClimbType type, Gym gym, Area area) {
        super();
        this.type = type.ordinal();
        this.gym = gym;
        this.area = area;
    }

    public int getGrade() {
        return grade;
    }

    public void setGrade(int grade) {
        edited();
        this.grade = grade;
    }

    public Shared.ClimbType getType() {
        return Shared.ClimbType.values()[type];
    }

    public void setType(Shared.ClimbType type) {
        edited();
        this.type = type.ordinal();
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        edited();
        this.color = color;
    }

    public Gym getGym() {
        return gym;
    }

    public void setGym(Gym gym) {
        this.gym = gym;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        edited();
        this.area = area;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        edited();
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
        edited();
        isRemoved = removed;
    }

    public RealmResults<Attempt> getAttempts() {
        return attempts;
    }

    public Date getDateCreated() {
        return createdAt;
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