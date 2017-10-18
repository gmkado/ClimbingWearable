package com.example.grant.wearableclimbtracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.WearableRecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.realm.Area;
import com.example.mysynclibrary.realm.AreaFields;
import com.example.mysynclibrary.realm.Attempt;
import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.realm.ClimbFields;
import com.example.mysynclibrary.realm.Gym;
import com.example.mysynclibrary.realm.GymFields;
import com.example.mysynclibrary.textdrawable.TextDrawable;

import java.util.EnumSet;
import java.util.List;

import io.realm.Realm;

import static com.example.grant.wearableclimbtracker.EditClimbActivity.EditClimbOptions.*;
import static com.example.grant.wearableclimbtracker.WearMainActivity.PREF_FILTER_AREA_ID;
import static com.example.grant.wearableclimbtracker.WearMainActivity.PREF_FILTER_CLIMBTYPE;
import static com.example.grant.wearableclimbtracker.WearMainActivity.PREF_FILTER_GYM_ID;

/**
 * Usages of this activity and the necessary extras
 * 1) WearMainActivity onMenuItemClicked -> climbtype
 * 2) WearMainActivity onItemClicked -> climbtype, climbuuid
 */
public class EditClimbActivity extends Activity implements DelayedConfirmationView.DelayedConfirmationListener{
    public static final String EXTRA_CLIMBTYPE = "ClimbType";
    public static final String EXTRA_CLIMBUUID = "ClimbUUID";
    private static final int CLIMB_LOCATION_REQUEST = 0;
    private static final String TAG = "EditClimbActivity";
    private boolean mAddClimbMode = true;

    private DelayedConfirmationView mDelayedView;
    private WearableRecyclerView mListView;
    private Climb mClimb;
    private boolean mOptionsMenuWasShown;  // have we seen the options menu yet, so we shouldn't step through the field lists
    private SummaryViewHolder mSummaryViewHolder;
    private EditClimbOptions mTimerAction;

    public static Intent newEditInstance(Context context, Climb climb) {
        // open the add activity
        Intent intent = new Intent(context, EditClimbActivity.class);
        intent.putExtra(EXTRA_CLIMBUUID, climb.getId());
        return intent;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CLIMB_LOCATION_REQUEST) {
            if (resultCode == RESULT_OK) {
                // set the climbs CHANGE_LOCATION
                if (data.hasExtra(LocationPickerActivity.EXTRA_GYM_ID)) {
                    String gymId = data.getStringExtra(LocationPickerActivity.EXTRA_GYM_ID);
                    mClimb.setGym(gymId==null? null: mRealm.copyFromRealm(mRealm.where(Gym.class).equalTo(GymFields.ID, gymId).findFirst()));

                }
                // now try adding the area
                if (data.hasExtra(LocationPickerActivity.EXTRA_AREA_ID)) {
                    String areaId = data.getStringExtra(LocationPickerActivity.EXTRA_AREA_ID);
                    mClimb.setArea(areaId == null? null: mRealm.copyFromRealm(mRealm.where(Area.class).equalTo(AreaFields.ID, areaId).findFirst()));

                }

                updateSummaryView();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    private Realm mRealm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_climb);

        Bundle extras = getIntent().getExtras();
        mRealm = Realm.getDefaultInstance();
        mOptionsMenuWasShown = false; // default to false
        mDelayedView = (DelayedConfirmationView) findViewById(R.id.saveDelayedConfirmationView);
        mDelayedView.setListener(EditClimbActivity.this);
        mDelayedView.setTotalTimeMs(3000);

        setDelayedViewVisible(false);

        mListView = (WearableRecyclerView)findViewById(R.id.listView);
        // set the listview margins
        mListView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        mSummaryViewHolder = new SummaryViewHolder(findViewById(R.id.layout_climbSummary));
        if(extras!=null && extras.containsKey(EXTRA_CLIMBUUID)) {
            mAddClimbMode = false;
            Climb climb = mRealm.where(Climb.class).equalTo(ClimbFields.ID, extras.getString(EXTRA_CLIMBUUID)).findFirst();
            mClimb = mRealm.copyFromRealm(climb);  // detach from realm so changes can be made without saving until save button is pressed

            // show options menu
            mListView.setAdapter(new AddClimbOptionsAdapter());
            /* TODO: deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try (Realm realm = Realm.getDefaultInstance()) {
                        realm.beginTransaction();
                        Climb climb = realm.where(Climb.class).equalTo(ClimbFields.ID, mClimbUUID).findFirst();
                        climb.safedelete(false);
                        realm.commitTransaction();
                    } finally {
                        dismiss();
                    }
                }
            });*/

        }else {
            // create unmanaged climb and initialize all default fields here
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

            String gymId = pref.getString(PREF_FILTER_GYM_ID, null);
            String areaId = pref.getString(PREF_FILTER_AREA_ID, null);
            mClimb = new Climb(Climb.ClimbType.values()[pref.getInt(PREF_FILTER_CLIMBTYPE, Climb.ClimbType.bouldering.ordinal())],
                    gymId == null? null: mRealm.copyFromRealm(mRealm.where(Gym.class).equalTo(GymFields.ID, gymId).findFirst()),
                    areaId == null? null: mRealm.copyFromRealm(mRealm.where(Area.class).equalTo(AreaFields.ID, areaId).findFirst()));

            mListView.setAdapter(new GradeAdapter());
        }
        updateSummaryView();

    }

    private void setDelayedViewVisible(boolean delayedViewVisible) {
        if(delayedViewVisible){
            findViewById(R.id.listViewLayout).setVisibility(View.GONE);
            findViewById(R.id.saveConfirmationLayout).setVisibility(View.VISIBLE);
        }else{
            findViewById(R.id.listViewLayout).setVisibility(View.VISIBLE);
            findViewById(R.id.saveConfirmationLayout).setVisibility(View.GONE);
        }
    }

    @Override
    public void onTimerFinished(View view) {
        // not cancelled so save the climb
        Log.d(TAG, "onTimerFinished");

        mRealm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                switch(mTimerAction) {
                    case ADD_AS_PROJECT:
                        realm.copyToRealmOrUpdate(mClimb);
                        break;
                    case ADD_AS_SEND:
                        realm.copyToRealm(Attempt.createSend(realm.copyToRealmOrUpdate(mClimb), false));// TODO: need a UI element or dialog box for if climb was on lead
                        break;
                    case SAVE_CHANGES:
                        realm.copyToRealmOrUpdate(mClimb);
                        break;
                    case DELETE_CLIMB:
                        Climb climb = realm.where(Climb.class).equalTo(ClimbFields.ID, mClimb.getId()).findFirst();
                        if (climb != null) {
                            climb.deleteFromRealm();
                        }
                        break;
                }
            }
        });

        // show confirmation
        Intent intent = new Intent(this, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                ConfirmationActivity.SUCCESS_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                "Saved");
        startActivity(intent);

        finish();
    }

    @Override
    public void onTimerSelected(View view) {
        Log.d(TAG, "onTimerSelected");

        mDelayedView.setPressed(true);

        // Prevent onTimerFinished from being heard.
        mDelayedView.setListener(null);

        // cancel selected, so go back to listview
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                setDelayedViewVisible(false);
            }
        }, 50);
    }

    public static Intent newAddInstance(Context context) {
        return new Intent(context, EditClimbActivity.class);
    }


    private class GradeAdapter extends RecyclerView.Adapter<GradeAdapter.ViewHolder> {
        List<String> mGradeStrings;
        GradeAdapter() {
            super();
            mGradeStrings = mClimb.getType().grades;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public int getItemCount() {
            return mGradeStrings.size();
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.grade.setText(mGradeStrings.get(position));
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClimb.setGrade(holder.getAdapterPosition());
                    updateSummaryView();
                    if(!mOptionsMenuWasShown) {
                        // move on to colors
                        mListView.setAdapter(new ClimbColorAdapter());
                    }else {
                        // return to options menu
                        mListView.setAdapter(new AddClimbOptionsAdapter());
                    }

                }
            });
        }

        class ViewHolder extends RecyclerView.ViewHolder{
            View mView;
            TextView grade;

            ViewHolder(View view) {
                super(view);
                mView = view;
                grade = (TextView) view.findViewById(android.R.id.text1);

            }

        }
    }

    private void updateSummaryView() {
        // CHANGE_GRADE
        TextDrawable drawable = TextDrawable.builder()
                .beginConfig()
                .textColor(Color.BLACK)
                .useFont(Typeface.DEFAULT)
                .setStrikethrough(mClimb.isRemoved())
                .fontSize(30) /* size in px */
                .bold()
                .endConfig()
                .buildRound(mClimb.getType().grades.get(mClimb.getGrade()), mClimb.getColor());

        mSummaryViewHolder.grade.setImageDrawable(drawable);
        String locationString;
        if(mClimb.getGym() == null) {
            locationString = "No gym set";
        }else {
            locationString = mClimb.getGym().getName();
            if (mClimb.getArea() == null) {
                locationString += ", No area set";
            } else {
                locationString += ", " + mClimb.getArea().getName();
            }
        }
    }

    enum EditClimbOptions {
        ADD_AS_SEND(R.drawable.ic_send_light, "Add As Send"),
        ADD_AS_PROJECT(R.drawable.ic_project_light, "Add As Project"),
        CHANGE_GRADE(R.drawable.ic_grade_light, "Change Grade"),
        CHANGE_COLOR(R.drawable.ic_color_light, "Change Color"),
        CHANGE_LOCATION(R.drawable.ic_location_light, "Change Location"),
        MARK_AS_REMOVED(R.drawable.ic_removed_light, "Toggle Removed"),
        SAVE_CHANGES(R.drawable.ic_save_light, "Save Changes"),
        DELETE_CLIMB(R.drawable.ic_delete_light, "Delete Climb");

        private final int drawable;
        private final String title;

        EditClimbOptions(int drawable, String title) {
            this.drawable = drawable;
            this.title = title;
        }
    }
    EnumSet<EditClimbOptions> editOptionsSet = EnumSet.complementOf(EnumSet.of(ADD_AS_SEND, ADD_AS_PROJECT));
    EnumSet<EditClimbOptions> addOptionsSet = EnumSet.complementOf(EnumSet.of(MARK_AS_REMOVED, SAVE_CHANGES, DELETE_CLIMB));


    /**
     * Wrapper adapter for addclimboptions
     */
    private class AddClimbOptionsAdapter extends RecyclerView.Adapter<AddClimbOptionsAdapter.ViewHolder> {
        EditClimbOptions[] mOptionsArray;

        AddClimbOptionsAdapter() {
            super();
            mOptionsMenuWasShown = true;
            mOptionsArray = mAddClimbMode?
                    addOptionsSet.toArray(new EditClimbOptions[addOptionsSet.size()]):
                    editOptionsSet.toArray(new EditClimbOptions[editOptionsSet.size()]);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_menu, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final EditClimbOptions option = mOptionsArray [position];
            holder.title.setText(option.title);

            /*float scale = 0.3f;
            Drawable image = getDrawable( option.drawable);
            int h = (int) (scale * image.getIntrinsicHeight());
            int w = (int) (scale * image.getIntrinsicWidth());
            image.setBounds( 0, 0, w, h);  Doesnt work because images are all different sizes*/
            // Scale the image
            holder.title.setCompoundDrawablesWithIntrinsicBounds(Shared.getScaledDrawable(EditClimbActivity.this, option.drawable, 50, 50),
                    null, null, null);

            final TextView tv = (TextView) findViewById(R.id.saveConfirmationTextView);
            // TODO: implement the rest of these
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch(option) {
                        case ADD_AS_SEND:
                            // NOTE: set list view, see onTimerFinished for actions taken
                            tv.setText(String.format("Saving %s send", mClimb.getGrade()));
                            setDelayedViewVisible(true);
                            mTimerAction = option;
                            mDelayedView.start();
                            break;
                        case ADD_AS_PROJECT:
                            tv.setText(String.format("Saving %s project", mClimb.getGrade()));
                            setDelayedViewVisible(true);
                            mTimerAction = option;
                            mDelayedView.start();
                            break;
                        case SAVE_CHANGES:
                            tv.setText(String.format("Saving changes", mClimb.getGrade()));
                            setDelayedViewVisible(true);
                            mTimerAction = option;
                            mDelayedView.start();
                            break;
                        case MARK_AS_REMOVED:
                            // Toggle
                            mClimb.setRemoved(!mClimb.isRemoved());
                            updateSummaryView();
                            break;
                        case DELETE_CLIMB:
                            tv.setText(String.format("Deleting %s", mClimb.getGrade()));
                            setDelayedViewVisible(true);
                            mTimerAction = option;
                            mDelayedView.start();
                            break;
                        case CHANGE_GRADE:
                            mListView.setAdapter(new GradeAdapter());
                            break;
                        case CHANGE_COLOR:
                            mListView.setAdapter(new ClimbColorAdapter());
                            break;
                        case CHANGE_LOCATION:
                            Intent intent = new Intent(EditClimbActivity.this, LocationPickerActivity.class);
                            startActivityForResult(intent, CLIMB_LOCATION_REQUEST);
                            break;
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mOptionsArray.length;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            View mView;

            ViewHolder(View view) {
                super(view);
                mView = view;
                title = (TextView) view.findViewById(R.id.textviewTitle);
            }
        }
    }

    private class ClimbColorAdapter extends RecyclerView.Adapter<ClimbColorAdapter.ViewHolder> {
        private int[] palette = ColorPalette.PRIMARY_COLORS;;

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            holder.mView.setBackgroundColor(palette[position]);
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClimb.setColor(palette[holder.getAdapterPosition()]);
                    updateSummaryView();
                    if(!mOptionsMenuWasShown) {
                        // TODO: could show CHANGE_LOCATION picker here?
                        mListView.setAdapter(new AddClimbOptionsAdapter());
                    }else {
                        // return to options menu
                        mListView.setAdapter(new AddClimbOptionsAdapter());
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return palette.length;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View mView;

            ViewHolder(View view) {
                super(view);
                mView = view;
            }
        }
    }

    private class SummaryViewHolder{
        ImageView grade;
        TextView location;

        SummaryViewHolder(View summaryView) {
            grade = (ImageView) summaryView.findViewById(R.id.image_view);
            location = (TextView) summaryView.findViewById(R.id.textview_location);
        }
    }
}
