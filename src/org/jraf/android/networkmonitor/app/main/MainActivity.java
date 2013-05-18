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
package org.jraf.android.networkmonitor.app.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.app.service.NetMonService;

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
