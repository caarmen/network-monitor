/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014 Carmen Alvarez (c@rmen.ca)
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

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.util.FileUtil;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Convenience methods for getting/setting shared preferences related to the speed test.
 */
public class SpeedTestPreferences {

    private static final String TAG = Constants.TAG + SpeedTestPreferences.class.getSimpleName();
    private static final String FILE = "speedtest";
    private final Context mContext;


    static final String PREF_SPEED_TEST_ENABLED = "PREF_SPEED_TEST_ENABLED";
    static final String PREF_SPEED_TEST_DOWNLOAD_URL = "PREF_SPEED_TEST_DOWNLOAD_URL";
    static final String PREF_SPEED_TEST_UPLOAD_SERVER = "PREF_SPEED_TEST_UPLOAD_SERVER";
    static final String PREF_SPEED_TEST_UPLOAD_PORT = "PREF_SPEED_TEST_UPLOAD_PORT";
    static final String PREF_SPEED_TEST_UPLOAD_USER = "PREF_SPEED_TEST_UPLOAD_USER";
    static final String PREF_SPEED_TEST_UPLOAD_PASSWORD = "PREF_SPEED_TEST_UPLOAD_PASSWORD";
    static final String PREF_SPEED_TEST_UPLOAD_PATH = "PREF_SPEED_TEST_UPLOAD_PATH";
    private static final String PREF_SPEED_TEST_LAST_DOWNLOAD_RESULT = "PREF_SPEED_TEST_LAST_DOWNLOAD_RESULT";

    private static final String PREF_SPEED_TEST_DEFAULT_UPLOAD_PORT = "21";
    private static final String PREF_SPEED_TEST_DEFAULT_UPLOAD_PATH = "/";

    private static SpeedTestPreferences INSTANCE = null;
    private final SharedPreferences mSharedPrefs;

    public static synchronized SpeedTestPreferences getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new SpeedTestPreferences(context);
        }
        return INSTANCE;
    }

    private SpeedTestPreferences(Context context) {
        mContext = context.getApplicationContext();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public boolean isEnabled() {
        return mSharedPrefs.getBoolean(PREF_SPEED_TEST_ENABLED, false);
    }

    void setEnabled(boolean enabled) {
        Log.v(TAG, "setEnabled " + enabled);
        mSharedPrefs.edit().putBoolean(PREF_SPEED_TEST_ENABLED, enabled).commit();
    }

    public SpeedTestUploadConfig getUploadConfig() {
        String server = mSharedPrefs.getString(PREF_SPEED_TEST_UPLOAD_SERVER, "");
        int port = getIntPreference(PREF_SPEED_TEST_UPLOAD_PORT, PREF_SPEED_TEST_DEFAULT_UPLOAD_PORT);
        String user = mSharedPrefs.getString(PREF_SPEED_TEST_UPLOAD_USER, "");
        String password = mSharedPrefs.getString(PREF_SPEED_TEST_UPLOAD_PASSWORD, "");
        String path = mSharedPrefs.getString(PREF_SPEED_TEST_UPLOAD_PATH, PREF_SPEED_TEST_DEFAULT_UPLOAD_PATH);

        File file = FileUtil.getCacheFile(mContext, FILE);
        return new SpeedTestUploadConfig(server, port, user, password, path, file);
    }

    public SpeedTestDownloadConfig getDownloadConfig() {
        String url = mSharedPrefs.getString(PREF_SPEED_TEST_DOWNLOAD_URL, "");
        File file = FileUtil.getCacheFile(mContext, FILE);
        return new SpeedTestDownloadConfig(url, file);
    }

    public SpeedTestResult getLastDownloadResult() {
        return SpeedTestResult.read(mSharedPrefs, PREF_SPEED_TEST_LAST_DOWNLOAD_RESULT);
    }

    public void setLastDownloadResult(SpeedTestResult result) {
        Log.v(TAG, "setLastDownloadResult " + result);
        result.write(mSharedPrefs, PREF_SPEED_TEST_LAST_DOWNLOAD_RESULT);
    }

    private int getIntPreference(String key, String defaultValue) {
        String valueStr = mSharedPrefs.getString(key, defaultValue);
        int valueInt = Integer.valueOf(valueStr);
        return valueInt;
    }

}
