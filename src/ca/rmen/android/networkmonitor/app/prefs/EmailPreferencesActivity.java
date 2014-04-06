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

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.util.Log;

public class EmailPreferencesActivity extends PreferenceActivity {
    private static final String TAG = Constants.TAG + EmailPreferencesActivity.class.getSimpleName();

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.email_preferences, false);
        addPreferencesFromResource(R.xml.email_preferences);
        updateListPreferenceSummary(NetMonPreferences.PREF_EMAIL_INTERVAL, R.string.pref_summary_email_report_interval);
        updateListPreferenceSummary(NetMonPreferences.PREF_EMAIL_SERVER, R.string.pref_summary_email_server);
        updateListPreferenceSummary(NetMonPreferences.PREF_EMAIL_PORT, R.string.pref_summary_email_port);
        updateListPreferenceSummary(NetMonPreferences.PREF_EMAIL_SECURITY, R.string.pref_summary_email_security);
        updateListPreferenceSummary(NetMonPreferences.PREF_EMAIL_USER, R.string.pref_summary_email_user);
        updateListPreferenceSummary(NetMonPreferences.PREF_EMAIL_PASSWORD, R.string.pref_summary_email_password);
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
            if (NetMonPreferences.PREF_EMAIL_INTERVAL.equals(key)) {
                updateListPreferenceSummary(NetMonPreferences.PREF_EMAIL_INTERVAL, R.string.pref_summary_email_report_interval);
            } else if (NetMonPreferences.PREF_EMAIL_SERVER.equals(key)) {
                updateListPreferenceSummary(NetMonPreferences.PREF_EMAIL_SERVER, R.string.pref_summary_email_server);
            } else if (NetMonPreferences.PREF_EMAIL_PORT.equals(key)) {
                updateListPreferenceSummary(NetMonPreferences.PREF_EMAIL_PORT, R.string.pref_summary_email_port);
            } else if (NetMonPreferences.PREF_EMAIL_SECURITY.equals(key)) {
                updateListPreferenceSummary(NetMonPreferences.PREF_EMAIL_SECURITY, R.string.pref_summary_email_security);
            } else if (NetMonPreferences.PREF_EMAIL_USER.equals(key)) {
                updateListPreferenceSummary(NetMonPreferences.PREF_EMAIL_USER, R.string.pref_summary_email_user);
            } else if (NetMonPreferences.PREF_EMAIL_PASSWORD.equals(key)) {
                updateListPreferenceSummary(NetMonPreferences.PREF_EMAIL_PASSWORD, R.string.pref_summary_email_password);
            }
        }
    };

    private void updateListPreferenceSummary(CharSequence key, int summaryResId) {
        @SuppressWarnings("deprecation")
        Preference pref = getPreferenceManager().findPreference(key);
        CharSequence value;
        if (pref instanceof ListPreference) value = ((ListPreference) pref).getEntry();
        else if (pref instanceof EditTextPreference) value = ((EditTextPreference) pref).getText();
        else
            return;
        String summary = getString(summaryResId, value);
        pref.setSummary(summary);
    }

    private final OnPreferenceClickListener mOnPreferenceClickListener = new OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference preference) {
            Log.v(TAG, "onPreferenceClick: " + preference);
            return false;
        }
    };


}
