package com.example.mysynclibrary.realm;

import com.example.mysynclibrary.Shared;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.Index;
import io.realm.annotations.LinkingObjects;
import io.realm.annotations.PrimaryKey;

// Your model just have to extend RealmObject.
// This will inherit an annotation which produces proxy getters and setters for ALL fields.
public class Climb extends RealmObject {

    // All fields are by default persisted.
    @PrimaryKey private String id;
    @Index private int grade;
    private int type;
    private Date createdAt = new Date();

    @Index private int color; // integer representation of color
    @Index private String gym;   // gym name
    @Index private String area; // area name
    private boolean isRemoved;  // has the climb been removed?
    private String notes;   // more distinguishing notes

    @LinkingObjects("climb")
    private final RealmResults<Attempt> attempts = null;

    // sync fields
    private boolean delete;
    private Date lastedit;
    private boolean onwear;

    public boolean isOnwear() {
        return onwear;
    }

    public void setOnwear(boolean onwear) {
        this.onwear = onwear;
    }

    // Let your IDE generate getters and setters for you!
    // Or if you like you can even have public fields and no accessors! See Dog.java and Cat.java
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getLastedit() {
        return lastedit;
    }

    public void setLastedit(Date lastedit) {
        this.lastedit = lastedit;
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public int getGrade() {
        return grade;
    }

    public void setGrade(int grade) {
        this.grade = grade;
    }

    public Shared.ClimbType getType() {
        return Shared.ClimbType.values()[type];
    }

    public void setType(Shared.ClimbType type) {
        this.type = type.ordinal();
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getGym() {
        return gym;
    }

    public void setGym(String gym) {
        this.gym = gym;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
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
        isRemoved = removed;
    }

    public RealmResults<Attempt> getAttempts() {
        return attempts;
    }

    public Date getDateCreated() {
        return createdAt;
    }
}