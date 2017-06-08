package com.example.mysynclibrary.goalDAO;

import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.realm.Goal;

import io.realm.RealmResults;

/**
 * Created by Grant on 6/3/2017.
 */

public class ProjectGoalDAO extends GoalDAO{
    public ProjectGoalDAO(){
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
        return "Complete all projects";
    }

}
