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
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.email.EmailPreferences;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences.LocationFetchingStrategy;
import ca.rmen.android.networkmonitor.app.service.NetMonNotification;
import ca.rmen.android.networkmonitor.util.Log;

public class AdvancedPreferencesActivity extends PreferenceActivity { // NO_UCD (use default)
    private static final String TAG = Constants.TAG + AdvancedPreferencesActivity.class.getSimpleName();
    private static final int ACTIVITY_REQUEST_CODE_IMPORT = 1;
    private static final String PREF_IMPORT = "PREF_IMPORT";

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        // The first time the user sees the notification preferences, we'll set the ringtone preference
        // to the default notification ringtone.
        if (!sharedPrefs.contains(NetMonPreferences.PREF_NOTIFICATION_RINGTONE)) NetMonPreferences.getInstance(this).setDefaultNotificationSoundUri();

        PreferenceManager.setDefaultValues(this, R.xml.adv_preferences, false);
        addPreferencesFromResource(R.xml.adv_preferences);
        updatePreferenceSummary(NetMonPreferences.PREF_TEST_SERVER, R.string.pref_summary_test_server);
        updatePreferenceSummary(NetMonPreferences.PREF_NOTIFICATION_RINGTONE, R.string.pref_summary_notification_ringtone);
        Preference testServerPreference = getPreferenceManager().findPreference(NetMonPreferences.PREF_TEST_SERVER);
        testServerPreference.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
        Preference importPreference = getPreferenceManager().findPreference(PREF_IMPORT);
        importPreference.setOnPreferenceClickListener(mOnPreferenceClickListener);
        Preference emailPreference = findPreference(EmailPreferences.PREF_EMAIL_REPORTS);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            emailPreference.setEnabled(false);
            emailPreference.setSummary(R.string.pref_email_unavailable);
        }
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
            Log.v(TAG, "onSharedPreferenceChanged: key = " + key);
            if (NetMonPreferences.PREF_TEST_SERVER.equals(key)) {
                updatePreferenceSummary(key, R.string.pref_summary_test_server);
            } else if (NetMonPreferences.PREF_NOTIFICATION_RINGTONE.equals(key)) {
                updatePreferenceSummary(key, R.string.pref_summary_notification_ringtone);
            } else if (NetMonPreferences.PREF_LOCATION_FETCHING_STRATEGY.equals(key)) {
                if (NetMonPreferences.getInstance(AdvancedPreferencesActivity.this).getLocationFetchingStrategy() == LocationFetchingStrategy.HIGH_ACCURACY) {
                    Intent intent = new Intent(PreferenceFragmentActivity.ACTION_CHECK_LOCATION_SETTINGS);
                    startActivity(intent);
                }
            } else if (NetMonPreferences.PREF_NOTIFICATION_ENABLED.equals(key)) {
                if (!NetMonPreferences.getInstance(AdvancedPreferencesActivity.this).getShowNotificationOnTestFailure())
                    NetMonNotification.dismissFailedTestNotification(AdvancedPreferencesActivity.this);
            }
        }
    };

    private void updatePreferenceSummary(String key, int summaryResId) {
        @SuppressWarnings("deprecation")
        Preference pref = getPreferenceManager().findPreference(key);
        CharSequence value;
        if (pref instanceof EditTextPreference) {
            value = ((EditTextPreference) pref).getText();
        } else if (pref instanceof RingtonePreference) {
            Uri ringtoneUri = NetMonPreferences.getInstance(this).getNotificationSoundUri();
            if (ringtoneUri == null) {
                value = getString(R.string.pref_value_notification_ringtone_silent);
            } else {
                Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
                if (ringtone == null) return;
                value = ringtone.getTitle(this);
            }
        } else {
            return;
        }
        String summary = getString(summaryResId, value);
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

    private final OnPreferenceChangeListener mOnPreferenceChangeListener = new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            // Ignore the value if it is empty.
            if (NetMonPreferences.PREF_TEST_SERVER.equals(preference.getKey())) {
                String newValueStr = (String) newValue;
                if (TextUtils.isEmpty(newValueStr)) return false;
                return true;
            }
            return true;
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
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
