package com.example.grant.wearableclimbtracker;

import android.app.Activity;
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

import com.amulyakhare.textdrawable.TextDrawable;
import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.realm.Area;
import com.example.mysynclibrary.realm.AreaFields;
import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.realm.ClimbFields;
import com.example.mysynclibrary.realm.Gym;
import com.example.mysynclibrary.realm.GymFields;

import java.util.List;

import io.realm.Realm;

import static com.example.grant.wearableclimbtracker.MainActivity.PREF_FILTER_AREA_ID;
import static com.example.grant.wearableclimbtracker.MainActivity.PREF_FILTER_CLIMBTYPE;
import static com.example.grant.wearableclimbtracker.MainActivity.PREF_FILTER_GYM_ID;

/**
 * Usages of this activity and the necessary extras
 * 1) MainActivity onMenuItemClicked -> climbtype
 * 2) MainActivity onItemClicked -> climbtype, climbuuid
 */
public class AddClimbActivity extends Activity implements DelayedConfirmationView.DelayedConfirmationListener{
    public static final String EXTRA_CLIMBTYPE = "ClimbType";
    public static final String EXTRA_CLIMBUUID = "ClimbUUID";
    private static final int CLIMB_LOCATION_REQUEST = 0;
    private static final String TAG = "AddClimbActivity";

    private DelayedConfirmationView mDelayedView;
    private WearableRecyclerView mListView;
    private Climb mClimb;
    private boolean mOptionsMenuWasShown;  // have we seen the options menu yet, so we shouldn't step through the field lists
    private SummaryViewHolder mSummaryViewHolder;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CLIMB_LOCATION_REQUEST) {
            if (resultCode == RESULT_OK) {
                // set the climbs location
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
        setDelayedViewVisible(false);

        mListView = (WearableRecyclerView)findViewById(R.id.listView);
        // set the listview margins
        mListView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        mSummaryViewHolder = new SummaryViewHolder(findViewById(R.id.layout_climbSummary));
        if(extras!=null && extras.containsKey(EXTRA_CLIMBUUID)) {
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
                   realm.copyToRealmOrUpdate(mClimb);
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
        // grade
        TextDrawable drawable = TextDrawable.builder()
                .beginConfig()
                .textColor(Color.BLACK)
                .useFont(Typeface.DEFAULT)
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
        mSummaryViewHolder.location.setText(locationString);
    }

    enum AddClimbOptions{
        addSend(R.drawable.ic_send_light, "Add Send"),
        addAttempt(R.drawable.ic_project_light, "Add Project"),
        grade(R.drawable.ic_grade_light, "Change Grade"),
        color(R.drawable.ic_color_light, "Change Color"),
        location(R.drawable.ic_location_light, "Change Location");

        private final int drawable;
        private final String title;

        AddClimbOptions(int drawable, String title) {
            this.drawable = drawable;
            this.title = title;
        }
    }

    /**
     * Wrapper adapter for addclimboptions
     */
    private class AddClimbOptionsAdapter extends RecyclerView.Adapter<AddClimbOptionsAdapter.ViewHolder> {

        AddClimbOptionsAdapter() {
            super();
            mOptionsMenuWasShown = true;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_menu, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final AddClimbOptions option = AddClimbOptions.values()[position];
            holder.title.setText(option.title);

            float scale = 0.3f;
            /*Drawable image = getDrawable( option.drawable);
            int h = (int) (scale * image.getIntrinsicHeight());
            int w = (int) (scale * image.getIntrinsicWidth());
            image.setBounds( 0, 0, w, h);  Doesnt work because images are all different sizes*/
            // Scale the image
            holder.title.setCompoundDrawablesWithIntrinsicBounds(Shared.getScaledDrawable(AddClimbActivity.this, option.drawable, 50, 50),
                    null, null, null);

            // TODO: implement the rest of these
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch(option) {
                        case addSend:
                            // set list view
                            TextView tv = (TextView) findViewById(R.id.saveConfirmationTextView);
                            tv.setText(String.format("Saving %s send", mClimb.getGrade()));
                            setDelayedViewVisible(true);
                            mDelayedView.setListener(AddClimbActivity.this);
                            // Two seconds to cancel the action
                            mDelayedView.setTotalTimeMs(3000);
                            // Start the timer
                            mDelayedView.start();
                            break;
                        case addAttempt:
                            break;
                        case grade:
                            mListView.setAdapter(new GradeAdapter());
                            break;
                        case color:
                            mListView.setAdapter(new ClimbColorAdapter());
                            break;
                        case location:
                            Intent intent = new Intent(AddClimbActivity.this, LocationPickerActivity.class);
                            startActivityForResult(intent, CLIMB_LOCATION_REQUEST);
                            break;
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return AddClimbOptions.values().length;
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
                        // TODO: could show location picker here?
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
