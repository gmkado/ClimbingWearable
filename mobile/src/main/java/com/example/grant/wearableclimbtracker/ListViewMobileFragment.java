package com.example.grant.wearableclimbtracker;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.eventbus.RealmResultsEvent;
import com.example.mysynclibrary.realm.Climb;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import io.realm.OrderedRealmCollection;
import io.realm.RealmBaseAdapter;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

/**
 * Created by Grant on 10/17/2016.
 */
public class ListViewMobileFragment extends Fragment {
    private final String TAG = "ListViewMobileFragment";
    private ListView mListView;

    public ListViewMobileFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_listview_mobile, container, false);
        mListView = (ListView)rootView.findViewById(R.id.list);
        return rootView;
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }

    @Subscribe(sticky = true)
    public void onRealmResult(RealmResultsEvent event) {
        mListView.setAdapter(new ClimbListAdapter(getContext(), event.realmResults));
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }


    private class ClimbListAdapter extends RealmBaseAdapter<Climb> implements ListAdapter {
        private class ViewHolder {
            TextView grade;
            TextView date;
        }

        public ClimbListAdapter(@NonNull Context context, @Nullable OrderedRealmCollection<Climb> data) {
            super(context, data);
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
            Shared.ClimbType type = Shared.ClimbType.values()[climb.getType()];


            viewHolder.grade.setText(type.grades.get(gradeInd));

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
            viewHolder.date.setText(sdf.format(climb.getDate()));

            // set font color based on difficulty
            int fontColor;
            if(gradeInd<=type.getMaxGradeInd(Shared.ClimbLevel.beginner)) {
                fontColor = ColorTemplate.MATERIAL_COLORS[0];
            }else if(gradeInd<=type.getMaxGradeInd(Shared.ClimbLevel.intermediate)) {
                fontColor = ColorTemplate.MATERIAL_COLORS[1];
            }else if(gradeInd<=type.getMaxGradeInd(Shared.ClimbLevel.advanced)) {
                fontColor = ColorTemplate.MATERIAL_COLORS[2];
            }else {
                fontColor = ColorTemplate.MATERIAL_COLORS[3];
            }
            viewHolder.grade.setTextColor(fontColor);
            viewHolder.date.setTextColor(fontColor);

            return convertView;
        }
    }
}
