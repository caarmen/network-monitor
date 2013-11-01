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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.jraf.android.networkmonitor.Constants;

public class NetMonDatabase extends SQLiteOpenHelper {
    private static final String TAG = Constants.TAG + NetMonDatabase.class.getSimpleName();

    public static final String DATABASE_NAME = "networkmonitor.db";
    private static final int DATABASE_VERSION = 6;

    // @formatter:off
    private static final String SQL_CREATE_TABLE_NETWORKMONITOR = "CREATE TABLE IF NOT EXISTS "
            + NetMonColumns.TABLE_NAME + " ( "
            + NetMonColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + NetMonColumns.TIMESTAMP + " INTEGER, "
            + NetMonColumns.SOCKET_CONNECTION_TEST + " TEXT, "
            + NetMonColumns.HTTP_CONNECTION_TEST + " TEXT, "
            + NetMonColumns.NETWORK_TYPE + " TEXT, "
            + NetMonColumns.MOBILE_DATA_NETWORK_TYPE + " TEXT, "
            + NetMonColumns.SIM_STATE + " TEXT, "
            + NetMonColumns.DETAILED_STATE + " TEXT, "
            + NetMonColumns.IS_CONNECTED + " INTEGER, "
            + NetMonColumns.IS_ROAMING + " INTEGER, "
            + NetMonColumns.IS_AVAILABLE + " INTEGER, "
            + NetMonColumns.IS_FAILOVER + " INTEGER, "
            + NetMonColumns.DATA_ACTIVITY + " TEXT, "
            + NetMonColumns.DATA_STATE + " TEXT, "
            + NetMonColumns.REASON + " TEXT, "
            + NetMonColumns.EXTRA_INFO + " TEXT, "
            + NetMonColumns.WIFI_SSID + " TEXT, "
            + NetMonColumns.WIFI_SIGNAL_STRENGTH + " INTEGER, "
            + NetMonColumns.WIFI_RSSI + " INTEGER, "
            + NetMonColumns.SIM_OPERATOR + " TEXT, "
            + NetMonColumns.NETWORK_OPERATOR + " TEXT, "
            + NetMonColumns.IS_NETWORK_METERED + " INTEGER, "
            + NetMonColumns.DEVICE_LATITUDE + " REAL, "
            + NetMonColumns.DEVICE_LONGITUDE + " REAL, "
            + NetMonColumns.CELL_SIGNAL_STRENGTH + " INTEGER, "
            + NetMonColumns.CELL_SIGNAL_STRENGTH_DBM + " INTEGER, "
            + NetMonColumns.CDMA_CELL_BASE_STATION_ID + " INTEGER, "
            + NetMonColumns.CDMA_CELL_LATITUDE + " INTEGER, "
            + NetMonColumns.CDMA_CELL_LONGITUDE + " INTEGER, "
            + NetMonColumns.CDMA_CELL_NETWORK_ID + " INTEGER, "
            + NetMonColumns.CDMA_CELL_SYSTEM_ID + " INTEGER, "
            + NetMonColumns.GSM_FULL_CELL_ID + " INTEGER, "
            + NetMonColumns.GSM_SHORT_CELL_ID + " INTEGER, "
            + NetMonColumns.GSM_CELL_LAC + " INTEGER, "
            + NetMonColumns.GSM_CELL_PSC + " INTEGER "
            + " );";
    // @formatter:on

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V2_SIM_OPERATOR = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.SIM_OPERATOR + " TEXT";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V2_NETWORK_OPERATOR = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.NETWORK_OPERATOR + " TEXT";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V3_HTTP_CONNECTION_TEST = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.HTTP_CONNECTION_TEST + " TEXT";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V4_WIFI_SSID = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.WIFI_SSID + " TEXT";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V5_WIFI_SIGNAL_STRENGTH = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.WIFI_SIGNAL_STRENGTH + " INTEGER";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V6_WIFI_RSSI = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.WIFI_RSSI + " INTEGER";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V6_CELL_SIGNAL_STRENGTH_DBM = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.CELL_SIGNAL_STRENGTH_DBM + " INTEGER";

    NetMonDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate");
        db.execSQL(SQL_CREATE_TABLE_NETWORKMONITOR);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        if (oldVersion < 2) {
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V2_SIM_OPERATOR);
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V2_NETWORK_OPERATOR);
        }

        if (oldVersion < 3) db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V3_HTTP_CONNECTION_TEST);

        if (oldVersion < 4) db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V4_WIFI_SSID);

        if (oldVersion < 5) db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V5_WIFI_SIGNAL_STRENGTH);

        if (oldVersion < 6) {
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V6_WIFI_RSSI);
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V6_CELL_SIGNAL_STRENGTH_DBM);
        }
    }
}
