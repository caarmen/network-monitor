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

import org.jraf.android.networkmonitor.app.prefs.SortPreferences.SortOrder;
import org.jraf.android.networkmonitor.app.service.scheduler.AlarmManagerScheduler;
import org.jraf.android.networkmonitor.app.service.scheduler.ExecutorServiceScheduler;
import org.jraf.android.networkmonitor.app.service.scheduler.Scheduler;
import org.jraf.android.networkmonitor.provider.NetMonColumns;

/**
 * Convenience methods for getting/setting shared preferences.
 */
public class NetMonPreferences {

    public enum CellIdFormat {
        DECIMAL, HEX, DECIMAL_HEX
    };

    public static final String PREF_UPDATE_INTERVAL = "PREF_UPDATE_INTERVAL";
    public static final String PREF_UPDATE_INTERVAL_DEFAULT = "10000";
    public static final String PREF_SERVICE_ENABLED = "PREF_SERVICE_ENABLED";
    public static final boolean PREF_SERVICE_ENABLED_DEFAULT = false;
    public static final String PREF_SCHEDULER = "PREF_SCHEDULER";
    public static final String PREF_SORT_ORDER = "PREF_SORT_ORDER";
    public static final String PREF_SORT_COLUMN_NAME = "PREF_SORT_COLUMN_NAME";

    static final String PREF_WAKE_INTERVAL = "PREF_WAKE_INTERVAL";
    static final String PREF_KML_EXPORT_COLUMN = "PREF_KML_EXPORT_COLUMN";
    static final String PREF_FILTER_RECORD_COUNT = "PREF_FILTER_RECORD_COUNT";
    static final String PREF_FILTER_RECORD_COUNT_DEFAULT = "250";
    static final String PREF_CELL_ID_FORMAT = "PREF_CELL_ID_FORMAT";
    static final String PREF_CELL_ID_FORMAT_DEFAULT = "decimal";

    private static final String PREF_WAKE_INTERVAL_DEFAULT = "0";
    private static final String PREF_SCHEDULER_DEFAULT = ExecutorServiceScheduler.class.getSimpleName();
    private static final String PREF_SELECTED_COLUMNS = "PREF_SELECTED_COLUMNS";
    private static final String PREF_SORT_COLUMN_NAME_DEFAULT = NetMonColumns.TIMESTAMP;
    private static final String PREF_SORT_ORDER_DEFAULT = SortOrder.DESC.name();

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

    /**
     * @return the interval, in milliseconds, between forced waking up the device (by turning the screen on). If not positive, we will never force wake up the
     *         device.
     */
    public int getWakeInterval() {
        return getIntPreference(NetMonPreferences.PREF_WAKE_INTERVAL, NetMonPreferences.PREF_WAKE_INTERVAL_DEFAULT);
    }

    /**
     * @return true if we are currently collecting and logging data.
     */
    public boolean isServiceEnabled() {
        return mSharedPrefs.getBoolean(NetMonPreferences.PREF_SERVICE_ENABLED, NetMonPreferences.PREF_SERVICE_ENABLED_DEFAULT);
    }

    /**
     * @param value true if we should collect and log data, false otherwise.
     */
    public void setServiceEnabled(boolean value) {
        Editor editor = mSharedPrefs.edit();
        editor.putBoolean(NetMonPreferences.PREF_SERVICE_ENABLED, value);
        editor.commit();
    }

    /**
     * @return the number of rows we should display in the log view. This is only for display: we always export all rows.
     */
    public int getFilterRecordCount() {
        return getIntPreference(NetMonPreferences.PREF_FILTER_RECORD_COUNT, NetMonPreferences.PREF_FILTER_RECORD_COUNT_DEFAULT);
    }

    /**
     * @param filterRecordCount the number of rows we should display in the log view. This is only for display: we always export all rows.
     */
    public void setFilterRecordCount(int filterRecordCount) {
        mSharedPrefs.edit().putString(PREF_FILTER_RECORD_COUNT, String.valueOf(filterRecordCount)).commit();
    }

    /**
     * @return the format in which numeric cell id fields should be displayed and exported.
     */
    public CellIdFormat getCellIdFormat() {
        String cellIdFormat = mSharedPrefs.getString(NetMonPreferences.PREF_CELL_ID_FORMAT, NetMonPreferences.PREF_CELL_ID_FORMAT_DEFAULT);
        if ("decimal".equals(cellIdFormat)) return CellIdFormat.DECIMAL;
        if ("hex".equals(cellIdFormat)) return CellIdFormat.HEX;
        return CellIdFormat.DECIMAL_HEX;
    }


    /**
     * @return the db column name which will be used for the placemark names in the KML export.
     */
    public String getKMLExportColumn() {
        return mSharedPrefs.getString(NetMonPreferences.PREF_KML_EXPORT_COLUMN, NetMonColumns.SOCKET_CONNECTION_TEST);
    }

    /**
     * @param value db column name which will be used for the placemark names in the KML export.
     */
    public void setKMLExportColumn(String value) {
        Editor editor = mSharedPrefs.edit();
        editor.putString(NetMonPreferences.PREF_KML_EXPORT_COLUMN, value);
        editor.commit();
    }

    /**
     * @return the implementation of the {@link Scheduler} interface which schedules each logging of data.
     */
    public Class<?> getSchedulerClass() {
        String schedulerPref = mSharedPrefs.getString(NetMonPreferences.PREF_SCHEDULER, NetMonPreferences.PREF_SCHEDULER_DEFAULT);
        if (schedulerPref.equals(AlarmManagerScheduler.class.getSimpleName())) return AlarmManagerScheduler.class;
        else
            return ExecutorServiceScheduler.class;
    }

    /**
     * @return the list of columns which will appear in the log view. This is only for display. All columns will be exported.
     */
    public List<String> getSelectedColumns() {
        String selectedColumnsString = mSharedPrefs.getString(NetMonPreferences.PREF_SELECTED_COLUMNS, null);
        final String[] selectedColumns;
        if (TextUtils.isEmpty(selectedColumnsString)) selectedColumns = NetMonColumns.getColumnNames(mContext);
        else
            selectedColumns = selectedColumnsString.split(",");
        return Arrays.asList(selectedColumns);
    }

    /**
     * @return the list of columns to appear in the log view. This is only for display. All columns will be exported.
     */
    public void setSelectedColumns(List<String> selectedColumns) {
        String selectedColumnsString = TextUtils.join(",", selectedColumns);
        mSharedPrefs.edit().putString(NetMonPreferences.PREF_SELECTED_COLUMNS, selectedColumnsString).commit();
    }

    /**
     * @return the settings for how rows in the log view or table export formats are sorted.
     */
    public SortPreferences getSortPreferences() {
        String sortColumnName = mSharedPrefs.getString(PREF_SORT_COLUMN_NAME, PREF_SORT_COLUMN_NAME_DEFAULT);
        SortOrder sortOrder = SortOrder.valueOf(mSharedPrefs.getString(PREF_SORT_ORDER, PREF_SORT_ORDER_DEFAULT));
        return new SortPreferences(sortColumnName, sortOrder);
    }

    /**
     * @return the settings for how rows in the log view or table export formats should be sorted.
     */
    public void setSortPreferences(SortPreferences sortPreferences) {
        Editor editor = mSharedPrefs.edit();
        editor.putString(PREF_SORT_COLUMN_NAME, sortPreferences.sortColumnName);
        editor.putString(PREF_SORT_ORDER, sortPreferences.sortOrder.name());
        editor.commit();
    }

    private int getIntPreference(String key, String defaultValue) {
        String valueStr = mSharedPrefs.getString(key, defaultValue);
        int valueInt = Integer.valueOf(valueStr);
        return valueInt;
    }

}
