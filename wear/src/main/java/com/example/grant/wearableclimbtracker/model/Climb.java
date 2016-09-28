package com.example.grant.wearableclimbtracker.model;

import android.support.annotation.RequiresPermission;

import com.example.grant.wearableclimbtracker.MainActivity;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

// Your model just have to extend RealmObject.
// This will inherit an annotation which produces proxy getters and setters for all fields.
public class Climb extends RealmObject {

    // All fields are by default persisted.
    private int grade;
    private int type;
    @Required private Date date;
    @PrimaryKey private String id;


    // Let your IDE generate getters and setters for you!
    // Or if you like you can even have public fields and no accessors! See Dog.java and Cat.java
    public String getId() {
        return id;
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

    public void setType(MainActivity.ClimbType type) {
        this.type = type.ordinal();
    }

    public Date getDate() {
        return date;
    }


    public void setDate(Date date) {
        this.date = date;
    }
}