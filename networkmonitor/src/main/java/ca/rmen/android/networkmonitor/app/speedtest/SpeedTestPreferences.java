/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2015 Carmen Alvarez (c@rmen.ca)
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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.File;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.util.FileUtil;
import android.util.Log;

/**
 * Convenience methods for getting/setting shared preferences related to the speed test.
 */
public class SpeedTestPreferences {

    private static final String TAG = Constants.TAG + SpeedTestPreferences.class.getSimpleName();
    private static final String FILE = "speedtest";

    static final String PREF_SPEED_TEST_ENABLED = "PREF_SPEED_TEST_ENABLED";
    static final String PREF_SPEED_TEST_DOWNLOAD_URL = "PREF_SPEED_TEST_DOWNLOAD_URL";
    static final String PREF_SPEED_TEST_INTERVAL = "PREF_SPEED_TEST_INTERVAL";
    static final String PREF_SPEED_TEST_UPLOAD_SERVER = "PREF_SPEED_TEST_UPLOAD_SERVER";
    static final String PREF_SPEED_TEST_UPLOAD_PORT = "PREF_SPEED_TEST_UPLOAD_PORT";
    static final String PREF_SPEED_TEST_UPLOAD_USER = "PREF_SPEED_TEST_UPLOAD_USER";
    static final String PREF_SPEED_TEST_UPLOAD_PATH = "PREF_SPEED_TEST_UPLOAD_PATH";

    static final int PREF_SPEED_TEST_INTERVAL_NETWORK_CHANGE = -2;
    static final int PREF_SPEED_TEST_INTERVAL_DBM_OR_NETWORK_CHANGE = -1;

    private static final String PREF_SPEED_TEST_UPLOAD_PASSWORD = "PREF_SPEED_TEST_UPLOAD_PASSWORD";

    private static final String PREF_SPEED_TEST_DEFAULT_UPLOAD_PORT = "21";
    private static final String PREF_SPEED_TEST_DEFAULT_UPLOAD_PATH = "/";
    private static final String PREF_SPEED_TEST_DEFAULT_INTERVAL = "1";

    private static SpeedTestPreferences INSTANCE = null;
    private final SharedPreferences mSharedPrefs;

    public static synchronized SpeedTestPreferences getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new SpeedTestPreferences(context.getApplicationContext());
        }
        return INSTANCE;
    }

    private SpeedTestPreferences(Context context) {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isEnabled() {
        return mSharedPrefs.getBoolean(PREF_SPEED_TEST_ENABLED, false);
    }

    public void disable() {
        Log.v(TAG, "disable");
        mSharedPrefs.edit().putBoolean(PREF_SPEED_TEST_ENABLED, false).apply();
    }

    int getSpeedTestInterval() {
        return Integer.valueOf(mSharedPrefs.getString(PREF_SPEED_TEST_INTERVAL, PREF_SPEED_TEST_DEFAULT_INTERVAL));
    }

    public SpeedTestUploadConfig getUploadConfig(Context context) {
        String server = mSharedPrefs.getString(PREF_SPEED_TEST_UPLOAD_SERVER, "").trim();
        int port = Integer.valueOf(mSharedPrefs.getString(PREF_SPEED_TEST_UPLOAD_PORT, PREF_SPEED_TEST_DEFAULT_UPLOAD_PORT));
        String user = mSharedPrefs.getString(PREF_SPEED_TEST_UPLOAD_USER, "").trim();
        String password = mSharedPrefs.getString(PREF_SPEED_TEST_UPLOAD_PASSWORD, "").trim();
        String path = mSharedPrefs.getString(PREF_SPEED_TEST_UPLOAD_PATH, PREF_SPEED_TEST_DEFAULT_UPLOAD_PATH).trim();

        File file = FileUtil.getCacheFile(context, FILE);
        return new SpeedTestUploadConfig(server, port, user, password, path, file);
    }

    public SpeedTestDownloadConfig getDownloadConfig(Context context) {
        String url = mSharedPrefs.getString(PREF_SPEED_TEST_DOWNLOAD_URL, "");
        File file = FileUtil.getCacheFile(context, FILE);
        return new SpeedTestDownloadConfig(url, file);
    }

    SpeedTestResult getLastDownloadResult() {
        return SpeedTestDownload.read(mSharedPrefs);
    }

    public void setLastDownloadResult(SpeedTestResult result) {
        Log.v(TAG, "setLastDownloadResult " + result);
        SpeedTestDownload.save(mSharedPrefs, result);
    }

}
