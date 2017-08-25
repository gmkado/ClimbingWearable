package com.example.grant.wearableclimbtracker;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.example.mysynclibrary.realm.Area;
import com.example.mysynclibrary.realm.Gym;
import com.farbod.labelledspinner.LabelledSpinner;
import com.github.clans.fab.FloatingActionButton;

import java.util.UUID;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;

/**
 * A fragment representing a list of Area RealmObjects.
 * <p/>
 */
public class AreaListFragment extends Fragment {
    private static final String TAG = "AreaListFragment";
    private static final String ARG_GYMUUID = "gymUUID";
    private Realm mRealm;
    private String mGymID;

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

        mGymID = getArguments().getString(ARG_GYMUUID);

        recyclerView.setAdapter(new MyAreaRecyclerViewAdapter(mRealm.where(Area.class).equalTo("gym.id", mGymID).findAll()));

        FloatingActionButton addAreaButton = (FloatingActionButton)view.findViewById(R.id.fab_add_area);
        addAreaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createEditAreaDialog(null);
            }
        });
        return view;
    }

    private void createEditAreaDialog(final String areaId) {
        // show an edit dialog
        Area area = null;
        if(areaId != null) {
            area = mRealm.where(Area.class).equalTo("id", areaId).findFirst();
        }

        final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title("Input Area Name")
                .customView(R.layout.dialog_add_area, true)
                .positiveText("OK")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull final MaterialDialog dialog, @NonNull DialogAction which) {

                        mRealm.executeTransactionAsync(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                Area area;
                                if(areaId != null) {
                                    area = realm.where(Area.class).equalTo("id", areaId).findFirst();
                                }else {
                                    area = realm.createObject(Area.class, UUID.randomUUID().toString());
                                    area.setGym(realm.where(Gym.class).equalTo("id", mGymID).findFirst());
                                }

                                // get the name from the edittext
                                EditText edittext = (EditText)dialog.getCustomView().findViewById(R.id.editText_areaname);
                                area.setName(edittext.getText().toString());

                                // get the type from the spinner
                                LabelledSpinner spinner = (LabelledSpinner) dialog.getCustomView().findViewById(R.id.spinner_areatype);
                                area.setType(Area.AreaType.values()[spinner.getSpinner().getSelectedItemPosition()]);
                            }
                        });

                    }
                })
                .build();

        EditText edittext = (EditText)dialog.getCustomView().findViewById(R.id.editText_areaname);
        edittext.setInputType(InputType.TYPE_CLASS_TEXT);
        edittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                // only enable if string is not empty
                dialog.getActionButton(DialogAction.POSITIVE).setEnabled(!s.toString().isEmpty());
            }
        });

        LabelledSpinner spinner = (LabelledSpinner) dialog.getCustomView().findViewById(R.id.spinner_areatype);
        spinner.setCustomAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, Area.AreaType.getStringArray()));

        if(area !=null) {
            spinner.setSelection(area.getType().ordinal());
            edittext.setText(area.getName());

        }else {
            spinner.setSelection(0);
            dialog.getActionButton(DialogAction.POSITIVE).setEnabled(false); // require a nonempty name
        }
        dialog.show();

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
            holder.areaType.setText(holder.mItem.getType().name());
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
                                    createEditAreaDialog(areaId);
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

        class ViewHolder extends RecyclerView.ViewHolder {
            final View mView;
            TextView areaName;
            Area mItem;
            public ImageButton menu;
            TextView areaType;

            ViewHolder(View view) {
                super(view);
                mView = view;
                areaName = (TextView) view.findViewById(R.id.textview_areaname);
                areaType = (TextView) view.findViewById(R.id.textview_areatype);
                menu = (ImageButton) view.findViewById(R.id.imagebutton_menu);
            }

        }
    }
}
