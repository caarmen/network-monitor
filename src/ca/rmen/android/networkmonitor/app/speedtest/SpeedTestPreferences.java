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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

/**
 * Convenience methods for getting/setting shared preferences related to the speed test.
 */
public class SpeedTestPreferences {


    public static class SpeedTestUploadConfig {
        final String server;
        final int port;
        final String user;
        final String password;

        public SpeedTestUploadConfig(String server, int port, String user, String password) {
            this.server = server;
            this.port = port;
            this.user = user;
            this.password = password;
        }

        /**
         * @return true if we have enough info to attempt to upload a file.
         */
        public boolean isValid() {
            return !TextUtils.isEmpty(server) && port > 0 && !TextUtils.isEmpty(user) && !TextUtils.isEmpty(password);
        }

        @Override
        public String toString() {
            return SpeedTestUploadConfig.class.getSimpleName() + " [server=" + server + ", port=" + port + ", user=" + user + ", password=" + password + "]";
        }

    }

    public static class SpeedTestDownloadConfig {
        final String url;

        public SpeedTestDownloadConfig(String url) {
            this.url = url;
        }

        /**
         * @return true if we have enough info to attempt to download a file.
         */
        public boolean isValid() {
            return !TextUtils.isEmpty(url);
        }

        @Override
        public String toString() {
            return SpeedTestDownloadConfig.class.getSimpleName() + "[url=" + url + "]";
        }
    }

    static final String PREF_SPEED_TEST_ENABLED = "PREF_SPEED_TEST_ENABLED";
    static final String PREF_SPEED_TEST_DOWNLOAD_URL = "PREF_SPEED_TEST_DOWNLOAD_URL";
    static final String PREF_SPEED_TEST_UPLOAD_SERVER = "PREF_SPEED_TEST_UPLOAD_SERVER";
    static final String PREF_SPEED_TEST_UPLOAD_PORT = "PREF_SPEED_TEST_UPLOAD_PORT";
    static final String PREF_SPEED_TEST_UPLOAD_USER = "PREF_SPEED_TEST_UPLOAD_USER";
    static final String PREF_SPEED_TEST_UPLOAD_PASSWORD = "PREF_SPEED_TEST_UPLOAD_PASSWORD";

    private static final String PREF_SPEED_TEST_DEFAULT_UPLOAD_PORT = "21";

    private static SpeedTestPreferences INSTANCE = null;
    private final SharedPreferences mSharedPrefs;

    public static synchronized SpeedTestPreferences getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new SpeedTestPreferences(context);
        }
        return INSTANCE;
    }

    private SpeedTestPreferences(Context context) {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isEnabled() {
        return mSharedPrefs.getBoolean(PREF_SPEED_TEST_ENABLED, false);
    }

    void setEnabled(boolean enabled) {
        mSharedPrefs.edit().putBoolean(PREF_SPEED_TEST_ENABLED, enabled).commit();
    }

    public SpeedTestUploadConfig getUploadConfig() {
        String server = mSharedPrefs.getString(PREF_SPEED_TEST_UPLOAD_SERVER, "");
        int port = getIntPreference(PREF_SPEED_TEST_UPLOAD_PORT, PREF_SPEED_TEST_DEFAULT_UPLOAD_PORT);
        String user = mSharedPrefs.getString(PREF_SPEED_TEST_UPLOAD_USER, "");
        String password = mSharedPrefs.getString(PREF_SPEED_TEST_UPLOAD_PASSWORD, "");
        return new SpeedTestUploadConfig(server, port, user, password);
    }

    public SpeedTestDownloadConfig getDownloadConfig() {
        String url = mSharedPrefs.getString(PREF_SPEED_TEST_DOWNLOAD_URL, "");
        return new SpeedTestDownloadConfig(url);
    }

    private int getIntPreference(String key, String defaultValue) {
        String valueStr = mSharedPrefs.getString(key, defaultValue);
        int valueInt = Integer.valueOf(valueStr);
        return valueInt;
    }

}
