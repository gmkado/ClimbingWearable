package com.example.mysynclibrary.realm;

import com.example.mysynclibrary.R;
import com.example.mysynclibrary.Shared;

import org.threeten.bp.temporal.ChronoUnit;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

// Your model just have to extend RealmObject.
// This will inherit an annotation which produces proxy getters and setters for ALL fields.
public class Goal extends RealmObject {

    // All fields are by default persisted.

    /*****************These fields are required*********************************************/
    // The primitives have the Required annotation by default
    @PrimaryKey private String id;
    private int climbtype;
    private int goalunit;
    private boolean includeAttempts;
    private String name;

    public int getMingrade() {
        return mingrade;
    }

    public void setMingrade(int mingrade) {
        this.mingrade = mingrade;
    }

    private int mingrade;
    private int endtype;
    @Required private Date startDate; // TODO: this could have weird consequences if user changes timezones.  Could solve by using localdatetime and storing as a string
    private int target;
    /**************************************************************************************/
    private int period;
    private int numPeriods;
    private Date endDate;
    private boolean recurring;
    private int heightunit;

    /**
     * Parses the fields and returns a readable summary of this goal
     * @return
     */
    public String getSummary() {
        String summary = (getClimbType()==Shared.ClimbType.bouldering?"Boulder ": "Rope climb ");
        switch(getGoalUnit()) {
            case CLIMBS:
                summary = summary.concat(Integer.toString(target) + " climbs ");
                break;
            case HEIGHT:
                summary = summary.concat(Integer.toString(target) +
                        (getHeightunit()==HeightUnit.FT?"ft ":"m "));
                break;
            case POINTS:
                summary = summary.concat(Integer.toString(target) + " points ");
                break;
        }
        summary = summary.concat("at least " + getClimbType().grades.get(getMingrade())+ " ");
        if(recurring) {
            switch (getPeriod()) {
                case SESSION:
                    summary = summary.concat("every session ");
                    break;
                case WEEKLY:
                    summary = summary.concat("every week ");
                    break;
                case MONTHLY:
                    summary = summary.concat("every month ");
                    break;
                case YEARLY:
                    summary = summary.concat("every year ");
                    break;
            }
        }
        DateFormat sdf = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT);
        summary = summary.concat("starting " + sdf.format(startDate) + " ");

        switch(getEndType()) {
            case NEVER:
                break;
            case DATE:
                summary = summary.concat("until " + sdf.format(endDate));
                break;
            case PERIOD:
                summary = summary.concat("for " + Integer.toString(numPeriods));
                switch (getPeriod()) {
                    case SESSION:
                        summary = summary.concat(" sessions ");
                        break;
                    case WEEKLY:
                        summary = summary.concat(" weeks ");
                        break;
                    case MONTHLY:
                        summary = summary.concat(" months ");
                        break;
                    case YEARLY:
                        summary = summary.concat(" years ");
                        break;
                }
                break;
        }
        return summary;
    }

    public String getUUID() {
        return id;
    }

    public boolean getIncludeAttempts() {
        return includeAttempts;
    }

    public void setIncludeAttempts(boolean includeAttempts) {
        this.includeAttempts = includeAttempts;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public enum HeightUnit {
        FT,
        M;

        public static List<String> getStringArray() {
            ArrayList<String> list = new ArrayList();
            for(HeightUnit heightUnit : HeightUnit.values()) {
                list.add(heightUnit.name());
            }
            return list;
        }
    }

    public enum GoalUnit {
        CLIMBS(R.drawable.ic_climb),
        HEIGHT(R.drawable.ic_height),
        POINTS(R.drawable.ic_points);

        private final int drawableId;

        GoalUnit(int drawableResourceId) {
            this.drawableId = drawableResourceId;
        }

        public int getDrawableId() {
            return drawableId;
        }
        public static List<String> getStringArray() {
            ArrayList<String> list = new ArrayList();
            for(GoalUnit goalUnit : GoalUnit.values()) {
                list.add(goalUnit.name());
            }
            return list;
        }
    }

    public enum Period {
        SESSION(ChronoUnit.DAYS, "session", "sessions"),
        WEEKLY(ChronoUnit.WEEKS, "week", "weeks"),
        MONTHLY(ChronoUnit.MONTHS,"month", "months"),
        YEARLY(ChronoUnit.YEARS,"year", "years");

        public ChronoUnit unit;
        private String plural;
        private String singular;

        Period(ChronoUnit unit, String singular, String plural) {
            this.unit = unit;
            this.singular = singular;
            this.plural = plural;
        }

        public static List<String> getStringArray() {
            ArrayList list = new ArrayList();
            for(Period period : Period.values()) {
                list.add(period.name());
            }
            return list;
        }

        public String getPlural() {
            return plural;
        }

        public String getSingular() {
            return singular;
        }
    }

    public enum EndType {
        NEVER,
        DATE,
        PERIOD;  // This needs to be at the end since we will take it off if the goal is not recurring

        public static List<String> getStringArray() {
            ArrayList list = new ArrayList();
            for(EndType type:EndType.values()) {
                list.add(type.name());
            }
            return list;
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    public GoalUnit getGoalUnit() {
        return GoalUnit.values()[goalunit];
    }

    public void setGoalunit(GoalUnit goalUnit) {
        this.goalunit = goalUnit.ordinal();
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public void setClimbType(Shared.ClimbType type) {
        this.climbtype = type.ordinal();
    }

    public Shared.ClimbType getClimbType() {
        return Shared.ClimbType.values()[climbtype];
    }

    public EndType getEndType() {
        return EndType.values()[endtype];
    }

    public void setEndtype(EndType endtype) {
        this.endtype = endtype.ordinal();
    }

    public Period getPeriod() {
        return Period.values()[period];
    }

    public void setPeriod(Period period) {
        this.period = period.ordinal();
    }

    public int getNumPeriods() {
        return numPeriods;
    }

    public void setNumPeriods(int numPeriods) {
        this.numPeriods = numPeriods;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring){
        this.recurring = recurring;
    }

    public HeightUnit getHeightunit() {
        return HeightUnit.values()[heightunit];
    }

    public void setHeightunit(HeightUnit heightunit) {
        this.heightunit = heightunit.ordinal();
    }

    /**
     *
     * @return true if goal has all fields required to be a valid goal
     */
    public boolean isValidGoal() {
        // TODO: should probably have some toast message to say why its not valid
        // check all required fields
        if (startDate == null) return false;
        if (target <=0) return false;

        switch(EndType.values()[endtype]) {
            case NEVER:
                break;
            case DATE:
                if (endDate == null || endDate.before(startDate)) {
                    return false;
                }
                break;
            case PERIOD:
                if (numPeriods <= 0) {
                    return false;
                }
        }
        return true;

    }
}