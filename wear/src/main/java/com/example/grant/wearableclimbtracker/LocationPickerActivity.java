package com.example.grant.wearableclimbtracker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WearableRecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mysynclibrary.realm.Area;
import com.example.mysynclibrary.realm.AreaFields;
import com.example.mysynclibrary.realm.Gym;
import com.example.mysynclibrary.realm.GymFields;

import java.util.UUID;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

import static com.example.mysynclibrary.realm.ISyncableRealmObject.SyncState.DELETE;

/**
 * Created by Grant on 9/20/2017.
 * Step through picking a gym or area
 */
public class LocationPickerActivity extends WearableActivity{
    private static final String TAG = "LocationPickerActivity";
    static final String EXTRA_GYM_ID = "gymId";
    static final String EXTRA_AREA_ID = "areaId";
    private Gym selectedGym;
    private Area selectedArea;
    private Realm mRealm;
    private WearableRecyclerView mListView;
    private Button mClearButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        Bundle extras = getIntent().getExtras();
        mRealm = Realm.getDefaultInstance();

        mListView = (WearableRecyclerView)findViewById(R.id.listView);
        // set the listview margins
        mListView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));


        RealmResults<Gym> gymResults = mRealm.where(Gym.class)
                .notEqualTo(GymFields.SYNC_STATE, DELETE.name()).findAll();
        if(gymResults.isEmpty()) {
            Toast.makeText(this, "No gyms found, add some in mobile app", Toast.LENGTH_LONG).show();
            finishWithResult();
        }else {
            mListView.setAdapter(new GymListAdapter(this, gymResults));
        }

        mClearButton = (Button) findViewById(R.id.button_clearlocation);
        mClearButton.setText("No gym");
        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedGym = null;
                selectedArea = null;
                finishWithResult();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    private void finishWithResult() {
        Intent result = new Intent();
        result.putExtra(EXTRA_GYM_ID, selectedGym == null? null: selectedGym.getId());
        result.putExtra(EXTRA_AREA_ID, selectedArea==null?null:selectedArea.getId());

        setResult(RESULT_OK, result);
        finish();
    }

    class GymListAdapter extends RealmRecyclerViewAdapter<Gym, GymListAdapter.ViewHolder> {

        GymListAdapter(Context context, OrderedRealmCollection<Gym> data) {
            super(data, true);
            setHasStableIds(true);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public long getItemId(int index) {
            return UUID.fromString(getItem(index).getId()).getMostSignificantBits();
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final Gym gym = getItem(position);
            holder.name.setText(gym.getName());
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedGym = gym;
                    RealmResults<Area> areaResults = mRealm.where(Area.class)
                            .equalTo(AreaFields.GYM.ID, gym.getId())
                            .notEqualTo(AreaFields.SYNC_STATE, DELETE.name()).findAll();
                    if(areaResults.isEmpty()) {
                        Toast.makeText(LocationPickerActivity.this, "No areas found for this gym, add some in mobile app", Toast.LENGTH_LONG).show();
                        finishWithResult();
                    }else {
                        mListView.setAdapter(new AreaListAdapter(LocationPickerActivity.this, areaResults));
                        mClearButton.setText("No area");
                        mClearButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                selectedArea = null;
                                finishWithResult();
                            }
                        });
                    }
                }
            });
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            View mView;

            ViewHolder(View view) {
                super(view);
                name = (TextView) view.findViewById(android.R.id.text1);
                mView = view;
            }
        }

    }

    class AreaListAdapter extends RealmRecyclerViewAdapter<Area, AreaListAdapter.ViewHolder> {

        AreaListAdapter(Context context, OrderedRealmCollection<Area> data) {
            super(data, true);
            setHasStableIds(true);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public long getItemId(int index) {
            return UUID.fromString(getItem(index).getId()).getMostSignificantBits();
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final Area area = getItem(position);
            holder.name.setText(area.getName());
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedArea = area;
                    finishWithResult();
                }
            });
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            View mView;

            ViewHolder(View view) {
                super(view);
                name = (TextView) view.findViewById(android.R.id.text1);
                mView = view;
            }
        }

    }
}
