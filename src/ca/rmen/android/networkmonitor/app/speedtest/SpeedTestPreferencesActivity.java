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
package ca.rmen.android.networkmonitor.app.speedtest;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dialog.PreferenceDialog;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestResult.SpeedTestStatus;
import ca.rmen.android.networkmonitor.util.FileUtil;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Preferences for the speed test.
 */
public class SpeedTestPreferencesActivity extends PreferenceActivity { // NO_UCD (use default)
    private static final String TAG = Constants.TAG + SpeedTestPreferencesActivity.class.getSimpleName();

    private SpeedTestPreferences mSpeedTestPrefs;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.speed_test_preferences, false);
        addPreferencesFromResource(R.xml.speed_test_preferences);
        mSpeedTestPrefs = SpeedTestPreferences.getInstance(this);
        Preference prefSpeedTestEnabled = findPreference(SpeedTestPreferences.PREF_SPEED_TEST_ENABLED);
        if (NetMonPreferences.getInstance(this).getUpdateInterval() < NetMonPreferences.PREF_MIN_POLLING_INTERVAL) prefSpeedTestEnabled.setEnabled(false);
        SpeedTestResult result = mSpeedTestPrefs.getLastDownloadResult();
        if (result.status != SpeedTestStatus.SUCCESS) download();
        else
            updateDownloadUrlPreferenceSummary();
        updatePreferenceSummary(SpeedTestPreferences.PREF_SPEED_TEST_UPLOAD_SERVER, R.string.pref_summary_speed_test_upload_server);
        updatePreferenceSummary(SpeedTestPreferences.PREF_SPEED_TEST_UPLOAD_PORT, R.string.pref_summary_speed_test_upload_port);
        updatePreferenceSummary(SpeedTestPreferences.PREF_SPEED_TEST_UPLOAD_USER, R.string.pref_summary_speed_test_upload_user);
        updatePreferenceSummary(SpeedTestPreferences.PREF_SPEED_TEST_UPLOAD_PATH, R.string.pref_summary_speed_test_upload_path);
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "onStart");
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop");
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        super.onStop();
        boolean speedTestEnabled = mSpeedTestPrefs.isEnabled();
        // If the user enabled the speed test, make sure we have enough info.
        if (speedTestEnabled) {
            SpeedTestDownloadConfig downloadConfig = mSpeedTestPrefs.getDownloadConfig();
            if (!downloadConfig.isValid()) {
                mSpeedTestPrefs.setEnabled(false);
                PreferenceDialog.showInfoDialog(this, getString(R.string.speed_test_missing_info_dialog_title),
                        getString(R.string.speed_test_missing_info_dialog_message));
            }
        }
    }

    private final OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.v(TAG, "onSharedPreferenceChanged: key = " + key);
            // Show a warning when the user enables the speed test.
            if (SpeedTestPreferences.PREF_SPEED_TEST_ENABLED.equals(key)) {
                if (sharedPreferences.getBoolean(key, false)) {
                    PreferenceDialog.showWarningDialog(SpeedTestPreferencesActivity.this, getString(R.string.speed_test_warning_title),
                            getString(R.string.speed_test_warning_message));
                }

            }
            // If the user changed the download url, delete the previously downloaded file
            // and download the new one.
            else if (SpeedTestPreferences.PREF_SPEED_TEST_DOWNLOAD_URL.equals(key)) {
                FileUtil.clearCache(SpeedTestPreferencesActivity.this);
                download();
            } else if (SpeedTestPreferences.PREF_SPEED_TEST_UPLOAD_SERVER.equals(key)) {
                updatePreferenceSummary(SpeedTestPreferences.PREF_SPEED_TEST_UPLOAD_SERVER, R.string.pref_summary_speed_test_upload_server);
            } else if (SpeedTestPreferences.PREF_SPEED_TEST_UPLOAD_PORT.equals(key)) {
                updatePreferenceSummary(SpeedTestPreferences.PREF_SPEED_TEST_UPLOAD_PORT, R.string.pref_summary_speed_test_upload_port);
            } else if (SpeedTestPreferences.PREF_SPEED_TEST_UPLOAD_USER.equals(key)) {
                updatePreferenceSummary(SpeedTestPreferences.PREF_SPEED_TEST_UPLOAD_USER, R.string.pref_summary_speed_test_upload_user);
            } else if (SpeedTestPreferences.PREF_SPEED_TEST_UPLOAD_PATH.equals(key)) {
                updatePreferenceSummary(SpeedTestPreferences.PREF_SPEED_TEST_UPLOAD_PATH, R.string.pref_summary_speed_test_upload_path);
            }
        }
    };

    private void updatePreferenceSummary(CharSequence key, int summaryResId) {
        @SuppressWarnings("deprecation")
        Preference pref = getPreferenceManager().findPreference(key);
        if (pref instanceof EditTextPreference) {
            CharSequence value = ((EditTextPreference) pref).getText();
            String summary = getString(summaryResId, value);
            pref.setSummary(summary);
        } else
            return;
    }

    /**
     * Update the summary of the url preference, to include the size of the file we last retrieved.
     */
    private void updateDownloadUrlPreferenceSummary() {
        SpeedTestResult result = mSpeedTestPrefs.getLastDownloadResult();
        String size = result.status == SpeedTestStatus.SUCCESS ? String.format("%.3f", (float) result.fileBytes / 1000000) : "?";
        String url = mSpeedTestPrefs.getDownloadConfig().url;
        url = ellipsize(url, 30);
        String summary = getString(R.string.pref_summary_speed_test_download_url, url, size);
        @SuppressWarnings("deprecation")
        Preference pref = getPreferenceManager().findPreference(SpeedTestPreferences.PREF_SPEED_TEST_DOWNLOAD_URL);
        pref.setSummary(summary);
    }

    private static String ellipsize(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        String beginning = text.substring(0, maxLength / 2);
        String end = text.substring(text.length() - maxLength / 2);
        return beginning + "\u2026" + end;
    }

    /**
     * Download the file to use for the speed test.
     * We save the downlaod result so we can update the summary of the url preference to include the file size.
     */
    private void download() {
        final SpeedTestDownloadConfig config = mSpeedTestPrefs.getDownloadConfig();
        new AsyncTask<Void, Void, Void>() {


            @Override
            @SuppressWarnings("deprecation")
            protected void onPreExecute() {
                Preference pref = getPreferenceManager().findPreference(SpeedTestPreferences.PREF_SPEED_TEST_DOWNLOAD_URL);
                String summary = getString(R.string.pref_summary_speed_test_download_url, config.url, "?");
                pref.setSummary(summary);
            }

            @Override
            protected Void doInBackground(Void... params) {
                SpeedTestResult result = SpeedTestDownload.download(config);
                mSpeedTestPrefs.setLastDownloadResult(result);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                updateDownloadUrlPreferenceSummary();
            }

        }.execute();

    }
}
