package com.example.mysynclibrary.goalDAO;

import io.realm.Realm;

/**
 * Created by Grant on 6/3/2017.
 */

public class GoalOfGoalsDAO extends GoalDAO {
    public GoalOfGoalsDAO() {
        super();
    }

    @Override
    public int getCurrentProgress() {
        return 0;
    }

    @Override
    public int getTarget() {
        return 0;
    }

    @Override
    public String getSummary() {
        return "Complete all goals";
    }
}
