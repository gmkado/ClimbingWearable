package com.example.mysynclibrary.realm;

import java.util.Calendar;
import java.util.Date;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;

/**
 * Created by Grant on 9/5/2017.
 */

public class SyncState extends RealmObject implements ISyncableRealmObject {
    // FIXME: ideally this would be implemented in a baseobject, but realm doesn't support inheritance -> https://github.com/realm/realm-java/issues/761
    private boolean dirty;
    private Date lastedit;
    private boolean onRemote;
    private boolean delete;

    public SyncState() {
        // constructor, set default values
        dirty = false;
        lastedit = now();
        onRemote = false;
        delete = false;

    }

    private Date now() {
        return Calendar.getInstance().getTime();
    }

    @Override
    public void edited() {
        lastedit = now();
        dirty = true;
    }

    @Override
    public void synced() {
        dirty = false;
        onRemote = true;
    }

    @Override
    public void safeDelete() {
        // NOTE: do nothing, this method is for realmobjects implementing the interface
        // NOTE: For realmobjects, call syncState.safeDelete(this) which will check if it is okay to delete this object before doing so
    }

    @Override
    public boolean isOnRemote() {
        return onRemote;
    }

    @Override
    public boolean isDelete() {
        return delete;
    }

    @Override
    public Date getLastEdit() {
        return lastedit;
    }

    @Override
    public String getId() {
        // NOTE: this method is for realmobjects implementing the interface
        return null;
    }

    public void safeDelete(RealmObject object) {
        if(delete || !onRemote) { // if already marked for deletion or not on remote, we can safely delete the object
            object.deleteFromRealm();
        }else {
            delete = true;
        }
    }

    public static <T extends RealmObject & ISyncableRealmObject> void safeDelete(RealmResults<T> list) {
        // This is used to safely delete all objects in a realmresult
        for (T object:list) {
            object.safeDelete();
        }
    }
}
