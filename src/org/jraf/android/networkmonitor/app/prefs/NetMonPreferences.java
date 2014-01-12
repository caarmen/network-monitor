/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014 Benoit 'BoD' Lubek (BoD@JRAF.org) //TODO <- replace with *your* name/email
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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.provider.NetMonColumns;

public class NetMonPreferences {

    private static NetMonPreferences INSTANCE = null;
    private final SharedPreferences mSharedPrefs;

    public static synchronized NetMonPreferences getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new NetMonPreferences(context);
        }
        return INSTANCE;
    }

    private NetMonPreferences(Context context) {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public int getUpdateInterval() {
        return getIntPreference(Constants.PREF_UPDATE_INTERVAL, Constants.PREF_UPDATE_INTERVAL_DEFAULT);
    }

    public int getWakeInterval() {
        return getIntPreference(Constants.PREF_WAKE_INTERVAL, Constants.PREF_WAKE_INTERVAL_DEFAULT);
    }

    public boolean isServiceEnabled() {
        return mSharedPrefs.getBoolean(Constants.PREF_SERVICE_ENABLED, Constants.PREF_SERVICE_ENABLED_DEFAULT);
    }

    public void setServiceEnabled(boolean value) {
        Editor editor = mSharedPrefs.edit();
        editor.putBoolean(Constants.PREF_SERVICE_ENABLED, value);
        editor.commit();
    }

    public String getKMLExportColumn() {
        return mSharedPrefs.getString(Constants.PREF_KML_EXPORT_COLUMN, NetMonColumns.SOCKET_CONNECTION_TEST);
    }

    public void setKMLExportColumn(String value) {
        Editor editor = mSharedPrefs.edit();
        editor.putString(Constants.PREF_KML_EXPORT_COLUMN, value);
        editor.commit();
    }

    private int getIntPreference(String key, String defaultValue) {
        String valueStr = mSharedPrefs.getString(key, defaultValue);
        int valueInt = Integer.valueOf(valueStr);
        return valueInt;
    }

}
