package com.example.grant.wearableclimbtracker;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.Switch;

import com.example.mysynclibrary.Shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.R.attr.entryValues;

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
        public final String KEY_WEAR_ENABLED = "wear_enabled_switch";
        public final String KEY_WARMUP_ENABLED = "warmup_enabled_switch";
        public final String KEY_MAXGRADE_BOULDER = "maxgrade_boulder_list";
        public final String KEY_NUMCLIMBS_BOULDER = "numclimbs_boulder_numpicker";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            // populate lists
            ListPreference maxGradePref = (ListPreference)findPreference(KEY_MAXGRADE_BOULDER);
            populateListPref(maxGradePref, Shared.ClimbType.bouldering.grades, 0);

            updateBoulderWarmupCategory();


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
                    ListPreference pref = (ListPreference) findPreference(key);
                    pref.setSummary("Max warmup bouldering grade: " + pref.getEntry());
                    break;
                case KEY_NUMCLIMBS_BOULDER:
                    NumberPickerPreference numPref = (NumberPickerPreference) findPreference(key);
                    numPref.setSummary("Minimum warmup bouldering climbs: " + Integer.toString(numPref.getValue()));
                    break;
                case KEY_WARMUP_ENABLED:
                    updateBoulderWarmupCategory();
                    break;

            }
        }

        private void updateBoulderWarmupCategory() {
            // check warmup pref and set category appropriately
            if(((SwitchPreference)findPreference(KEY_WARMUP_ENABLED)).isChecked()) {
                findPreference(KEY_MAXGRADE_BOULDER).setEnabled(true);
                findPreference(KEY_NUMCLIMBS_BOULDER).setEnabled(true);
            }else {
                findPreference(KEY_MAXGRADE_BOULDER).setEnabled(false);
                findPreference(KEY_NUMCLIMBS_BOULDER).setEnabled(false);
            }
        }
    }

}
