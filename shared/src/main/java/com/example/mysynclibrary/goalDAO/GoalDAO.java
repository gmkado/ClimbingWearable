package com.example.mysynclibrary.goalDAO;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.realm.Attempt;
import com.example.mysynclibrary.realm.Goal;

import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.temporal.ChronoUnit;
import org.threeten.bp.temporal.WeekFields;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TreeMap;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;


/**
 * Created by Grant on 6/3/2017.
 */

public class GoalDAO {
    GoalDAOListener listener;
    private Goal mGoal;
    private TreeMap<Date, RealmResults<Attempt>> mPeriodResultMap;
    private RealmResults<Attempt> mFullRangeResults;
    private Date mStartDate;
    private Date mEndDate;

    public GoalDAO(Goal goal) {
        super();
        mGoal = goal; // NOTE: we don't have a changelistener on goal because anytime the goals change, GoalListFragment repopulates the list anyways
        listener = null;

        getStartAndEndDate();
        getRealmResult();
        if(mGoal.isRecurring()) {
            populateGoalMap();
        }
    }

    public TreeMap<Date, RealmResults<Attempt>> getPeriodResultMap() {
        return mPeriodResultMap;
    }

    public RealmResults<Attempt> getFullRangeResults() {
        return mFullRangeResults;
    }

    public Goal getGoal() {
        return mGoal;
    }


    private void getStartAndEndDate() {
        switch(mGoal.getEndType()) {
            case NEVER:
                mStartDate = mGoal.getStartDate();
                mEndDate = null;
                break;
            case DATE:
                mStartDate = mGoal.getStartDate();
                mEndDate = mGoal.getEndDate();
                break;
            case PERIOD:
                if(mGoal.getPeriod() == Goal.Period.SESSION) {
                    evaluateSessionPeriod();
                }else {
                    mStartDate = mGoal.getStartDate();
                    mEndDate = getIncrementedDate(getIncrementedDate(mStartDate, mGoal.getPeriod().unit, mGoal.getNumPeriods()+1), ChronoUnit.DAYS, -1); // end date for 4 wks = 5 wks from start - 1 day
                }
                break;
            default:
                throw new IllegalArgumentException("Unrecognized goal end type");
        }
    }

    private void evaluateSessionPeriod() {
        try(Realm realm = Realm.getDefaultInstance()) {
            // No need to close the Realm instance manually since this is wrapped in try statement
            // https://realm.io/docs/java/latest/#closing-realm-instances
            Date today = getTruncatedDate(Calendar.getInstance().getTime(), ChronoUnit.DAYS); // start of today

            // query for distinct session dates
            RealmResults<Attempt> results = realm.where(Attempt.class).greaterThanOrEqualTo("datetime", mGoal.getStartDate()).distinct("date").sort("datetime", Sort.ASCENDING);

            if(results.isEmpty()) {
                mStartDate = today;
                mEndDate = null;
            }else {
                mStartDate = getTruncatedDate(results.get(0).getDatetime(), ChronoUnit.DAYS);
                if(results.size() < mGoal.getNumPeriods()) {
                    mEndDate = null;
                }else {
                    mEndDate = getTruncatedDate(results.get(mGoal.getNumPeriods()-1).getDatetime(), ChronoUnit.DAYS);
                }
            }

        }
    }

    private void getRealmResult() {
        try (Realm realm = Realm.getDefaultInstance()) {
            // No need to close the Realm instance manually since this is wrapped in try statement
            // https://realm.io/docs/java/latest/#closing-realm-instances

            // NOTE: our contract here is that the realm results stay valid after the map is populated.
            // NOTE: if not recurring, goallist will not use the map but rather the entire data set.

            // get start and end date


            // get the realm results dealing with the "nonrecurring" goal
            // TODO: if enddate == null, then just use start date
            RealmQuery<Attempt> query = realm.where(Attempt.class)
                    .greaterThanOrEqualTo("datetime", mStartDate)
                    .equalTo("climb.type", mGoal.getClimbType().ordinal())
                    .greaterThanOrEqualTo("climb.grade", mGoal.getMingrade());
            if(mEndDate != null) {
                query.lessThan("datetime", getIncrementedDate(mEndDate, ChronoUnit.DAYS, 1));
            }
            if (!mGoal.getIncludeAttempts()) {
                query.equalTo("isSend", true);
            }
            if (mFullRangeResults != null) {
                mFullRangeResults.removeAllChangeListeners(); // just in case, remove any listeners previously attached to these results
            }
            mFullRangeResults = query.findAll();
            mFullRangeResults.addChangeListener(new RealmChangeListener<RealmResults<Attempt>>() {
                @Override
                public void onChange(RealmResults<Attempt> attempts) {
                    if (mGoal.getEndType() == Goal.EndType.PERIOD && mGoal.getPeriod() == Goal.Period.SESSION) {
                        evaluateSessionPeriod(); // session might have been deleted, so we need to revaluate start/end dates and re-get the realm results
                        getRealmResult();
                    }
                    if (mGoal.isRecurring()) {
                        populateGoalMap();
                    }
                    if (listener != null) {
                        listener.onGoalResultsChanged();  // notify the listener that the underlying data has changed
                    }
                }
            });
        }
    }

    private void populateGoalMap() {
        //      loop through dates and add <date, realmresult> to map
        Date periodStartDate = mStartDate;
        Date periodEndDate = getIncrementedDate(mStartDate, mGoal.getPeriod().unit, 1);
        Date today = getTruncatedDate(Calendar.getInstance().getTime(), ChronoUnit.DAYS); // start of today

        mPeriodResultMap = new TreeMap<>();
        do {
            RealmResults<Attempt> periodResults = mFullRangeResults.where().between("datetime", periodStartDate, periodEndDate).findAll();
            // every entry in map will be a "block" in the listview, so entries for sessions should only be added if realmresult is not empty or startdate >= start of today
            if(mGoal.getPeriod() == Goal.Period.SESSION) {
                if(!periodResults.isEmpty() || periodStartDate.equals(today))
                {
                    mPeriodResultMap.put(periodStartDate, periodResults);
                }
            }else {
                mPeriodResultMap.put(periodStartDate, periodResults);
            }
            periodStartDate = periodEndDate;
            periodEndDate = getIncrementedDate(periodEndDate, mGoal.getPeriod().unit, 1);
        } while (!periodStartDate.after(mEndDate == null ? today : mEndDate));

    }

    public int getProgressForPeriod(RealmResults < Attempt > periodResults) {
        if(periodResults.isEmpty()) return 0;
        else {
            int periodTotal;
            switch (mGoal.getGoalUnit()) {
                case CLIMBS:
                    periodTotal = periodResults.size();
                    break;
                case HEIGHT:
                    // TODO: add height multiplier
                    periodTotal = periodResults.size();
                    break;
                case POINTS:
                    periodTotal = 0;
                    for(Attempt attempt: periodResults) {
                        periodTotal += attempt.getClimb().getGrade();
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected goal unit found");
            }

            return periodTotal;
        }
    }

    public void setGoalListener (GoalDAOListener listener){
        this.listener = listener;
    }

        /**
         * Return date moved by increment number of units
         */

    public static Date getIncrementedDate(Date date, ChronoUnit unit, int increment) {

        return Shared.ZDTToDate(Shared.DateToZDT(date).plus(increment,unit));
    }

    /**
     * @param date
     * @param unit
     * @return return this date truncated by unit
     */
    public static Date getTruncatedDate(Date date, ChronoUnit unit) {
        ZonedDateTime zdt = Shared.DateToZDT(date);
        switch (unit) {
            case DAYS:
                zdt = zdt.truncatedTo(ChronoUnit.DAYS);
                break;
            case WEEKS:
                zdt = zdt.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
                break;
            case MONTHS:
                zdt = zdt.withDayOfMonth(1);
                break;
            case YEARS:
                zdt = zdt.withDayOfYear(1);
                break;
            default:
                throw new IllegalArgumentException("Got invalid goal period");
        }
        return Shared.ZDTToDate(zdt);
    }

    public interface GoalDAOListener {
        void onGoalResultsChanged();
    }
}