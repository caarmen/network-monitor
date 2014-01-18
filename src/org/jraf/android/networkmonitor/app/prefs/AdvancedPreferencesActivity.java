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
package org.jraf.android.networkmonitor.app.prefs;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import org.jraf.android.networkmonitor.R;

public class AdvancedPreferencesActivity extends PreferenceActivity {
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.adv_preferences, false);
        addPreferencesFromResource(R.xml.adv_preferences);
        updateListPreferenceSummary(NetMonPreferences.PREF_WAKE_INTERVAL, R.string.pref_summary_wake_interval);
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
            if (NetMonPreferences.PREF_WAKE_INTERVAL.equals(key)) {
                updateListPreferenceSummary(NetMonPreferences.PREF_WAKE_INTERVAL, R.string.pref_summary_wake_interval);
            } else if (NetMonPreferences.PREF_SCHEDULER.equals(key)) {
                updateListPreferenceSummary(NetMonPreferences.PREF_SCHEDULER, R.string.pref_summary_scheduler);
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

}
