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

import com.example.grant.wearableclimbtracker.DbHelper;
import com.example.grant.wearableclimbtracker.MainActivity;
import com.example.grant.wearableclimbtracker.R;

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

        public SessionOverviewFragment() {
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_session_overview, container, false);
            pointsTextView = (TextView) rootView.findViewById(R.id.points_textview);
            countTextView = (TextView) rootView.findViewById(R.id.count_textview);

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
            DbHelper dbHelper = new DbHelper(getActivity());
            String query = "SELECT sum(" + DbHelper.ClimbEntry.COLUMN_GRADE_INDEX +
                            "), count(" + DbHelper.ClimbEntry.COLUMN_GRADE_INDEX +
                            ") FROM " + DbHelper.ClimbEntry.TABLE_NAME +
                            " WHERE type = " + mClimbType.ordinal() +
                            " AND " + DbHelper.ClimbEntry.COLUMN_DATETIME + " >=  date('now','localtime', 'start of day')";
            Cursor c = dbHelper.getReadableDatabase().rawQuery(query, null);
            c.moveToFirst();
            int sum = 0;
            int count = 0;
            if (c.getCount()>0) {
                // got a result
                sum = c.getInt(0);
                count = c.getInt(1);
            }
            pointsTextView.setText(String.format("POINTS: %d", sum));
            countTextView.setText(String.format("CLIMBS: %d", count));

            }
    }

