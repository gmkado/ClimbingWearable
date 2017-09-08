package com.example.mysynclibrary.realm;

import android.support.annotation.NonNull;

import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmObject;

public interface ISyncableRealmObject<T> {
    /* I'm using composition over inheritance design pattern: https://stackoverflow.com/questions/31281642/sharing-realm-fields-on-android
        - Each realmobject that needs to be synced implements ISyncableRealmObject and contains a SyncState field (the component), that also implements this interface
        - When any sync-related queries are needed, the query is made against the SyncState object
        - All sync-related logic is implemented in SyncState, removing any repeated code

        NOTE: RealmObjects in this application should never use createObject, but should use constructors that allow the appropriate default fields to be set
        FIXME: is there a way to force this behavior?
     */
    void edited(); // object has been edited, so mark as dirty and set last edited date
    void synced(); // object has been synced, so mark as onremote and not dirty
    void safeDelete(); // delete object if its safe to do so, otherwise mark for deletion


    boolean isOnRemote();
    boolean isDelete();
    Date getLastEdit();


    String getId();
}