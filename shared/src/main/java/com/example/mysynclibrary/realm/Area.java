package com.example.mysynclibrary.realm;

import android.support.annotation.NonNull;

import com.example.mysynclibrary.Shared;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

// Your model just have to extend RealmObject.
// This will inherit an annotation which produces proxy getters and setters for ALL fields.
public class Area extends RealmObject {

    // All fields are by default persisted.
    private String name;
    private Gym gym;
    private int type;
    @PrimaryKey private String id;

    public AreaType getType() {
        return AreaType.values()[type];
    }

    public void setType(AreaType type) {
        this.type = type.ordinal();
    }

    public enum AreaType{
        MIXED ("Mixed"),
        ROPES_ONLY ("Ropes Only"),
        BOULDER_ONLY ("Boulder Only");

        private String title;

        AreaType(String title) {
            this.title = title;
        }

        public static List<String> getStringArray() {
            ArrayList<String> list = new ArrayList<>();
            for (AreaType type : AreaType.values()) {
                list.add(type.getTitle());
            }
            return list;
        }

        public String getTitle() {
            return title;
        }

        public static boolean matches(AreaType type, Shared.ClimbType climbType) {
            // return whether the areatype matches the climbtype
            if(type == MIXED) {
                return true;
            }else {
                return type == (climbType == Shared.ClimbType.bouldering ? AreaType.BOULDER_ONLY : AreaType.ROPES_ONLY);
            }
        }
    }

    public String getId() {
        return id;
    }

    public Gym getGym() {
        return gym;
    }

    public void setGym(Gym gym) {
        this.gym = gym;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    }