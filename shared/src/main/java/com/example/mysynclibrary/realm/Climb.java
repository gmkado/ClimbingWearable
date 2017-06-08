package com.example.mysynclibrary.realm;

import com.example.mysynclibrary.Shared;

import org.threeten.bp.temporal.ChronoUnit;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

// Your model just have to extend RealmObject.
// This will inherit an annotation which produces proxy getters and setters for ALL fields.
public class Climb extends RealmObject {

    // All fields are by default persisted.
    @Index private int grade;
    private int type;
    @Required private Date date;  // datetime of send
    @Required private String sessionDate;  // truncated date for distinct search, can only use string or int
    @PrimaryKey private String id;

    // project fields
    // TODO: fall detection https://github.com/BharadwajS/Fall-detection-in-Android/blob/master/Android/SenseFall/SenseFall/src/com/example/sensefall/DisplayMessageActivity.java
    // - http://stackoverflow.com/questions/22093572/android-sensor-listening-when-app-in-background
    // - http://stackoverflow.com/questions/4848490/android-how-to-approach-fall-detection-algorithm
    /*private boolean project; // is a project
    private int color; // integer representation of color
    private String area; // area name
    private Date setdate;  // datetime of set
    private boolean completed;
    private int attempts;*/

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

        // TODO: is this robust?
        DateFormat sdf = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG);
        this.sessionDate = sdf.format(ChronoUnit.DAYS);
    }


    /********** helper functions for exporting to CSV *******************************/
    public static String[] getTitleRow() {
        return new String[] {"Date", "Type", "Grade"};
    }

    public String[] toStringArray() {
        Shared.ClimbType type = Shared.ClimbType.values()[getType()];
        return new String[] {getDate().toString(), type.title, type.grades.get(getGrade())};
    }
}