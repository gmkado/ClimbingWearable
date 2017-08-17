package com.example.mysynclibrary.realm;

import android.support.annotation.NonNull;

import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.LinkingObjects;
import io.realm.annotations.PrimaryKey;

// Your model just have to extend RealmObject.
// This will inherit an annotation which produces proxy getters and setters for ALL fields.
public class Gym extends RealmObject {

    // All fields are by default persisted.
    private String name;
    @PrimaryKey private String id;
    @LinkingObjects("gym")
    private final RealmResults<Area> areas = null;

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RealmResults<Area> getAreas() {
        return areas;
    }
}