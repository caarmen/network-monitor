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

import android.net.Uri;
import android.provider.BaseColumns;

public class NetMonColumns implements BaseColumns {
    public static final String TABLE_NAME = "networkmonitor";
    public static final Uri CONTENT_URI = Uri.parse(NetMonProvider.CONTENT_URI_BASE + "/" + TABLE_NAME);
    static final String GSM_SUMMARY = "/gsm_summary";
    static final String CDMA_SUMMARY = "/cdma_summary";
    static final String SUMMARY = "/summary";
    public static final Uri CONTENT_URI_GSM_SUMMARY = Uri.parse(NetMonProvider.CONTENT_URI_BASE + "/" + TABLE_NAME + GSM_SUMMARY);
    public static final Uri CONTENT_URI_CDMA_SUMMARY = Uri.parse(NetMonProvider.CONTENT_URI_BASE + "/" + TABLE_NAME + CDMA_SUMMARY);
    public static final Uri CONTENT_URI_SUMMARY = Uri.parse(NetMonProvider.CONTENT_URI_BASE + "/" + TABLE_NAME + SUMMARY);


    public static final String _ID = BaseColumns._ID;

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
    public static final String WIFI_SIGNAL_STRENGTH = "wifi_signal_strength";
    public static final String SIM_OPERATOR = "sim_operator";
    public static final String NETWORK_OPERATOR = "network_operator";
    public static final String IS_NETWORK_METERED = "is_network_metered";
    public static final String DEVICE_LATITUDE = "device_latitude";
    public static final String DEVICE_LONGITUDE = "device_longitude";
    public static final String CELL_SIGNAL_STRENGTH = "cell_signal_strength";
    public static final String CDMA_CELL_BASE_STATION_ID = "cdma_cell_base_station_id";
    public static final String CDMA_CELL_LATITUDE = "cdma_cell_latitude";
    public static final String CDMA_CELL_LONGITUDE = "cdma_cell_longitude";
    public static final String CDMA_CELL_NETWORK_ID = "cdma_cell_network_id";
    public static final String CDMA_CELL_SYSTEM_ID = "cdma_cell_system_id";
    public static final String GSM_FULL_CELL_ID = "gsm_full_cell_id";
    public static final String GSM_SHORT_CELL_ID = "gsm_short_cell_id";
    public static final String GSM_CELL_LAC = "gsm_cell_lac";
    public static final String GSM_CELL_PSC = "gsm_cell_psc";

    // Columns for the summary URIs.
    public static final String PASS_COUNT = "pass_count";
    public static final String FAIL_COUNT = "fail_count";
    public static final String SLOW_COUNT = "slow_count";

    public static final String DEFAULT_ORDER = _ID;
}