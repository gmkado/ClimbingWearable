package com.example.mysynclibrary.goalDAO;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.realm.Goal;

import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmModel;
import io.realm.RealmQuery;
import io.realm.RealmResults;

/**
 * Created by Grant on 6/3/2017.
 */

public class CustomGoalDAO extends GoalDAO {
    /**
     * Here's a description of how goals are structured
     * Goals can be of these types:
     *      -
     */
    private RealmResults<Climb> mResults;
    private Goal mGoal;

    public CustomGoalDAO(Goal goal) {
        super();
        mGoal = goal;

        // TODO: does this need to be removed somewhere?  If I have a RecyclerView.Adapter wrap around a list of GoalDAO's,
        // they should be strong references and not removed unless the entire fragment is destroyed
        goal.addChangeListener(new RealmChangeListener<RealmModel>() {
            @Override
            public void onChange(RealmModel element) {
                // rerun the query
                findResultForGoal();
            }
        });

        findResultForGoal();
    }

    @Override
    public int getCurrentProgress() {
        return 0;
    }

    @Override
    public int getTarget() {
        return mGoal.getTarget();
    }

    @Override
    public String getSummary() {
        return mGoal.getSummary();
    }


    /**
     * Run a query for the given goal and calculate the result
     */
    private void findResultForGoal() {
        try (Realm realm = Realm.getDefaultInstance()) {
            // No need to close the Realm instance manually since this is wrapped in try statement
            // https://realm.io/docs/java/latest/#closing-realm-instances

            // TODO: should this be done async?
            RealmQuery query = realm.where(Climb.class)
                    .greaterThanOrEqualTo("date", mGoal.getStartDate())
                    .equalTo("type", mGoal.getClimbType().ordinal());

            switch (mGoal.getEndType()) {
                case NO_END:
                    break;
                case DATE:
                    query.lessThanOrEqualTo("date", mGoal.getEndDate());
                    break;
                case PERIOD:
                    if(mGoal.getPeriod()== Goal.Period.SESSION) {
                        // query for distinct session dates and add constraint to query for "numperiod" sessions
                        RealmResults<Climb> results = realm.where(Climb.class).distinct("sessionDate");
                        List<Climb> list = results.sort("date").subList(0, mGoal.getNumPeriods());
                        query.lessThanOrEqualTo("date",list.get(list.size()-1).getDate());
                    }else {
                        // TODO: we should do this in goal to keep track of enddate
                        ChronoUnit unit;
                        switch (mGoal.getPeriod()) {
                            case WEEKLY:
                                unit = ChronoUnit.WEEKS;
                                break;
                            case MONTHLY:
                                unit = ChronoUnit.MONTHS;
                                break;
                            case YEARLY:
                                unit = ChronoUnit.YEARS;
                                break;
                            default:
                                throw new IllegalArgumentException("Got invalid goal period");
                        }
                        // add "numperiod" increments of "unit" and add constraint to query
                        ZonedDateTime zdt = Shared.DateToZDT(mGoal.getStartDate());
                        zdt.plus(mGoal.getNumPeriods(), unit);
                        query.lessThanOrEqualTo("date", Shared.ZDTToDate(zdt));
                    }
                    break;
            }
            mResults = query.findAll();

            /**
             * TODO: How to structure this?
             *  - have a variable keeping track of end-date for subquery
             *      - NO_END                -> end-date = goal end date
             *      - DATE + not recurring  -> end-date = goal end date
             *      - DATE + recurring      -> end-date = period end date
             *      - PERIOD                -> end-date = period end date
             *  - if end-date is >= goal end-date (never for NO_END), add each point as ScatterEntry (grade) or LineEntry (points/climbs/height)
             *  - otherwise, just get that period's total and add to BarEntry
             *
             * At the end of this routine, should have all chart datasets and progress for
             *  - current progress
             *  - historical progress (for recurring only)
             *      - % target reached
             *      - # of period
             *  - time left (not relevant for NO_END)
             */


            switch (mGoal.getGoalUnit()) {

            }
        }
    }
}
