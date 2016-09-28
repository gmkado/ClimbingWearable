package com.example.grant.wearableclimbtracker;

import android.app.Fragment;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.example.grant.wearableclimbtracker.MainActivity;
import com.example.grant.wearableclimbtracker.R;
import com.example.grant.wearableclimbtracker.model.Climb;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by Grant on 8/1/2016.
 */
public class SessionOverviewFragment extends Fragment {

    public static final String ARG_CLIMB_TYPE = "climbTypeArg";
    private TextView pointsTextView;
    private MainActivity.ClimbType mClimbType;
    private final String TAG = "SessionOverviewFragment";
    private TextView typeTextView;
    private TextView countTextView;
    private Realm mRealm;
    private TextView maxTextView;

    public SessionOverviewFragment() {
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            mRealm = Realm.getDefaultInstance();
            View rootView = inflater.inflate(R.layout.fragment_session_overview, container, false);
            pointsTextView = (TextView) rootView.findViewById(R.id.points_textview);
            countTextView = (TextView) rootView.findViewById(R.id.count_textview);
            maxTextView = (TextView) rootView.findViewById(R.id.max_textView);

            typeTextView = (TextView)rootView.findViewById(R.id.title_textView);
            mClimbType = MainActivity.ClimbType.values()[getArguments().getInt(ARG_CLIMB_TYPE)];

            typeTextView.setText(mClimbType.title);
            return rootView;
        }

        @Override
        public void onResume() {
            super.onResume();
            Log.d(TAG, "onResume()");
            updatePointsView();
        }

        public void updatePointsView() {
            Log.d(TAG, "updatePointsView()");

            // run a query for today
            Calendar cal = Calendar.getInstance();

            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            final RealmResults<Climb> results = mRealm.where(Climb.class)
                    .equalTo("type", mClimbType.ordinal())
                    .greaterThan("date",cal.getTime())
                    .findAll();

            int sum =  (results.sum("grade")).intValue();
            int count = results.size();
            Number max = results.max("grade");

            if(max == null) {
                maxTextView.setVisibility(View.GONE);
            }else{
                maxTextView.setVisibility(View.VISIBLE);
                List<String> gradeList = mClimbType == MainActivity.ClimbType.bouldering ?
                        Arrays.asList(getResources().getStringArray(R.array.bouldering_grades)) :
                        Arrays.asList(getResources().getStringArray(R.array.rope_grades));

                maxTextView.setText(String.format("MAX: %s", gradeList.get(max.intValue())));
            }

            pointsTextView.setText(String.format("POINTS: %d", sum));
            countTextView.setText(String.format("CLIMBS: %d", count));


        }
    }

