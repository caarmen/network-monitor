/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013 Benoit 'BoD' Lubek (BoD@JRAF.org)
 * Copyright (C) 2013 Carmen Alvarez (c@rmen.ca)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.rmen.android.networkmonitor.app.prefs;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.log.LogActionsActivity;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences.LocationFetchingStrategy;
import ca.rmen.android.networkmonitor.util.Log;

public class AdvancedPreferencesActivity extends PreferenceActivity {
    private static final String TAG = Constants.TAG + AdvancedPreferencesActivity.class.getSimpleName();
    private static final int ACTIVITY_REQUEST_CODE_IMPORT = 1;
    private static final String PREF_IMPORT = "PREF_IMPORT";

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.adv_preferences, false);
        addPreferencesFromResource(R.xml.adv_preferences);
        updateListPreferenceSummary(NetMonPreferences.PREF_CELL_ID_FORMAT, R.string.pref_summary_cell_id_format);
        updateListPreferenceSummary(NetMonPreferences.PREF_WAKE_INTERVAL, R.string.pref_summary_wake_interval);
        updateListPreferenceSummary(NetMonPreferences.PREF_SCHEDULER, R.string.pref_summary_scheduler);
        updateListPreferenceSummary(NetMonPreferences.PREF_LOCATION_FETCHING_STRATEGY, R.string.pref_summary_location_fetching_strategy);
        Preference importPreference = getPreferenceManager().findPreference(PREF_IMPORT);
        importPreference.setOnPreferenceClickListener(mOnPreferenceClickListener);
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
            if (NetMonPreferences.PREF_CELL_ID_FORMAT.equals(key)) {
                updateListPreferenceSummary(NetMonPreferences.PREF_CELL_ID_FORMAT, R.string.pref_summary_cell_id_format);
            } else if (NetMonPreferences.PREF_WAKE_INTERVAL.equals(key)) {
                updateListPreferenceSummary(NetMonPreferences.PREF_WAKE_INTERVAL, R.string.pref_summary_wake_interval);
            } else if (NetMonPreferences.PREF_SCHEDULER.equals(key)) {
                updateListPreferenceSummary(NetMonPreferences.PREF_SCHEDULER, R.string.pref_summary_scheduler);
            } else if (NetMonPreferences.PREF_LOCATION_FETCHING_STRATEGY.equals(key)) {
                updateListPreferenceSummary(NetMonPreferences.PREF_LOCATION_FETCHING_STRATEGY, R.string.pref_summary_location_fetching_strategy);
                if (NetMonPreferences.getInstance(AdvancedPreferencesActivity.this).getLocationFetchingStrategy() == LocationFetchingStrategy.HIGH_ACCURACY) {
                    Intent intent = new Intent(LogActionsActivity.ACTION_CHECK_LOCATION_SETTINGS);
                    startActivity(intent);
                }
            }
        }
    };

    private void updateListPreferenceSummary(CharSequence key, int summaryResId) {
        @SuppressWarnings("deprecation")
        ListPreference pref = (ListPreference) getPreferenceManager().findPreference(key);
        CharSequence entry = pref.getEntry();
        String summary = getString(summaryResId, entry);
        pref.setSummary(summary);
    }

    private final OnPreferenceClickListener mOnPreferenceClickListener = new OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference preference) {
            Log.v(TAG, "onPreferenceClick: " + preference);
            if (PREF_IMPORT.equals(preference.getKey())) {

                Intent importIntent = new Intent(Intent.ACTION_GET_CONTENT);
                importIntent.setType("file/*");
                startActivityForResult(Intent.createChooser(importIntent, getResources().getText(R.string.pref_summary_import)), ACTIVITY_REQUEST_CODE_IMPORT);
            }
            return false;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "onActivityResult: requestCode =  " + requestCode + ", resultCode = " + resultCode + ", data=" + data);
        /**
         * Allow the user to choose a DB to import
         */
        if (requestCode == ACTIVITY_REQUEST_CODE_IMPORT) {
            if (resultCode == Activity.RESULT_OK) {
                Intent intent = new Intent(PreferenceFragmentActivity.ACTION_IMPORT);
                intent.putExtra(PreferenceFragmentActivity.EXTRA_IMPORT_URI, data.getData());
                startActivity(intent);
            }
        }
    }

}
