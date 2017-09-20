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

import static com.example.mysynclibrary.realm.ISyncableRealmObject.SyncState.DIRTY;

// Your model just have to extend RealmObject.
// This will inherit an annotation which produces proxy getters and setters for ALL fields.
public class Area extends RealmObject implements ISyncableRealmObject{

    String syncState;
    // All fields are by default persisted.
    private String name;
    private Gym gym;
    private int type;
    @PrimaryKey private String id;
    private Date lastEdit;
    private boolean onRemote;


    public Area(){
        // NOTE: DON'T USE THIS CONSTRUCTOR!!!
        if(BuildConfig.DEBUG && id == null) {
            throw new AssertionError("Use parameterized constructor");
        }
    }


    public Area(String name, AreaType type, Gym gym) {
        id = UUID.randomUUID().toString();
        this.name = name;
        setSyncState(DIRTY);
        onRemote = false;
        setType(type);
        this.gym = gym;
    }

    @Override
    public void safedelete(boolean forceDeletion) {
        // NOTE: this should only be called in a transaction
        if(forceDeletion || !onRemote){
            // delete all child objects
            //...
            // delete this object
            deleteFromRealm();
        } else {
            // mark for deletion
            setSyncState(SyncState.DELETE);
        }
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

    public AreaType getType() {
        return AreaType.values()[type];
    }

    public void setType(AreaType type) {
        setSyncState(DIRTY);
        this.type = type.ordinal();
    }

    public Gym getGym() {
        return gym;
    }

    public void setGym(Gym gym) {
        setSyncState(DIRTY);
        this.gym = gym;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(String name) {
        setSyncState(DIRTY);
        this.name = name;
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