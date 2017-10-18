package com.example.mysynclibrary.realm;

import com.example.mysynclibrary.BuildConfig;
import com.example.mysynclibrary.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.Index;
import io.realm.annotations.LinkingObjects;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;


// Your model just have to extend RealmObject.
// This will inherit an annotation which produces proxy getters and setters for ALL fields.

@RealmClass
public class Climb extends RealmObject {
    // All fields are by default persisted.
    @PrimaryKey private String id;
    @Index private int grade;
    private int type;
    private Date createdAt = new Date();

    @Index private int color; // integer representation of color
    private Gym gym;   // gym name
    private Area area; // area name
    private boolean isRemoved;  // has the climb been removed?
    private String notes;   // more distinguishing notes

    @LinkingObjects("climb")
    private final RealmResults<Attempt> attempts = null;


    public Climb() {
        // NOTE: DON'T USE THIS CONSTRUCTOR!!!
        if(BuildConfig.DEBUG && id == null) {
            throw new AssertionError("Use parameterized constructor");
        }

    }
    public Climb(ClimbType type, Gym gym, Area area) {
        id = UUID.randomUUID().toString();
        grade = 0;
        color = -1;
        this.type = type.ordinal();
        this.gym = gym;
        this.area = area;
    }

    public int getGrade() {
        return grade;
    }

    public void setGrade(int grade) {
        this.grade = grade;
    }

    public ClimbType getType() {
        return ClimbType.values()[type];
    }

    public void setType(ClimbType type) {
        this.type = type.ordinal();
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public Gym getGym() {
        return gym;
    }

    public void setGym(Gym gym) {
        this.gym = gym;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /********** helper functions for exporting to CSV *******************************/
    public static String[] getTitleRow() {
        return new String[] {"Date", "Type", "Grade"};
    }

    public String[] toStringArray() {
        ClimbType type = getType();
        // TODO: fix this to include new way of recording attempts -- return new String[] {getSendDate().toString(), type.title, type.grades.get(getGrade())};
        return null;
    }

    /*********** check validity of climb object *****************/
    public boolean isValidClimb() {
        // check all required fields;
        return true;
    }

    public boolean isRemoved() {
        return isRemoved;
    }

    public void setRemoved(boolean removed) {
        isRemoved = removed;
    }

    public RealmResults<Attempt> getAttempts() {
        return attempts;
    }

    public Date getDateCreated() {
        return createdAt;
    }

    public String getId() {
        return id;
    }


    public enum ClimbType {
        bouldering("Bouldering", R.drawable.icon_boulder,
                createGradeList(0, 17, "V", 18, null),
                Arrays.asList("V3", "V6", "V9")),
        ropes("Ropes", R.drawable.icon_ropes,
                createGradeList(6, 15, "5.",10, Arrays.asList("a","b","c","d")),
                Arrays.asList("5.8", "5.10d", "5.12d"));

        private static List<String> createGradeList(int minGrade, int maxGrade, String prefix, int minSuffixGrade, List<String> suffixList) {
            ArrayList<String> gradeList = new ArrayList();

            for(int grade = minGrade; grade <= maxGrade; grade++) {
                // this is janky but oh well
                if(grade >= minSuffixGrade) {
                    for(String suffix: suffixList) {
                        gradeList.add(prefix + grade + suffix);
                    }
                }else {
                    gradeList.add(prefix + grade);
                }
            }
            return gradeList;
        }

        public String title;
        public int icon;
        public List<String> grades;
        public List<Integer> indMaxGradeForLevel; // index of the max grade for a particular level

        ClimbType(String title, int icon, List<String> grades, List<String> levelDef){
            this.title = title;
            this.icon = icon;
            this.grades = grades;

            // levelDef = hardest grades for easy, med, hard. Expert is assumed as anything larger than hard
            assert levelDef.size() == ClimbLevel.values().length - 1;
            indMaxGradeForLevel = Arrays.asList(
                    grades.indexOf(levelDef.get(ClimbLevel.beginner.ordinal())),
                    grades.indexOf(levelDef.get(ClimbLevel.intermediate.ordinal())),
                    grades.indexOf(levelDef.get(ClimbLevel.advanced.ordinal())),
                    grades.size() - 1);

            if(indMaxGradeForLevel.contains(-1)) {
                // this means one of the grades was not found so throw an error
                throw new IndexOutOfBoundsException();
            }
        }

        public int getIndexOfMaxGradeForLevel(ClimbLevel level) {
            return indMaxGradeForLevel.get(level.ordinal());
        }

        public String getLabelForLevel(ClimbLevel level) {
            int startInd;
            if(level.ordinal() == 0) {
                startInd = 0;
            }else {
                startInd = getIndexOfMaxGradeForLevel(level.values()[level.ordinal()-1])+1;
            }
            int endInd = getIndexOfMaxGradeForLevel(level);
            return level.title + " (" + grades.get(startInd) + " to " +grades.get(endInd) + ")";
        }
    }

    public enum ClimbLevel{
        beginner("Beginner"),
        intermediate("Intermediate"),
        advanced("Advanced"),
        expert("Expert");

        public String title;
        ClimbLevel(String title) {
            this.title = title;
        }
    }
}