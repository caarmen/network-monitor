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
 * Copyright (C) 2013-2015 Carmen Alvarez (c@rmen.ca)
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;

import com.squareup.otto.Subscribe;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.bus.NetMonBus;
import ca.rmen.android.networkmonitor.app.email.EmailPreferences;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences.LocationFetchingStrategy;
import ca.rmen.android.networkmonitor.app.service.NetMonNotification;
import ca.rmen.android.networkmonitor.util.Log;

public class AdvancedPreferencesActivity extends AppCompatActivity { // NO_UCD (use default)
    private static final String TAG = Constants.TAG + AdvancedPreferencesActivity.class.getSimpleName();
    private static final int ACTIVITY_REQUEST_CODE_IMPORT = 1;
    private static final int ACTIVITY_REQUEST_CODE_RINGTONE = 2;
    private static final String PREF_IMPORT = "PREF_IMPORT";
    private static final String PREF_COMPRESS = "PREF_COMPRESS";

    private NetMonPreferenceFragmentCompat mPreferenceFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        NetMonPreferences prefs = NetMonPreferences.getInstance(this);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        // The first time the user sees the notification preferences, we'll set the ringtone preference
        // to the default notification ringtone.
        if (!sharedPrefs.contains(NetMonPreferences.PREF_NOTIFICATION_RINGTONE)) NetMonPreferences.getInstance(this).setDefaultNotificationSoundUri();

        PreferenceManager.setDefaultValues(this, R.xml.adv_preferences, false);
        mPreferenceFragment = NetMonPreferenceFragmentCompat.newInstance(R.xml.adv_preferences);
        getSupportFragmentManager().
                beginTransaction().
                replace(android.R.id.content, mPreferenceFragment).
                commit();
        getSupportFragmentManager().executePendingTransactions();
        updatePreferenceSummary(NetMonPreferences.PREF_TEST_SERVER, R.string.pref_summary_test_server);
        updatePreferenceSummary(NetMonPreferences.PREF_NOTIFICATION_RINGTONE, R.string.pref_summary_notification_ringtone);
        Preference enableConnectionTest = mPreferenceFragment.findPreference(NetMonPreferences.PREF_ENABLE_CONNECTION_TEST);
        if (prefs.isFastPollingEnabled()) enableConnectionTest.setEnabled(false);
        setOnPreferenceChangeListeners(NetMonPreferences.PREF_TEST_SERVER);
        setOnPreferenceClickListeners(PREF_IMPORT, PREF_COMPRESS, NetMonPreferences.PREF_NOTIFICATION_RINGTONE);
        Preference emailPreference = mPreferenceFragment.findPreference(EmailPreferences.PREF_EMAIL_REPORTS);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            emailPreference.setEnabled(false);
            emailPreference.setSummary(R.string.pref_email_unavailable);
        }
    }

    private void setOnPreferenceClickListeners(String... keys) {
        for (String key : keys) {
            Preference preference = mPreferenceFragment.findPreference(key);
            preference.setOnPreferenceClickListener(mOnPreferenceClickListener);
        }
    }

    private void setOnPreferenceChangeListeners(String... keys) {
        for (String key : keys) {
            Preference preference = mPreferenceFragment.findPreference(key);
            preference.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        NetMonBus.getBus().register(this);
    }

    @Override
    protected void onStop() {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        NetMonBus.getBus().unregister(this);
        super.onStop();
    }


    private final OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            final NetMonPreferences prefs = NetMonPreferences.getInstance(AdvancedPreferencesActivity.this);
            Log.v(TAG, "onSharedPreferenceChanged: key = " + key);
            if (NetMonPreferences.PREF_TEST_SERVER.equals(key)) {
                updatePreferenceSummary(key, R.string.pref_summary_test_server);
            } else if (NetMonPreferences.PREF_NOTIFICATION_RINGTONE.equals(key)) {
                updatePreferenceSummary(key, R.string.pref_summary_notification_ringtone);
            } else if (NetMonPreferences.PREF_LOCATION_FETCHING_STRATEGY.equals(key)) {
                if (prefs.getLocationFetchingStrategy() == LocationFetchingStrategy.HIGH_ACCURACY) {
                    Intent intent = new Intent(PreferenceFragmentActivity.ACTION_CHECK_LOCATION_SETTINGS);
                    startActivity(intent);
                }
            } else if (NetMonPreferences.PREF_NOTIFICATION_ENABLED.equals(key)) {
                if (!prefs.getShowNotificationOnTestFailure()) NetMonNotification.dismissFailedTestNotification(AdvancedPreferencesActivity.this);
            } else if (NetMonPreferences.PREF_DB_RECORD_COUNT.equals(key)) {
                Intent intent = new Intent(PreferenceFragmentActivity.ACTION_CLEAR_OLD);
                startActivity(intent);
            }
        }
    };

    private void updatePreferenceSummary(final String key, final int summaryResId) {
        final Preference pref = mPreferenceFragment.findPreference(key);
        // RingtoneManager.getRingtone() actually does some disk reads.
        // Discovered this with StrictMode and monkey.
        // Ugly code (async task) to make it easier to find real StrictMode violations...
        new AsyncTask<Void, Void, CharSequence>() {

            @Override
            protected CharSequence doInBackground(Void... params) {
                if (pref instanceof EditTextPreference) {
                    return ((EditTextPreference) pref).getText();
                } else if (pref.getKey().equals(NetMonPreferences.PREF_NOTIFICATION_RINGTONE)) {
                    Uri ringtoneUri = NetMonPreferences.getInstance(AdvancedPreferencesActivity.this).getNotificationSoundUri();
                    if (ringtoneUri == null) {
                        return getString(R.string.pref_value_notification_ringtone_silent);
                    } else {
                        Ringtone ringtone = RingtoneManager.getRingtone(AdvancedPreferencesActivity.this, ringtoneUri);
                        if (ringtone == null) return null;
                        return ringtone.getTitle(AdvancedPreferencesActivity.this);
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(CharSequence value) {
                if (value == null) return;
                String summary = getString(summaryResId, value);
                pref.setSummary(summary);
            }

        }.execute();
    }

    private final Preference.OnPreferenceClickListener mOnPreferenceClickListener = new Preference.OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference preference) {
            Log.v(TAG, "onPreferenceClick: " + preference);
            if (PREF_IMPORT.equals(preference.getKey())) {

                Intent importIntent = new Intent(Intent.ACTION_GET_CONTENT);
                importIntent.setType("file/*");
                startActivityForResult(Intent.createChooser(importIntent, getResources().getText(R.string.pref_summary_import)), ACTIVITY_REQUEST_CODE_IMPORT);
            } else if (PREF_COMPRESS.equals(preference.getKey())) {
                Intent intent = new Intent(PreferenceFragmentActivity.ACTION_COMPRESS);
                startActivity(intent);

            } else if (NetMonPreferences.PREF_NOTIFICATION_RINGTONE.equals(preference.getKey())) {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, NetMonPreferences.getInstance(getApplicationContext()).getNotificationSoundUri());

                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.pref_title_notification_ringtone));
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE_RINGTONE);
            }
            return false;
        }
    };

    private final Preference.OnPreferenceChangeListener mOnPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            // Ignore the value if it is empty.
            if (NetMonPreferences.PREF_TEST_SERVER.equals(preference.getKey())) {
                String newValueStr = (String) newValue;
                return !TextUtils.isEmpty(newValueStr);
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
        } else if (requestCode == ACTIVITY_REQUEST_CODE_RINGTONE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                NetMonPreferences.getInstance(this).setNotificationSoundUri(uri);
                updatePreferenceSummary(NetMonPreferences.PREF_NOTIFICATION_RINGTONE, R.string.pref_summary_notification_ringtone);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Subscribe
    public void onDBOperationStarted(NetMonBus.DBOperationStarted event) {
        Log.d(TAG, "onDBOperationStarted() called with " + "event = [" + event + "]");
        mPreferenceFragment.findPreference(PREF_IMPORT).setEnabled(false);
        mPreferenceFragment.findPreference(PREF_COMPRESS).setEnabled(false);
        mPreferenceFragment.findPreference(NetMonPreferences.PREF_DB_RECORD_COUNT).setEnabled(false);
    }

    @Subscribe
    public void onDBOperationEnded(NetMonBus.DBOperationEnded event) {
        Log.d(TAG, "onDBOperationEnded() called with " + "event = [" + event + "]");
        mPreferenceFragment.findPreference(PREF_IMPORT).setEnabled(true);
        mPreferenceFragment.findPreference(PREF_COMPRESS).setEnabled(true);
        mPreferenceFragment.findPreference(NetMonPreferences.PREF_DB_RECORD_COUNT).setEnabled(true);
    }

}