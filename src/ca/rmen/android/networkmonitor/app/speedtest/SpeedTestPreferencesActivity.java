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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.prefs.PreferenceFragmentActivity;
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestResult.SpeedTestStatus;
import ca.rmen.android.networkmonitor.util.FileUtil;
import ca.rmen.android.networkmonitor.util.Log;

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
    protected void onPause() {
        Log.v(TAG, "onPause");
        super.onPause();
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
                Intent intent = new Intent(PreferenceFragmentActivity.ACTION_SHOW_INFO_DIALOG);
                intent.putExtra(PreferenceFragmentActivity.EXTRA_DIALOG_TITLE, getString(R.string.speed_test_missing_info_dialog_title));
                intent.putExtra(PreferenceFragmentActivity.EXTRA_DIALOG_MESSAGE, getString(R.string.speed_test_missing_info_dialog_message));
                startActivity(intent);
            }
        }
    }

    private final OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.v(TAG, "onSharedPreferenceChanged: key = " + key);
            if (SpeedTestPreferences.PREF_SPEED_TEST_ENABLED.equals(key)) {
                if (sharedPreferences.getBoolean(key, false)) {
                    // We can't show a dialog directly here because we're a PreferenceActivity.
                    // We use this convoluted hack to ask the PreferenceFragmentActivity to show the dialog for us.
                    Intent intent = new Intent(PreferenceFragmentActivity.ACTION_SHOW_WARNING_DIALOG);
                    intent.putExtra(PreferenceFragmentActivity.EXTRA_DIALOG_TITLE, getString(R.string.speed_test_warning_title));
                    intent.putExtra(PreferenceFragmentActivity.EXTRA_DIALOG_MESSAGE, getString(R.string.speed_test_warning_message));
                    startActivity(intent);
                }

            } else if (SpeedTestPreferences.PREF_SPEED_TEST_DOWNLOAD_URL.equals(key)) {
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
        CharSequence value;
        if (pref instanceof ListPreference) value = ((ListPreference) pref).getEntry();
        else if (pref instanceof EditTextPreference) value = ((EditTextPreference) pref).getText();
        else
            return;
        String summary = getString(summaryResId, value);
        pref.setSummary(summary);
    }

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
