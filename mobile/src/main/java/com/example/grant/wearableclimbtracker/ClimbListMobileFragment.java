package com.example.grant.wearableclimbtracker;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.eventbus.EditClimbDialogEvent;
import com.example.mysynclibrary.eventbus.ListScrollEvent;
import com.example.mysynclibrary.realm.Climb;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmBaseAdapter;
import io.realm.RealmQuery;
import io.realm.RealmResults;

/**
 * Created by Grant on 10/17/2016.
 */
public class ClimbListMobileFragment extends Fragment {
    private final String TAG = "ClimbListMobileFragment";
    private ListView mListView;
    private ClimbListAdapter mAdapter;
    private RealmResults<Climb> mResult;
    private Realm mRealm;

    public ClimbListMobileFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_listview_mobile, container, false);
        mListView = (ListView)rootView.findViewById(R.id.list);
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                // Create and show the dialog.
                Climb climb = mAdapter.getItem(i);
                EventBus.getDefault().post(new EditClimbDialogEvent(climb.getId(), EditClimbDialogEvent.EditClimbMode.EDIT));
                return true;
            }
        });
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            private int mLastFirstVisibleItem;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if(mLastFirstVisibleItem<firstVisibleItem)
                {
                    Log.i("SCROLLING DOWN","TRUE");
                    EventBus.getDefault().post(new ListScrollEvent(ListScrollEvent.ScrollType.down));
                }
                if(mLastFirstVisibleItem>firstVisibleItem)
                {
                    Log.i("SCROLLING UP","TRUE");
                    EventBus.getDefault().post(new ListScrollEvent(ListScrollEvent.ScrollType.up));
                }
                mLastFirstVisibleItem=firstVisibleItem;

            }
        });

        // populate the listview
        mRealm = Realm.getDefaultInstance();
        invalidateRealmResult();
        mAdapter = new ClimbListAdapter(mResult);
        mListView.setAdapter(mAdapter);
        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    private void invalidateRealmResult() {
        mResult = mRealm.where(Climb.class).findAll();
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }

    @Override
    public void onStop() {
        super.onStop();
        //EventBus.getDefault().unregister(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        //EventBus.getDefault().register(this);
    }



    private class ClimbListAdapter extends RealmBaseAdapter<Climb> implements ListAdapter {
        private class ViewHolder {
            TextView grade;
            TextView date;
        }

        public ClimbListAdapter(@Nullable OrderedRealmCollection<Climb> data) {
            super(data);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if(convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_climb_mobile, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.grade = (TextView) convertView.findViewById(R.id.grade_textview);
                viewHolder.date = (TextView) convertView.findViewById(R.id.date_textview);
                convertView.setTag(viewHolder);
            }else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            Climb climb = adapterData.get(position);
            int gradeInd = climb.getGrade();
            Shared.ClimbType type = climb.getType();

            viewHolder.grade.setText(type.grades.get(gradeInd));

            DateFormat df = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            // TODO: add send/attempt info

            // set font color based on difficulty
            int fontColor;
            if(gradeInd<=type.getIndexOfMaxGradeForLevel(Shared.ClimbLevel.beginner)) {
                fontColor = ColorTemplate.MATERIAL_COLORS[0];
            }else if(gradeInd<=type.getIndexOfMaxGradeForLevel(Shared.ClimbLevel.intermediate)) {
                fontColor = ColorTemplate.MATERIAL_COLORS[1];
            }else if(gradeInd<=type.getIndexOfMaxGradeForLevel(Shared.ClimbLevel.advanced)) {
                fontColor = ColorTemplate.MATERIAL_COLORS[2];
            }else {
                fontColor = ColorTemplate.MATERIAL_COLORS[3];
            }
            viewHolder.grade.setTextColor(fontColor);
            //viewHolder.date.setTextColor(fontColor);

            return convertView;
        }
    }
}
