/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013 Carmen Alvarez (c@rmen.ca)
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
package org.jraf.android.networkmonitor.provider;

import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;

import org.jraf.android.networkmonitor.R;

public class NetMonColumns implements BaseColumns {
    public static final String TABLE_NAME = "networkmonitor";
    public static final String UNIQUE_VALUES = "unique_values";
    public static final Uri CONTENT_URI = Uri.parse(NetMonProvider.CONTENT_URI_BASE + "/" + TABLE_NAME);
    public static final Uri UNIQUE_VALUES_URI = Uri.parse(NetMonProvider.CONTENT_URI_BASE + "/" + UNIQUE_VALUES);

    static final String _ID = BaseColumns._ID;

    public static final String TIMESTAMP = "timestamp";
    public static final String NETWORK_TYPE = "network_type";
    public static final String MOBILE_DATA_NETWORK_TYPE = "mobile_data_network_type";
    // google_connection_test now corresponds to a basic socket connection test.
    // The column name has been kept for backwards compatibility.
    public static final String SOCKET_CONNECTION_TEST = "google_connection_test";
    public static final String HTTP_CONNECTION_TEST = "http_connection_test";
    public static final String SIM_STATE = "sim_state";
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
    public static final String CELL_SIGNAL_STRENGTH = "cell_signal_strength";
    public static final String CELL_SIGNAL_STRENGTH_DBM = "cell_signal_strength_dbm";
    public static final String CELL_ASU_LEVEL = "cell_asu_level";
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
    public static final String NETWORK_INTERFACE = "network_interface";
    public static final String IPV4_ADDRESS = "ipv4_address";
    public static final String IPV6_ADDRESS = "ipv6_address";

    public static final String UNIQUE_VALUES_VALUE = "unique_values_value";
    public static final String UNIQUE_VALUES_COUNT = "unique_values_count";

    /**
     * @return the list of column names in the main table. This returns the technical names of the columns, as they appear in the DB.
     */
    public static String[] getColumnNames(Context context) {
        return context.getResources().getStringArray(R.array.db_columns);
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
     * @return the localized display name for a particular DB column name.
     */
    public static String getColumnLabel(Context context, String columnName) {
        int columnLabelId = context.getResources().getIdentifier(columnName, "string", R.class.getPackage().getName());
        String columnLabel = context.getString(columnLabelId);
        return columnLabel;
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
        return context.getResources().getStringArray(R.array.filterable_columns);
    }

    public static boolean isColumnFilterable(Context context, String columnName) {
        String[] filterableColumns = getFilterableColumns(context);
        for (String filterableColumn : filterableColumns)
            if (columnName.equals(filterableColumn)) return true;
        return false;
    }

    static final String DEFAULT_ORDER = _ID;
}