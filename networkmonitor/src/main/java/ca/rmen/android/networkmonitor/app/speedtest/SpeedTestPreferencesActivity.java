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
 * Copyright (C) 2015 Rasmus Holm
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

import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import android.util.Log;

import java.util.Locale;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dialog.DialogFragmentFactory;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferenceFragmentCompat;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestResult.SpeedTestStatus;
import ca.rmen.android.networkmonitor.util.FileUtil;

/**
 * Preferences for the speed test.
 */
public class SpeedTestPreferencesActivity extends AppCompatActivity { // NO_UCD (use default)
    private static final String TAG = Constants.TAG + SpeedTestPreferencesActivity.class.getSimpleName();

    private SpeedTestPreferences mSpeedTestPrefs;
    private NetMonPreferenceFragmentCompat mPreferenceFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mPreferenceFragment = NetMonPreferenceFragmentCompat.newInstance(R.xml.speed_test_preferences);
        getSupportFragmentManager().
                beginTransaction().
                replace(android.R.id.content, mPreferenceFragment).
                commit();
        getSupportFragmentManager().executePendingTransactions();
        mSpeedTestPrefs = SpeedTestPreferences.getInstance(this);
        Preference prefSpeedTestEnabled = mPreferenceFragment.findPreference(SpeedTestPreferences.PREF_SPEED_TEST_ENABLED);
        if (NetMonPreferences.getInstance(this).isFastPollingEnabled())
            prefSpeedTestEnabled.setEnabled(false);
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
            SpeedTestDownloadConfig downloadConfig = mSpeedTestPrefs.getDownloadConfig(this);
            if (!downloadConfig.isValid()) {
                mSpeedTestPrefs.disable();
                DialogFragmentFactory.showInfoDialog(this, getString(R.string.speed_test_missing_info_dialog_title),
                        getString(R.string.speed_test_missing_info_dialog_message));
            }
        }
    }

    private final OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = (sharedPreferences, key) -> {
        Log.v(TAG, "onSharedPreferenceChanged: key = " + key);
        // Show a warning when the user enables the speed test.
        switch (key) {
            case SpeedTestPreferences.PREF_SPEED_TEST_ENABLED:
                if (sharedPreferences.getBoolean(key, false)) {
                    DialogFragmentFactory.showWarningDialog(SpeedTestPreferencesActivity.this, getString(R.string.speed_test_warning_title),
                            getString(R.string.speed_test_warning_message));
                }
                break;
            case SpeedTestPreferences.PREF_SPEED_TEST_INTERVAL:
                updatePreferenceSummary(key, R.string.pref_summary_speed_test_interval);
                break;
            // If the user changed the download url, delete the previously downloaded file
            // and download the new one.
            case SpeedTestPreferences.PREF_SPEED_TEST_DOWNLOAD_URL:
                FileUtil.clearCache(SpeedTestPreferencesActivity.this);
                download();
                break;
            case SpeedTestPreferences.PREF_SPEED_TEST_UPLOAD_SERVER:
                updatePreferenceSummary(key, R.string.pref_summary_speed_test_upload_server);
                break;
            case SpeedTestPreferences.PREF_SPEED_TEST_UPLOAD_PORT:
                updatePreferenceSummary(key, R.string.pref_summary_speed_test_upload_port);
                break;
            case SpeedTestPreferences.PREF_SPEED_TEST_UPLOAD_USER:
                updatePreferenceSummary(key, R.string.pref_summary_speed_test_upload_user);
                break;
            case SpeedTestPreferences.PREF_SPEED_TEST_UPLOAD_PATH:
                updatePreferenceSummary(key, R.string.pref_summary_speed_test_upload_path);
                break;
            default:
                break;
        }
    };

    private void updatePreferenceSummary(CharSequence key, int summaryResId) {
        Preference pref = mPreferenceFragment.findPreference(key);
        if (pref instanceof EditTextPreference) {
            CharSequence value = ((EditTextPreference) pref).getText();
            if (value == null) {
                value = "";
            }
            String summary = getString(summaryResId, value);
            pref.setSummary(summary);
        }
    }

    /**
     * Update the summary of the url preference, to include the size of the file we last retrieved.
     */
    private void updateDownloadUrlPreferenceSummary() {
        SpeedTestResult result = mSpeedTestPrefs.getLastDownloadResult();
        String size = result.status == SpeedTestStatus.SUCCESS ? String.format(Locale.getDefault(), "%.3f", (float) result.fileBytes / 1000000) : "?";
        String url = mSpeedTestPrefs.getDownloadConfig(this).url;
        url = ellipsize(url);
        String summary = getString(R.string.pref_summary_speed_test_download_url, url, size);
        Preference pref = mPreferenceFragment.findPreference(SpeedTestPreferences.PREF_SPEED_TEST_DOWNLOAD_URL);
        pref.setSummary(summary);
    }

    private static String ellipsize(String text) {
        int maxLength = 30;
        if (text.length() <= maxLength) return text;
        String beginning = text.substring(0, maxLength / 2);
        String end = text.substring(text.length() - maxLength / 2);
        return beginning + "\u2026" + end;
    }

    /**
     * Download the file to use for the speed test.
     * We save the download result so we can update the summary of the url preference to include the file size.
     */
    private void download() {
        final SpeedTestDownloadConfig config = mSpeedTestPrefs.getDownloadConfig(this);
        Preference pref = mPreferenceFragment.findPreference(SpeedTestPreferences.PREF_SPEED_TEST_DOWNLOAD_URL);
        String summary = getString(R.string.pref_summary_speed_test_download_url, config.url, "?");
        pref.setSummary(summary);
        SpeedTestDownload.download(config, result -> {
                    mSpeedTestPrefs.setLastDownloadResult(result);
                    updateDownloadUrlPreferenceSummary();
                }
        );
    }
}
