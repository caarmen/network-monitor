/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013-2019 Carmen Alvarez (c@rmen.ca)
 * Copyright (C) 2013 Benoit 'BoD' Lubek (BoD@JRAF.org)
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
package ca.rmen.android.networkmonitor.provider;

import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import ca.rmen.android.networkmonitor.R;

public class NetMonColumns implements BaseColumns {
    public static final String TABLE_NAME = "networkmonitor";
    public static final Uri CONTENT_URI = Uri.parse(NetMonProvider.CONTENT_URI_BASE + "/" + TABLE_NAME);

    static final String _ID = BaseColumns._ID;

    public static final String TIMESTAMP = "timestamp";
    public static final String NETWORK_TYPE = "network_type";
    public static final String MOBILE_DATA_NETWORK_TYPE = "mobile_data_network_type";
    // google_connection_test now corresponds to a basic socket connection test.
    // The column name has been kept for backwards compatibility.
    public static final String SOCKET_CONNECTION_TEST = "google_connection_test";
    public static final String HTTP_CONNECTION_TEST = "http_connection_test";
    public static final String SIM_STATE = "sim_state";
    public static final String SERVICE_STATE = "service_state";
    public static final String DETAILED_STATE = "detailed_state";
    public static final String IS_CONNECTED = "is_connected";
    public static final String IS_ROAMING = "is_roaming";
    public static final String IS_AVAILABLE = "is_available";
    public static final String IS_FAILOVER = "is_failover";
    public static final String DATA_ACTIVITY = "data_activity";
    public static final String DATA_STATE = "data_state";
    public static final String REASON = "reason";
    public static final String EXTRA_INFO = "extra_info";
    public static final String WIFI_SSID = "wifi_ssid";
    public static final String WIFI_BSSID = "wifi_bssid";
    public static final String WIFI_FREQUENCY = "wifi_frequency";
    public static final String WIFI_CHANNEL = "wifi_channel";
    public static final String WIFI_SIGNAL_STRENGTH = "wifi_signal_strength";
    public static final String WIFI_RSSI = "wifi_rssi";
    public static final String SIM_OPERATOR = "sim_operator";
    public static final String SIM_MCC = "sim_mcc";
    public static final String SIM_MNC = "sim_mnc";
    public static final String NETWORK_OPERATOR = "network_operator";
    public static final String NETWORK_MCC = "network_mcc";
    public static final String NETWORK_MNC = "network_mnc";
    public static final String IS_NETWORK_METERED = "is_network_metered";
    public static final String DEVICE_LATITUDE = "device_latitude";
    public static final String DEVICE_LONGITUDE = "device_longitude";
    public static final String DEVICE_POSITION_ACCURACY = "device_position_accuracy";
    public static final String DEVICE_SPEED = "device_speed";
    public static final String CELL_SIGNAL_STRENGTH = "cell_signal_strength";
    public static final String CELL_SIGNAL_STRENGTH_DBM = "cell_signal_strength_dbm";
    public static final String CELL_ASU_LEVEL = "cell_asu_level";
    public static final String GSM_BER = "gsm_ber";
    public static final String LTE_RSRQ = "lte_rsrq";
    //public static final String EVDO_ECIO = "evdo_ecio";
    public static final String CDMA_CELL_BASE_STATION_ID = "cdma_cell_base_station_id";
    public static final String CDMA_CELL_LATITUDE = "cdma_cell_latitude";
    public static final String CDMA_CELL_LONGITUDE = "cdma_cell_longitude";
    public static final String CDMA_CELL_NETWORK_ID = "cdma_cell_network_id";
    public static final String CDMA_CELL_SYSTEM_ID = "cdma_cell_system_id";
    public static final String GSM_FULL_CELL_ID = "gsm_full_cell_id";
    public static final String GSM_RNC = "gsm_rnc";
    public static final String GSM_SHORT_CELL_ID = "gsm_short_cell_id";
    public static final String GSM_CELL_LAC = "gsm_cell_lac";
    public static final String GSM_CELL_PSC = "gsm_cell_psc";
    public static final String LTE_CELL_CI = "lte_cell_ci";
    public static final String LTE_CELL_EARFCN = "lte_cell_earfcn";
    public static final String LTE_CELL_PCI = "lte_cell_pci";
    public static final String LTE_CELL_TAC = "lte_cell_tac";
    public static final String NETWORK_INTERFACE = "network_interface";
    public static final String IPV4_ADDRESS = "ipv4_address";
    public static final String IPV6_ADDRESS = "ipv6_address";
    public static final String MOST_CONSUMING_APP_NAME = "most_consuming_app_name";
    public static final String MOST_CONSUMING_APP_BYTES = "most_consuming_app_bytes";
    public static final String BATTERY_LEVEL = "battery_level";
    public static final String DOWNLOAD_SPEED = "download_speed";
    public static final String UPLOAD_SPEED = "upload_speed";


    /**
     * @return the list of column names in the main table. This returns the technical names of the columns, as they appear in the DB.
     */
    public static String[] getColumnNames(Context context) {
        String [] allColumnNames = context.getResources().getStringArray(R.array.db_columns);
        List<String> newerApiColumnNames = getNewerApiColumns(context);
        List<String> result = new ArrayList<>();
        for (String columnName : allColumnNames) {
            if (!newerApiColumnNames.contains(columnName)) {
                result.add(columnName);
            }
        }
        return result.toArray(new String[0]);
    }

    /**
     * @return The localized display names of all the columns in the DB.
     */
    public static String[] getColumnLabels(Context context) {
        String[] columnNames = getColumnNames(context);
        String[] columnLabels = new String[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) {
            columnLabels[i] = getColumnLabel(context, columnNames[i]);
        }
        return columnLabels;
    }

    /**
     * @return the localized display names of all the given DB column names.
     */
    public static CharSequence[] getColumnLabels(Context context, CharSequence[] columnNames) {
        CharSequence[] result = new String[columnNames.length];
        for (int i=0; i < columnNames.length; i++) {
            result[i] = getColumnLabel(context, columnNames[i].toString());
        }
        return result;
    }

    /**
     * @return the list of columns which should be visible in the log view by default
     *         (until the user explicitly changes the list of visible columns).
     */
    public static String[] getDefaultVisibleColumnNames(Context context) {
        String[] columnNames = context.getResources().getStringArray(R.array.db_columns);
        String[] hiddenColumnNames = context.getResources().getStringArray(R.array.db_columns_hide);
        List<String> hiddenColumnNamesList = Arrays.asList(hiddenColumnNames);
        List<String> newerApiColumnNames = getNewerApiColumns(context);
        List<String> result = new ArrayList<>();
        for (String columnName : columnNames) {
            if (!hiddenColumnNamesList.contains(columnName)
                    && !newerApiColumnNames.contains(columnName)) {
                result.add(columnName);
            }
        }
        return result.toArray(new String[0]);
    }

    /**
     * @return the localized display name for a particular DB column name.
     */
    @NonNull
    public static String getColumnLabel(Context context, String columnName) {
        int columnLabelId = context.getResources().getIdentifier(columnName, "string", context.getPackageName());
        return context.getString(columnLabelId);
    }

    /**
     * @return the DB column name which has this label
     */
    public static String getColumnName(Context context, String columnLabel) {
        String[] columnNames = getColumnNames(context);
        for (String columnName : columnNames) {
            if (columnLabel.equals(getColumnLabel(context, columnName))) return columnName;
        }
        return null;
    }

    public static String[] getFilterableColumns(Context context) {
        String[] filterableColumns = context.getResources().getStringArray(R.array.filterable_columns);
        List<String> newerApiColumns = getNewerApiColumns(context);
        List<String> result = new ArrayList<>();
        for (String column : filterableColumns) {
            if (!newerApiColumns.contains(column)) {
                result.add(column);
            }
        }
        return result.toArray(new String[0]);
    }

    public static boolean isColumnFilterable(Context context, String columnName) {
        String[] filterableColumns = getFilterableColumns(context);
        for (String filterableColumn : filterableColumns)
            if (columnName.equals(filterableColumn)) return true;
        return false;
    }

    private static List<String> getNewerApiColumns(Context context) {
        return Arrays.asList(context.getResources().getStringArray(R.array.newer_api_db_columns));
    }

    static final String DEFAULT_ORDER = _ID;
}