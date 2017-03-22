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

import static com.example.mysynclibrary.Shared.KEY_MAXGRADE_BOULDER;
import static com.example.mysynclibrary.Shared.KEY_MAXGRADE_ROPES;
import static com.example.mysynclibrary.Shared.KEY_NUMCLIMBS_BOULDER;
import static com.example.mysynclibrary.Shared.KEY_NUMCLIMBS_ROPES;
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
            populateListPref((ListPreference)findPreference(KEY_MAXGRADE_BOULDER), Shared.ClimbType.bouldering.grades, 0);
            populateListPref((ListPreference)findPreference(KEY_MAXGRADE_ROPES), Shared.ClimbType.ropes.grades, 0);

            updateWarmupCategory();


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
            switch(key) {
                case KEY_MAXGRADE_BOULDER:
                    updateWarmupSummary(key);
                    break;
                case KEY_NUMCLIMBS_BOULDER:
                    updateWarmupSummary(key);
                    break;
                case KEY_MAXGRADE_ROPES:
                    updateWarmupSummary(key);
                    break;
                case KEY_NUMCLIMBS_ROPES:
                    updateWarmupSummary(key);
                    break;

                case KEY_WARMUP_ENABLED:
                    updateWarmupCategory();
                    break;

            }
        }

        private void updateWarmupSummary(String key) {
            switch(key) {
                case KEY_MAXGRADE_BOULDER:
                    ListPreference maxGradeBoulders = (ListPreference) findPreference(key);
                    if(maxGradeBoulders.isEnabled()) {
                        maxGradeBoulders.setSummary(getString(R.string.pref_summary_maxbouldergrade) + ": " + maxGradeBoulders.getEntry());
                    }else {
                        maxGradeBoulders.setSummary("");
                    }
                    break;
                case KEY_NUMCLIMBS_BOULDER:
                    NumberPickerPreference numBoulderClimbs = (NumberPickerPreference) findPreference(key);
                    if(numBoulderClimbs.isEnabled()) {
                        numBoulderClimbs.setSummary(getString(R.string.pref_summary_numboulder) + ": " + Integer.toString(numBoulderClimbs.getValue()));
                    }else {
                        numBoulderClimbs.setSummary("");
                    }
                    break;
                case KEY_MAXGRADE_ROPES:
                    ListPreference maxGradeRopes = (ListPreference) findPreference(key);
                    if(maxGradeRopes.isEnabled()) {
                        maxGradeRopes.setSummary(getString(R.string.pref_summary_maxropesgrade) + ": " + maxGradeRopes.getEntry());
                    }else {
                        maxGradeRopes.setSummary("");
                    }
                    break;
                case KEY_NUMCLIMBS_ROPES:
                    NumberPickerPreference numRopeClimbs = (NumberPickerPreference) findPreference(key);
                    if(numRopeClimbs.isEnabled()) {
                        numRopeClimbs.setSummary(getString(R.string.pref_summary_numropes) + ": " + Integer.toString(numRopeClimbs.getValue()));
                    }else {
                        numRopeClimbs.setSummary("");
                    }
                    break;
            }
        }

        private void updateWarmupCategory() {
            // check warmup pref and set category appropriately
            if(((SwitchPreference)findPreference(KEY_WARMUP_ENABLED)).isChecked()) {
                findPreference(KEY_MAXGRADE_BOULDER).setEnabled(true);
                findPreference(KEY_NUMCLIMBS_BOULDER).setEnabled(true);
                findPreference(KEY_MAXGRADE_ROPES).setEnabled(true);
                findPreference(KEY_NUMCLIMBS_ROPES).setEnabled(true);
            }else {
                findPreference(KEY_MAXGRADE_BOULDER).setEnabled(false);
                findPreference(KEY_NUMCLIMBS_BOULDER).setEnabled(false);
                findPreference(KEY_MAXGRADE_ROPES).setEnabled(false);
                findPreference(KEY_NUMCLIMBS_ROPES).setEnabled(false);
            }
            updateWarmupSummary(KEY_MAXGRADE_BOULDER);
            updateWarmupSummary(KEY_NUMCLIMBS_BOULDER);
            updateWarmupSummary(KEY_MAXGRADE_ROPES);
            updateWarmupSummary(KEY_NUMCLIMBS_ROPES);
        }


    }

}
