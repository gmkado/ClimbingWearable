package com.example.mysynclibrary.goalDAO;

import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.realm.Goal;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmModel;
import io.realm.RealmResults;

/**
 * Created by Grant on 6/1/2017.
 * This class is a wrapper for Goal RealmObjects.  It abstracts the process of querying for the appropriate climbs
 */

public abstract class GoalDAO {

    public GoalDAO() {}

    /**
     *
     * @return the current value for the progress bar
     */
    public abstract int getCurrentProgress();


    /**
     *
     * @return the target for the progress bar
     */
    public abstract int getTarget();

    public abstract String getSummary();
}
