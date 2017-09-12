package com.example.grant.wearableclimbtracker;

import android.os.Bundle;
import android.support.annotation.NonNull;
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
import com.example.mysynclibrary.realm.Climb;
import com.example.mysynclibrary.realm.ClimbFields;
import com.example.mysynclibrary.realm.Gym;
import com.example.mysynclibrary.realm.GymFields;
import com.github.clans.fab.FloatingActionButton;

import java.util.UUID;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmRecyclerViewAdapter;

import static com.example.mysynclibrary.realm.ISyncableRealmObject.SyncState.DIRTY;

/**
 * A fragment representing a list of Gym RealmObjects.
 * <p/>
 */
public class GymListFragment extends Fragment {
    private Realm mRealm;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public GymListFragment() {
    }

    @SuppressWarnings("unused")
    public static GymListFragment newInstance() {
        return new GymListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gym_list, container, false);
        mRealm = Realm.getDefaultInstance();
        // Set the adapter
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_gymlist);
        recyclerView.setAdapter(new MyGymRecyclerViewAdapter(mRealm.where(Gym.class).findAll()));

        FloatingActionButton addGymButton = (FloatingActionButton)view.findViewById(R.id.fab_add_gym);
        addGymButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // show an edited dialog
                new MaterialDialog.Builder(getActivity())
                        .title("Input Gym Name")
                        .content("")
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .input("e.g. PG Sunnyvale", null, false, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, final CharSequence input) {
                                mRealm.executeTransactionAsync(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        Gym unmanagedGym = new Gym(input.toString());
                                        realm.copyToRealm(unmanagedGym);
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

    class MyGymRecyclerViewAdapter extends RealmRecyclerViewAdapter<Gym, MyGymRecyclerViewAdapter.ViewHolder> {
        MyGymRecyclerViewAdapter(OrderedRealmCollection<Gym> data) {
            super(data, true);
            setHasStableIds(true);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.listitem_gym, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = getItem(position);
            assert holder.mItem != null;

            holder.gymName.setText(holder.mItem.getName());
            holder.numAreas.setText(String.format("%s areas", Integer.toString(holder.mItem.getAreas().size())));
            long numClimbs = mRealm.where(Climb.class).equalTo(ClimbFields.GYM.ID, holder.mItem.getId()).count();
            holder.numClimbs.setText(String.format("%s climbs", Long.toString(numClimbs)));
            final String gymId = holder.mItem.getId();
            holder.menu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu menu = new PopupMenu(getContext(), holder.menu);
                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch(item.getItemId()) {
                                case R.id.item_edit:
                                    // show an edited dialog
                                    new MaterialDialog.Builder(getActivity())
                                            .title("Input Gym Name")
                                            .content("")
                                            .inputType(InputType.TYPE_CLASS_TEXT)
                                            .input("e.g. PG Sunnyvale", holder.mItem.getName(), false, new MaterialDialog.InputCallback() {
                                                @Override
                                                public void onInput(@NonNull MaterialDialog dialog, final CharSequence input) {
                                                    mRealm.executeTransactionAsync(new Realm.Transaction() {
                                                        @Override
                                                        public void execute(Realm realm) {
                                                            realm.where(Gym.class).equalTo(GymFields.ID, gymId).findFirst().setName(input.toString());
                                                        }
                                                    });
                                                }
                                            })
                                            .show();
                                    break;
                                case R.id.item_editareas:
                                    Fragment fragment = AreaListFragment.newInstance(gymId);
                                    getActivity().getSupportFragmentManager().beginTransaction().addToBackStack(null)
                                            .replace(R.id.content_main, fragment).commit();  // Add to backstack so back button brings us back to main page
                                    getActivity().setTitle("Areas"); // TODO: Set title to gym name
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
            //noinspection ConstantConditions
            return UUID.fromString(getItem(index).getId()).getMostSignificantBits();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final View mView;
            final TextView numAreas;
            private final TextView numClimbs;
            TextView gymName;

            Gym mItem;
            public ImageButton menu;

            ViewHolder(View view) {
                super(view);
                mView = view;
                gymName = (TextView) view.findViewById(R.id.textview_gymname);
                menu = (ImageButton) view.findViewById(R.id.imagebutton_menu);
                numAreas = (TextView) view.findViewById(R.id.textView_numAreas);
                numClimbs = (TextView) view.findViewById(R.id.textView_numClimbs);
            }

        }
    }
}
