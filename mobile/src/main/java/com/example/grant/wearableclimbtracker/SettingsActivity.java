package com.example.grant.wearableclimbtracker;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;

import com.example.mysynclibrary.Shared;

import java.util.List;

import static com.example.mysynclibrary.Shared.KEY_GOAL_GRADE_BOULDER;
import static com.example.mysynclibrary.Shared.KEY_GOAL_GRADE_ROPES;
import static com.example.mysynclibrary.Shared.KEY_GOAL_NUMCLIMBS_BOULDER;
import static com.example.mysynclibrary.Shared.KEY_GOAL_NUMCLIMBS_ROPES;
import static com.example.mysynclibrary.Shared.KEY_GOAL_NUMSESSIONS_BOULDER;
import static com.example.mysynclibrary.Shared.KEY_GOAL_NUMSESSIONS_ROPES;
import static com.example.mysynclibrary.Shared.KEY_GOAL_VPOINTS_BOULDER;
import static com.example.mysynclibrary.Shared.KEY_GOAL_VPOINTS_ROPES;
import static com.example.mysynclibrary.Shared.KEY_WARMUP_MAXGRADE_BOULDER;
import static com.example.mysynclibrary.Shared.KEY_WARMUP_MAXGRADE_ROPES;
import static com.example.mysynclibrary.Shared.KEY_WARMUP_NUMCLIMBS_BOULDER;
import static com.example.mysynclibrary.Shared.KEY_WARMUP_NUMCLIMBS_ROPES;
import static com.example.mysynclibrary.Shared.KEY_WARMUP_ENABLED;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            // populate lists
            populateListPref((ListPreference)findPreference(KEY_WARMUP_MAXGRADE_BOULDER), Shared.ClimbType.bouldering.grades, 0);
            populateListPref((ListPreference)findPreference(KEY_WARMUP_MAXGRADE_ROPES), Shared.ClimbType.ropes.grades, 0);

            populateListPref((ListPreference)findPreference(KEY_GOAL_GRADE_BOULDER), Shared.ClimbType.bouldering.grades, 0);
            populateListPref((ListPreference)findPreference(KEY_GOAL_GRADE_ROPES), Shared.ClimbType.ropes.grades, 0);

            updateWarmupCategory();
            updateGoalCategory();

        }



        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        private void populateListPref(ListPreference pref, List<String> entries, Integer defaultValue) {
            pref.setEntries(entries.toArray(new CharSequence[entries.size()]));
            CharSequence[] values = new CharSequence[entries.size()];
            for(int i = 0; i < entries.size(); i++) {
                values[i] = entries.get(i).toString();
            }
            pref.setEntryValues(values);
            pref.setDefaultValue(defaultValue.toString());
        }


        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if(key.equals(KEY_WARMUP_ENABLED)) {
                updateWarmupCategory();
            }else {
                updatePreference(key);
            }
        }

        private void updatePreference(String key) {
            ListPreference listpref;
            NumberPickerPreference numpref;
            switch(key) {
                case KEY_WARMUP_MAXGRADE_BOULDER:
                    listpref= (ListPreference) findPreference(key);
                    if(listpref.isEnabled()) {
                        listpref.setSummary(getString(R.string.pref_summary_maxbouldergrade) + ": " + listpref.getEntry());
                    }else {
                        listpref.setSummary("");
                    }
                    break;
                case KEY_WARMUP_NUMCLIMBS_BOULDER:
                    numpref = (NumberPickerPreference) findPreference(key);
                    if(numpref.isEnabled()) {
                        numpref.setSummary(getString(R.string.pref_summary_numboulder) + ": " + Integer.toString(numpref.getValue()));
                    }else {
                        numpref.setSummary("");
                    }
                    break;
                case KEY_WARMUP_MAXGRADE_ROPES:
                    listpref = (ListPreference) findPreference(key);
                    if(listpref.isEnabled()) {
                        listpref.setSummary(getString(R.string.pref_summary_maxropesgrade) + ": " + listpref.getEntry());
                    }else {
                        listpref.setSummary("");
                    }
                    break;
                case KEY_WARMUP_NUMCLIMBS_ROPES:
                    numpref = (NumberPickerPreference) findPreference(key);
                    if(numpref.isEnabled()) {
                        numpref.setSummary(getString(R.string.pref_summary_numropes) + ": " + Integer.toString(numpref.getValue()));
                    }else {
                        numpref.setSummary("");
                    }
                    break;
                case KEY_GOAL_GRADE_BOULDER:
                    listpref = (ListPreference) findPreference(key);
                    listpref.setTitle(getString(R.string.pref_title_goalGradeBoulder) + ": " + listpref.getEntry());
                    break;
                case KEY_GOAL_VPOINTS_BOULDER:
                    numpref = (NumberPickerPreference) findPreference(key);
                    numpref.setTitle(getString(R.string.pref_title_goalVpointsBoulder) + ": " + Integer.toString(numpref.getValue()));
                    break;
                case KEY_GOAL_NUMCLIMBS_BOULDER:
                    numpref = (NumberPickerPreference) findPreference(key);
                    numpref.setTitle(getString(R.string.pref_title_goalNumclimbsBoulder) + ": " + Integer.toString(numpref.getValue()));
                    break;
                case KEY_GOAL_NUMSESSIONS_BOULDER:
                    numpref = (NumberPickerPreference) findPreference(key);
                    numpref.setTitle(getString(R.string.pref_title_goalNumsessionsBoulder) + ": " + Integer.toString(numpref.getValue()));
                    break;
                case KEY_GOAL_GRADE_ROPES:
                    listpref = (ListPreference) findPreference(key);
                    listpref.setTitle(getString(R.string.pref_title_goalGradeRopes) + ": " + listpref.getEntry());
                    break;
                case KEY_GOAL_VPOINTS_ROPES:
                    numpref = (NumberPickerPreference) findPreference(key);
                    numpref.setTitle(getString(R.string.pref_title_goalVpointsRopes) + ": " + Integer.toString(numpref.getValue()));
                    break;
                case KEY_GOAL_NUMCLIMBS_ROPES:
                    numpref = (NumberPickerPreference) findPreference(key);
                    numpref.setTitle(getString(R.string.pref_title_goalNumclimbsRopes) + ": " + Integer.toString(numpref.getValue()));
                    break;
                case KEY_GOAL_NUMSESSIONS_ROPES:
                    numpref = (NumberPickerPreference) findPreference(key);
                    numpref.setTitle(getString(R.string.pref_title_goalNumsessionsRopes) + ": " + Integer.toString(numpref.getValue()));
                    break;
            }
        }

        private void updateWarmupCategory() {
            // check warmup pref and set category appropriately
            if(((SwitchPreference)findPreference(KEY_WARMUP_ENABLED)).isChecked()) {
                findPreference(KEY_WARMUP_MAXGRADE_BOULDER).setEnabled(true);
                findPreference(KEY_WARMUP_NUMCLIMBS_BOULDER).setEnabled(true);
                findPreference(KEY_WARMUP_MAXGRADE_ROPES).setEnabled(true);
                findPreference(KEY_WARMUP_NUMCLIMBS_ROPES).setEnabled(true);
            }else {
                findPreference(KEY_WARMUP_MAXGRADE_BOULDER).setEnabled(false);
                findPreference(KEY_WARMUP_NUMCLIMBS_BOULDER).setEnabled(false);
                findPreference(KEY_WARMUP_MAXGRADE_ROPES).setEnabled(false);
                findPreference(KEY_WARMUP_NUMCLIMBS_ROPES).setEnabled(false);
            }
            updatePreference(KEY_WARMUP_MAXGRADE_BOULDER);
            updatePreference(KEY_WARMUP_NUMCLIMBS_BOULDER);
            updatePreference(KEY_WARMUP_MAXGRADE_ROPES);
            updatePreference(KEY_WARMUP_NUMCLIMBS_ROPES);
        }

        private void updateGoalCategory() {
            updatePreference(KEY_GOAL_GRADE_BOULDER);
            updatePreference(KEY_GOAL_VPOINTS_BOULDER);
            updatePreference(KEY_GOAL_NUMCLIMBS_BOULDER);
            updatePreference(KEY_GOAL_NUMSESSIONS_BOULDER);
            updatePreference(KEY_GOAL_GRADE_ROPES);
            updatePreference(KEY_GOAL_VPOINTS_ROPES);
            updatePreference(KEY_GOAL_NUMCLIMBS_ROPES);
            updatePreference(KEY_GOAL_NUMSESSIONS_ROPES);
        }


    }

}
