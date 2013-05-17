package org.jraf.android.networkmonitor.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;

public class MainActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        addPreferencesFromResource(R.xml.preferences);
        updateIntervalSummary();
    }

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
    }

    @Override
    protected void onStop() {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        super.onStop();
    }


    private final OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (Constants.PREF_SERVICE_ENABLED.equals(key)) {
                if (sharedPreferences.getBoolean(Constants.PREF_SERVICE_ENABLED, Constants.PREF_SERVICE_ENABLED_DEFAULT)) {
                    startService(new Intent(MainActivity.this, NetMonService.class));
                }
            } else if (Constants.PREF_UPDATE_INTERVAL.equals(key)) {
                updateIntervalSummary();
            }
        }
    };

    private void updateIntervalSummary() {
        String value = PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.PREF_UPDATE_INTERVAL, Constants.PREF_UPDATE_INTERVAL_DEFAULT);
        int labelIndex = 0;
        int i = 0;
        for (String v : getResources().getStringArray(R.array.preferences_updateInterval_values)) {
            if (v.equals(value)) {
                labelIndex = i;
                break;
            }
            i++;
        }
        String[] labels = getResources().getStringArray(R.array.preferences_updateInterval_labels);
        findPreference(Constants.PREF_UPDATE_INTERVAL).setSummary(labels[labelIndex]);
    }
}
