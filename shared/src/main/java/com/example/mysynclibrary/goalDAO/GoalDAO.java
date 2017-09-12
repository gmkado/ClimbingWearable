package com.example.mysynclibrary.goalDAO;

import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.SimpleSpanBuilder;
import com.example.mysynclibrary.realm.Attempt;
import com.example.mysynclibrary.realm.AttemptFields;
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
    private int mNumSuccessfulPeriods;
    private Date mCurrPeriodEndDate;

    public GoalDAO(Goal goal) {
        mGoal = goal; // NOTE: we don't have a changelistener on goal because anytime the goals change, GoalListFragment repopulates the list anyways
        listener = null;

        getDateLimits();
        getRealmResult();
        if(mGoal.isRecurring()) {
            populateGoalMapAndGetRecurringStats();
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

    private void getDateLimits() {
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
                    getSessionDateLimits();
                }else {
                    mStartDate = mGoal.getStartDate();
                    mEndDate = getIncrementedDate(getIncrementedDate(mStartDate, mGoal.getPeriod().unit, mGoal.getNumPeriods()+1), ChronoUnit.DAYS, -1); // end date for 4 wks = 5 wks from start - 1 day
                }
                break;
            default:
                throw new IllegalArgumentException("Unrecognized goal end type");
        }
    }

    private void getSessionDateLimits() {
        // Get the start and end date for the special case of Sessions
        try(Realm realm = Realm.getDefaultInstance()) {
            // No need to close the Realm instance manually since this is wrapped in try statement
            // https://realm.io/docs/java/latest/#closing-realm-instances
            Date today = getTruncatedDate(Calendar.getInstance().getTime(), ChronoUnit.DAYS); // start of today

            // query for distinct session dates
            RealmResults<Attempt> results = realm.where(Attempt.class).greaterThanOrEqualTo(AttemptFields.DATETIME, mGoal.getStartDate()).distinct("date").sort("datetime", Sort.ASCENDING);

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
            // NOTE: our contract here is that the realm results stay valid after the map is populated.
            // NOTE: if not recurring, goallist will not use the map but rather the entire data set.

            // get start and end date
            // get the realm results dealing with the "nonrecurring" goal
            RealmQuery<Attempt> query = realm.where(Attempt.class)
                    .greaterThanOrEqualTo(AttemptFields.DATETIME, mStartDate)
                    .equalTo(AttemptFields.CLIMB.TYPE, mGoal.getClimbType().ordinal())
                    .greaterThanOrEqualTo(AttemptFields.CLIMB.GRADE, mGoal.getMingrade());
            if(mEndDate != null) {
                query.lessThan(AttemptFields.DATETIME, getIncrementedDate(mEndDate, ChronoUnit.DAYS, 1));
            }
            if (!mGoal.getIncludeAttempts()) {
                query.equalTo(AttemptFields.IS_SEND, true);
            }
            if (mFullRangeResults != null) {
                mFullRangeResults.removeAllChangeListeners(); // just in case, remove any listeners previously attached to these results
            }
            mFullRangeResults = query.findAll();
            mFullRangeResults.addChangeListener(new RealmChangeListener<RealmResults<Attempt>>() {
                @Override
                public void onChange(RealmResults<Attempt> attempts) {
                    if (mGoal.getEndType() == Goal.EndType.PERIOD && mGoal.getPeriod() == Goal.Period.SESSION) {
                        // session might have been deleted, so we need to revaluate start/end dates and re-get the realm results
                        getSessionDateLimits();
                        getRealmResult();
                    }
                    if (mGoal.isRecurring()) {
                        populateGoalMapAndGetRecurringStats();
                    }

                    if (listener != null) {
                        listener.onGoalResultsChanged();  // notify the listener that the underlying data has changed
                    }
                }
            });
        }
    }

    private void populateGoalMapAndGetRecurringStats() {
        // loop through dates and add <date, realmresult> to map
        Date periodStartDate = mStartDate;
        Date periodEndDate = getIncrementedDate(mStartDate, mGoal.getPeriod().unit, 1);
        Date today = getTruncatedDate(Calendar.getInstance().getTime(), ChronoUnit.DAYS); // start of today

        mNumSuccessfulPeriods = 0;
        mPeriodResultMap = new TreeMap<>();

        Date pastResultsEndDate = mEndDate == null?   // the end date for past results is today or the goal end date, whichever comes first
                today:
                mEndDate.before(today)?
                        mEndDate:
                        today;

        while(!periodStartDate.after(pastResultsEndDate)){
            RealmResults<Attempt> periodResults = mFullRangeResults.where().between("datetime", periodStartDate, periodEndDate).findAll();

            // tally the recurring period successes.  if period is session then results need to be not empty or  today to be counted
            if(mGoal.getPeriod() != Goal.Period.SESSION || periodStartDate.equals(today) || !periodResults.isEmpty()) {
                mPeriodResultMap.put(periodStartDate, periodResults);
                if (getProgressForPeriod(periodResults) >= mGoal.getTarget()) {
                    mNumSuccessfulPeriods++;
                }
            }
            mCurrPeriodEndDate = periodEndDate; // used to determine remaining time
            periodStartDate = periodEndDate;
            periodEndDate = getIncrementedDate(periodEndDate, mGoal.getPeriod().unit, 1);
        }

    }

    private boolean isGoalExpired() {
        if(mEndDate==null) {
            return false; // no end
        } else {
            Date today = getTruncatedDate(Calendar.getInstance().getTime(), ChronoUnit.DAYS); // start of today
            return today.after(mEndDate);
        }
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

    public SpannableStringBuilder getPastProgressText() {
        // 9 out of 10 sessions (90%), 3 days remaining
        if(mGoal.isRecurring()) {
            SimpleSpanBuilder ssb = new SimpleSpanBuilder();
            ssb.append("Previous " + mGoal.getPeriod().getPlural(), new RelativeSizeSpan(1.5f));
            int numPastPeriods = mPeriodResultMap.size();
            int percent = mNumSuccessfulPeriods *100/ numPastPeriods;

            ssb.append(String.format(Locale.getDefault(),
                    "\n%d out of %d %s (%d%%)",
                    mNumSuccessfulPeriods,
                    numPastPeriods,
                    numPastPeriods==1?mGoal.getPeriod().getSingular(): mGoal.getPeriod().getPlural(),
                    percent));
            if(mGoal.getEndType() != Goal.EndType.NEVER) {
                // enddate = (endtype = period && period = sessions) -> numPeriods-numPastPeriods, else enddate = date
                int numPeriodsRemaining;
                if(mGoal.getEndType() == Goal.EndType.PERIOD && mGoal.getPeriod() == Goal.Period.SESSION) {
                    numPeriodsRemaining = mGoal.getNumPeriods() - numPastPeriods;
                }else {
                    numPeriodsRemaining = (int)getPeriodsBetweenDates(mCurrPeriodEndDate, mEndDate, mGoal.getPeriod().unit);
                }

                ssb.append(String.format(Locale.getDefault(),
                    "\n%d %s remaining",
                    numPeriodsRemaining,
                    numPeriodsRemaining == 1? mGoal.getPeriod().getSingular():mGoal.getPeriod().getSingular()));
            }
            return ssb.build();
        } else {
            return null;
        }
    }

    public SpannableStringBuilder getCurrentProgressText() {
        SimpleSpanBuilder ssb = new SimpleSpanBuilder();
        if(mGoal.isRecurring()) {
            ssb.append("Current " + mGoal.getPeriod().getSingular(), new RelativeSizeSpan(1.5f));
        }else {
            ssb.append("Current Progress", new RelativeSizeSpan(1.5f));
        }

        ssb.append(String.format(Locale.getDefault(),
                "\n%d out of %d %s over %s",
                getCurrentProgress(),
                mGoal.getTarget(), mGoal.getGoalUnit().name(),
                mGoal.getClimbType().grades.get(mGoal.getMingrade())));

        // now get the remaining period
        // if this is recurring, unit = (period = days) -> hours, (period = weeks) -> days, etc
        // if this is nonrecurring, unit = (endtype = period) -> period, (endtype = date) -> days
        if(mGoal.getEndType()!= Goal.EndType.NEVER) {
            Date today = getTruncatedDate(Calendar.getInstance().getTime(), ChronoUnit.DAYS); // start of today
            ChronoUnit remainderUnit = null;
            if (mGoal.isRecurring()) {
                switch (mGoal.getPeriod()) {
                    case SESSION:
                        today = Calendar.getInstance().getTime();
                        remainderUnit = ChronoUnit.HOURS;
                        break;
                    case WEEKLY:
                        remainderUnit = ChronoUnit.DAYS;
                        break;
                    case MONTHLY:
                        remainderUnit = ChronoUnit.DAYS;
                        break;
                    case YEARLY:
                        remainderUnit = ChronoUnit.MONTHS;
                        break;
                }
            } else {
                if (mGoal.getEndType() == Goal.EndType.PERIOD) {
                    remainderUnit = mGoal.getPeriod().unit;
                } else if (mGoal.getEndType() == Goal.EndType.DATE) {
                    remainderUnit = ChronoUnit.DAYS;
                }
                mCurrPeriodEndDate = mEndDate;
            }
            if (remainderUnit != null && mCurrPeriodEndDate!=null) {
                ssb.append(String.format(Locale.getDefault(), "\n%d %s remaining",
                        getPeriodsBetweenDates(today, mCurrPeriodEndDate, remainderUnit),
                        remainderUnit.name()
                ));
            }
        }
        return ssb.build();
    }

    public int getCurrentProgress() {
        return getProgressForPeriod(mGoal.isRecurring()? mPeriodResultMap.lastEntry().getValue():mFullRangeResults);
    }

    public interface GoalDAOListener {
            void onGoalResultsChanged();
        }


    private long getPeriodsBetweenDates(Date start, Date end, ChronoUnit unit) {
        ZonedDateTime startzdt = Shared.DateToZDT(start);
        ZonedDateTime endzdt = Shared.DateToZDT(end);
        return unit.between(startzdt, endzdt);
    }
}