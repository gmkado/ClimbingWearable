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
     */

    /* NOTE: this needs to take care of cascading deletes:
    https://github.com/realm/realm-core/issues/746
    https://github.com/realm/realm-java/issues/1104
    Current solution: https://github.com/realm/realm-java/issues/2717#issuecomment-255973863
    */
    void safedelete(boolean forceDeletion); // object has been deleted, so mark for deletion or delete if not on remote,

    boolean isOnRemote();
    void setOnRemote(boolean onRemote);
    SyncState getSyncState();
    void setSyncState(SyncState state);

    Date getLastEdit();
    String getId();


    enum SyncState{
        CLEAN,
        DIRTY,
        DELETE
    }
}