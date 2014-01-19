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
package org.jraf.android.networkmonitor.app.prefs;

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.jraf.android.networkmonitor.app.service.scheduler.AlarmManagerScheduler;
import org.jraf.android.networkmonitor.app.service.scheduler.ExecutorServiceScheduler;
import org.jraf.android.networkmonitor.provider.NetMonColumns;

/**
 * Convenience methods for getting/setting shared preferences.
 */
public class NetMonPreferences {

    public static final String PREF_UPDATE_INTERVAL = "PREF_UPDATE_INTERVAL";
    public static final String PREF_UPDATE_INTERVAL_DEFAULT = "10000";
    static final String PREF_WAKE_INTERVAL = "PREF_WAKE_INTERVAL";
    public static final String PREF_SERVICE_ENABLED = "PREF_SERVICE_ENABLED";
    public static final boolean PREF_SERVICE_ENABLED_DEFAULT = false;
    public static final String PREF_SCHEDULER = "PREF_SCHEDULER";

    private static final String PREF_WAKE_INTERVAL_DEFAULT = "0";
    private static final String PREF_KML_EXPORT_COLUMN = "PREF_KML_EXPORT_COLUMN";
    private static final String PREF_SCHEDULER_DEFAULT = ExecutorServiceScheduler.class.getSimpleName();
    private static final String PREF_SELECTED_COLUMNS = "PREF_SELECTED_COLUMNS";
    private static final String PREF_FILTER_RECORD_COUNT = "PREF_FILTER_RECORD_COUNT";
    private static final String PREF_FILTER_RECORD_COUNT_DEFAULT = "1000";

    private static NetMonPreferences INSTANCE = null;
    private final SharedPreferences mSharedPrefs;
    private final Context mContext;

    public static synchronized NetMonPreferences getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new NetMonPreferences(context);
        }
        return INSTANCE;
    }

    private NetMonPreferences(Context context) {
        mContext = context;
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * @return the interval between log entries, in millis
     */
    public int getUpdateInterval() {
        return getIntPreference(NetMonPreferences.PREF_UPDATE_INTERVAL, NetMonPreferences.PREF_UPDATE_INTERVAL_DEFAULT);
    }

    public int getWakeInterval() {
        return getIntPreference(NetMonPreferences.PREF_WAKE_INTERVAL, NetMonPreferences.PREF_WAKE_INTERVAL_DEFAULT);
    }

    public boolean isServiceEnabled() {
        return mSharedPrefs.getBoolean(NetMonPreferences.PREF_SERVICE_ENABLED, NetMonPreferences.PREF_SERVICE_ENABLED_DEFAULT);
    }

    public int getFilterRecordCount() {
        return getIntPreference(NetMonPreferences.PREF_FILTER_RECORD_COUNT, NetMonPreferences.PREF_FILTER_RECORD_COUNT_DEFAULT);
    }

    public void setFilterRecordCount(int filterRecordCount) {
        mSharedPrefs.edit().putString(PREF_FILTER_RECORD_COUNT, String.valueOf(filterRecordCount)).commit();
    }

    public void setServiceEnabled(boolean value) {
        Editor editor = mSharedPrefs.edit();
        editor.putBoolean(NetMonPreferences.PREF_SERVICE_ENABLED, value);
        editor.commit();
    }

    public String getKMLExportColumn() {
        return mSharedPrefs.getString(NetMonPreferences.PREF_KML_EXPORT_COLUMN, NetMonColumns.SOCKET_CONNECTION_TEST);
    }

    public void setKMLExportColumn(String value) {
        Editor editor = mSharedPrefs.edit();
        editor.putString(NetMonPreferences.PREF_KML_EXPORT_COLUMN, value);
        editor.commit();
    }

    private int getIntPreference(String key, String defaultValue) {
        String valueStr = mSharedPrefs.getString(key, defaultValue);
        int valueInt = Integer.valueOf(valueStr);
        return valueInt;
    }

    public Class<?> getSchedulerClass() {
        String schedulerPref = mSharedPrefs.getString(NetMonPreferences.PREF_SCHEDULER, NetMonPreferences.PREF_SCHEDULER_DEFAULT);
        if (schedulerPref.equals(AlarmManagerScheduler.class.getSimpleName())) return AlarmManagerScheduler.class;
        else
            return ExecutorServiceScheduler.class;

    }

    public List<String> getSelectedColumns() {
        String selectedColumnsString = mSharedPrefs.getString(NetMonPreferences.PREF_SELECTED_COLUMNS, null);
        final String[] selectedColumns;
        if (TextUtils.isEmpty(selectedColumnsString)) selectedColumns = NetMonColumns.getColumnNames(mContext);
        else
            selectedColumns = selectedColumnsString.split(",");
        return Arrays.asList(selectedColumns);
    }

    public void setSelectedColumns(List<String> selectedColumns) {
        String selectedColumnsString = TextUtils.join(",", selectedColumns);
        mSharedPrefs.edit().putString(NetMonPreferences.PREF_SELECTED_COLUMNS, selectedColumnsString).commit();
    }
}
