package com.example.mysynclibrary.realm;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

// Your model just have to extend RealmObject.
// This will inherit an annotation which produces proxy getters and setters for ALL fields.
public class Climb extends RealmObject {

    // All fields are by default persisted.
    private int grade;
    private int type;
    @Required private Date date;
    @PrimaryKey private String id;
    private boolean dirty;
    private boolean delete;
    private boolean onwear;
    private Date lastedit;

    // Let your IDE generate getters and setters for you!
    // Or if you like you can even have public fields and no accessors! See Dog.java and Cat.java
    public String getId() {
        return id;
    }

    public Date getLastedit() {
        return lastedit;
    }

    public void setLastedit(Date lastedit) {
        this.lastedit = lastedit;
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean isOnwear() {
        return onwear;
    }

    public void setOnwear(boolean onwear) {
        this.onwear = onwear;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getGrade() {
        return grade;
    }

    public void setGrade(int grade) {
        this.grade = grade;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Date getDate() {
        return date;
    }


    public void setDate(Date date) {
        this.date = date;
    }


}