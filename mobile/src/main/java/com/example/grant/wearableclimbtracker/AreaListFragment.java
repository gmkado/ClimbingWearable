package com.example.grant.wearableclimbtracker;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.example.mysynclibrary.realm.Area;
import com.example.mysynclibrary.realm.Gym;
import com.github.clans.fab.FloatingActionButton;

import java.util.UUID;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmRecyclerViewAdapter;

/**
 * A fragment representing a list of Area RealmObjects.
 * <p/>
 */
public class AreaListFragment extends Fragment {
    private static final String TAG = "AreaListFragment";
    private static final String ARG_GYMUUID = "gymUUID";
    private Realm mRealm;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AreaListFragment() {
    }

    @SuppressWarnings("unused")
    public static AreaListFragment newInstance(String gymUUID) {
        AreaListFragment fragment = new AreaListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GYMUUID, gymUUID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_area_list, container, false);
        mRealm = Realm.getDefaultInstance();
        // Set the adapter
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_arealist);

        final String gymID = getArguments().getString(ARG_GYMUUID);

        recyclerView.setAdapter(new MyAreaRecyclerViewAdapter(mRealm.where(Area.class).equalTo("gym.id", gymID).findAll()));

        FloatingActionButton addAreaButton = (FloatingActionButton)view.findViewById(R.id.fab_add_area);
        addAreaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // show an edit dialog
                new MaterialDialog.Builder(getActivity())
                        .title("Input Area Name")
                        .content("")
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .input("e.g. backwall", null, false, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, final CharSequence input) {
                                mRealm.executeTransactionAsync(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        Area area = realm.createObject(Area.class, UUID.randomUUID().toString());
                                        area.setGym(realm.where(Gym.class).equalTo("id", gymID).findFirst());
                                        area.setName(input.toString());
                                    }
                                });
                            }
                        })
                        .show();
            }
        });
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRealm.close();
    }

    class MyAreaRecyclerViewAdapter extends RealmRecyclerViewAdapter<Area, MyAreaRecyclerViewAdapter.ViewHolder> {
        public MyAreaRecyclerViewAdapter(OrderedRealmCollection<Area> data) {
            super(data, true);
            setHasStableIds(true);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.listitem_area, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = getItem(position);
            holder.areaName.setText(holder.mItem.getName());

            final String areaId = holder.mItem.getId();
            holder.menu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu menu = new PopupMenu(getContext(), holder.menu);
                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch(item.getItemId()) {
                                case R.id.item_edit:
                                    // show an edit dialog
                                    new MaterialDialog.Builder(getActivity())
                                            .title("Input Area Name")
                                            .content("")
                                            .inputType(InputType.TYPE_CLASS_TEXT)
                                            .input("e.g. backwall", holder.mItem.getName(), false, new MaterialDialog.InputCallback() {
                                                @Override
                                                public void onInput(MaterialDialog dialog, final CharSequence input) {
                                                    mRealm.executeTransactionAsync(new Realm.Transaction() {
                                                        @Override
                                                        public void execute(Realm realm) {
                                                            realm.where(Area.class).equalTo("id", areaId).findFirst().setName(input.toString());
                                                        }
                                                    });
                                                }
                                            })
                                            .show();
                                    break;
                                case R.id.item_delete:
                                    // TODO: show "are you sure" dialog
                                    break;
                            }
                            return true;
                        }
                    });

                    menu.inflate(R.menu.menu_gym_listitem);
                    menu.show();
                }
            });
        }


        @Override
        public long getItemId(int index) {
            return UUID.fromString(getItem(index).getId()).getMostSignificantBits();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public TextView areaName;
            public Area mItem;
            public ImageButton menu;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                areaName = (TextView) view.findViewById(R.id.textview_areaname);
                menu = (ImageButton) view.findViewById(R.id.imagebutton_menu);
            }

        }
    }
}
