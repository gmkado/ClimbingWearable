package com.example.grant.wearableclimbtracker;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.eventbus.EditClimbDialogEvent;
import com.example.mysynclibrary.eventbus.RealmResultsEvent;
import com.example.mysynclibrary.realm.Climb;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.SimpleDateFormat;

import io.realm.OrderedRealmCollection;
import io.realm.RealmBaseAdapter;
import io.realm.Sort;

/**
 * Created by Grant on 10/17/2016.
 * // TODO: add ability to add/edit/delete climbs, keep track if edits were made today, so watch can be updated
 */
public class ListViewMobileFragment extends Fragment {
    private final String TAG = "ListViewMobileFragment";
    private ListView mListView;
    private Shared.ClimbType mClimbType;
    private ClimbListAdapter mAdapter;

    public ListViewMobileFragment() {

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
                EventBus.getDefault().post(new EditClimbDialogEvent(EditClimbDialogEvent.DialogActionType.OPEN_REQUEST,climb.getId()));
                return true;
            }
        });
        return rootView;
    }



    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }

    @Subscribe(sticky = true)
    public void onRealmResult(RealmResultsEvent event) {
        mClimbType = event.climbType;
        mAdapter = new ClimbListAdapter(event.realmResults);
        mListView.setAdapter(mAdapter);
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
            Shared.ClimbType type = Shared.ClimbType.values()[climb.getType()];

            if(gradeInd>-1) { // TODO: take this out, this was because I have some incorrectly created climbs
                viewHolder.grade.setText(type.grades.get(gradeInd));
            }
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
