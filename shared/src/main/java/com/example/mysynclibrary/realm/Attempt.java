package com.example.mysynclibrary.realm;

import com.example.mysynclibrary.Shared;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

// Your model just have to extend RealmObject.
// This will inherit an annotation which produces proxy getters and setters for ALL fields.
public class Attempt extends RealmObject {

    // All fields are by default persisted.
    private boolean isSend;
    private Climb climb;
    private Date datetime;
    @Index private String date;

    public void setDate(Date date) {
        this.datetime = date;

        // TODO: is this robust for different timezones?
        DateFormat sdf = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG);
        this.date = sdf.format(date);
    }


    public String getDate() {
        return date;
    }

    public Date getDatetime() {
        return datetime;
    }

    public Climb getClimb() {
        return climb;
    }
}