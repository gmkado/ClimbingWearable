package com.example.mysynclibrary.realm;

import android.support.annotation.NonNull;

import com.example.mysynclibrary.Shared;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

// Your model just have to extend RealmObject.
// This will inherit an annotation which produces proxy getters and setters for ALL fields.
public class Area extends RealmObject implements ISyncableRealmObject{

    // All fields are by default persisted.
    private String name;
    private Gym gym;
    private int type;
    @PrimaryKey private String id;

    SyncState syncState;

    public Area(){
        // NOTE: DON'T USE THIS CONSTRUCTOR!!!
        id = UUID.randomUUID().toString();
        syncState = new SyncState();  // This unmanaged syncState will be saved when the object is saved to realm.
    }


    public Area(String name, AreaType type, Gym gym) {
        super();
        this.name = name;
        setType(type);
        this.gym = gym;
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

    public AreaType getType() {
        return AreaType.values()[type];
    }

    public void setType(AreaType type) {
        edited();
        this.type = type.ordinal();
    }

    public Gym getGym() {
        return gym;
    }

    public void setGym(Gym gym) {
        edited();
        this.gym = gym;
    }

    @NonNull
    public String getName() {
        edited();
        return name;
    }

    public void setName(String name) {
        edited();
        this.name = name;
    }

}