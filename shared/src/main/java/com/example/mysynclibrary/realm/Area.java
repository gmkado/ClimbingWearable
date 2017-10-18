package com.example.mysynclibrary.realm;


import android.support.annotation.NonNull;

import com.example.mysynclibrary.BuildConfig;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;


// Your model just have to extend RealmObject.
// This will inherit an annotation which produces proxy getters and setters for ALL fields.
public class Area extends RealmObject{

    // All fields are by default persisted.
    private String name;
    private Gym gym;
    private int type;
    @PrimaryKey private String id;


    public Area(){
        // NOTE: DON'T USE THIS CONSTRUCTOR!!!
        if(BuildConfig.DEBUG && id == null) {
            throw new AssertionError("Use parameterized constructor");
        }
    }


    public Area(String name, AreaType type, Gym gym) {
        id = UUID.randomUUID().toString();
        this.name = name;
        setType(type);
        this.gym = gym;
    }


    public AreaType getType() {
        return AreaType.values()[type];
    }

    public void setType(AreaType type) {
        this.type = type.ordinal();
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

    public String getId() {
        return id;
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

        public static boolean matches(AreaType type, Climb.ClimbType climbType) {
            // return whether the areatype matches the climbtype
            if(type == MIXED) {
                return true;
            }else {
                return type == (climbType == Climb.ClimbType.bouldering ? AreaType.BOULDER_ONLY : AreaType.ROPES_ONLY);
            }
        }

        public String getTitle() {
            return title;
        }
    }

}