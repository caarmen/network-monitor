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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.Constants.ConnectionType;
import org.jraf.android.networkmonitor.util.TelephonyUtil;

public class NetMonDatabase extends SQLiteOpenHelper {
    private static final String TAG = Constants.TAG + NetMonDatabase.class.getSimpleName();

    public static final String DATABASE_NAME = "networkmonitor.db";
    private static final int DATABASE_VERSION = 8;

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
            + NetMonColumns.WIFI_BSSID + " TEXT, "
            + NetMonColumns.WIFI_SIGNAL_STRENGTH + " INTEGER, "
            + NetMonColumns.WIFI_RSSI + " INTEGER, "
            + NetMonColumns.SIM_OPERATOR + " TEXT, "
            + NetMonColumns.SIM_MCC + " TEXT, "
            + NetMonColumns.SIM_MNC + " TEXT, "
            + NetMonColumns.NETWORK_OPERATOR + " TEXT, "
            + NetMonColumns.NETWORK_MCC + " TEXT, "
            + NetMonColumns.NETWORK_MNC + " TEXT, "
            + NetMonColumns.IS_NETWORK_METERED + " INTEGER, "
            + NetMonColumns.DEVICE_LATITUDE + " REAL, "
            + NetMonColumns.DEVICE_LONGITUDE + " REAL, "
            + NetMonColumns.DEVICE_POSITION_ACCURACY + " REAL, "
            + NetMonColumns.CELL_SIGNAL_STRENGTH + " INTEGER, "
            + NetMonColumns.CELL_SIGNAL_STRENGTH_DBM + " INTEGER, "
            + NetMonColumns.CELL_ASU_LEVEL + " INTEGER, "
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

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V6_WIFI_BSSID = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.WIFI_BSSID + " TEXT";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V6_CELL_SIGNAL_STRENGTH_DBM = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.CELL_SIGNAL_STRENGTH_DBM + " INTEGER";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V7_CELL_ASU_LEVEL = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.CELL_ASU_LEVEL + " INTEGER";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V8_DEVICE_POSITION_ACCURACY = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.DEVICE_POSITION_ACCURACY + " REAL";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V8_SIM_MCC = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN " + NetMonColumns.SIM_MCC
            + " TEXT";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V8_SIM_MNC = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN " + NetMonColumns.SIM_MNC
            + " TEXT";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V8_NETWORK_MCC = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.NETWORK_MCC + " TEXT";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V8_NETWORK_MNC = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.NETWORK_MNC + " TEXT";

    private static final String SQL_CREATE_VIEW_CONNECTION_TEST_STATS = "CREATE VIEW " + ConnectionTestStatsColumns.VIEW_NAME + " AS "
            + buildConnectionTestQuery();

    NetMonDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate");
        db.execSQL(SQL_CREATE_TABLE_NETWORKMONITOR);
        db.execSQL(SQL_CREATE_VIEW_CONNECTION_TEST_STATS);
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
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V6_WIFI_BSSID);
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V6_CELL_SIGNAL_STRENGTH_DBM);
            db.execSQL(SQL_CREATE_VIEW_CONNECTION_TEST_STATS);
        }

        if (oldVersion < 7) db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V7_CELL_ASU_LEVEL);

        if (oldVersion < 8) {
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V8_DEVICE_POSITION_ACCURACY);
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V8_SIM_MCC);
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V8_SIM_MNC);
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V8_NETWORK_MCC);
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V8_NETWORK_MNC);
            updateMccMnc(db);
        }
    }

    /**
     * @return a query to retrieve the stats of the connection test results.
     */
    private static final String buildConnectionTestQuery() {
        String gsmQuery = buildConnectionTestSubQuery(ConnectionType.GSM, NetMonColumns.GSM_CELL_LAC, NetMonColumns.GSM_SHORT_CELL_ID,
                NetMonColumns.GSM_FULL_CELL_ID, NetMonColumns.EXTRA_INFO, NetMonColumns.DATA_STATE + "='" + Constants.DATA_STATE_CONNECTED + "'");
        String cdmaQuery = buildConnectionTestSubQuery(ConnectionType.CDMA, NetMonColumns.CDMA_CELL_BASE_STATION_ID, NetMonColumns.CDMA_CELL_NETWORK_ID,
                NetMonColumns.CDMA_CELL_SYSTEM_ID, NetMonColumns.EXTRA_INFO, NetMonColumns.DATA_STATE + "='" + Constants.DATA_STATE_CONNECTED + "'");
        String wifiQuery = buildConnectionTestSubQuery(ConnectionType.WIFI, NetMonColumns.WIFI_BSSID, "NULL", "NULL", NetMonColumns.WIFI_SSID,
                NetMonColumns.NETWORK_TYPE + "='" + ConnectionType.WIFI + "'");
        return gsmQuery + " UNION " + cdmaQuery + " UNION " + wifiQuery;
    }

    /**
     * @return a query to retrieve the stats of the connection test results, for a particular connection type (gsm, cdma, or wifi).
     */
    private static final String buildConnectionTestSubQuery(ConnectionType type, String id1Column, String id2Column, String id3Column, String labelColumn,
            String selection) {
        // @formatter:off
        return "SELECT '" + type + "' as " + ConnectionTestStatsColumns.TYPE + ","
            + id1Column + " as " + ConnectionTestStatsColumns.ID1 + ", "
            + id2Column + " as " + ConnectionTestStatsColumns.ID2 + ", "
            + id3Column + " as " + ConnectionTestStatsColumns.ID3 + ", "
            + labelColumn + " as " + ConnectionTestStatsColumns.LABEL + ", "
            + NetMonColumns.SOCKET_CONNECTION_TEST + " as " + ConnectionTestStatsColumns.TEST_RESULT + ", "
            + "COUNT(" + NetMonColumns.SOCKET_CONNECTION_TEST +") as " + ConnectionTestStatsColumns.TEST_COUNT 
            + " FROM " + NetMonColumns.TABLE_NAME
            + " WHERE (" + selection + ") AND "
            + "("
            + ConnectionTestStatsColumns.ID1 + " NOT NULL OR "
            + ConnectionTestStatsColumns.ID2 + " NOT NULL OR "
            + ConnectionTestStatsColumns.ID3 + " NOT NULL "
            + ")"
            + " GROUP BY "
            + ConnectionTestStatsColumns.ID1 + ","
            + ConnectionTestStatsColumns.ID2 + ","
            + ConnectionTestStatsColumns.ID3 + ","
            + ConnectionTestStatsColumns.LABEL + ","
            + ConnectionTestStatsColumns.TEST_RESULT;
        // @formatter:on
    }

    /**
     * In versions < 8 of the DB, the sim and network operators were stored in this format: "BYTEL (20820)". In version 8, we separate this into three columns:
     * "BYTEL", "208" and "20".
     */
    private void updateMccMnc(SQLiteDatabase db) {
        // Get all SIM and network operators
        Cursor cursor = db.query(false, NetMonColumns.TABLE_NAME,
                new String[] { NetMonColumns._ID, NetMonColumns.SIM_OPERATOR, NetMonColumns.NETWORK_OPERATOR }, NetMonColumns.SIM_OPERATOR + " NOT NULL OR "
                        + NetMonColumns.NETWORK_OPERATOR + " NOT NULL", null, null, null, null, null);
        if (cursor != null) {
            // Keep track of all the rows we need to update.
            Map<Long, ContentValues> updates = new HashMap<Long, ContentValues>();
            try {
                int idIndex = cursor.getColumnIndex(NetMonColumns._ID);
                int simOperatorIndex = cursor.getColumnIndex(NetMonColumns.SIM_OPERATOR);
                int networkOperatorIndex = cursor.getColumnIndex(NetMonColumns.NETWORK_OPERATOR);
                // Pattern to extract the operator name, and the mccMnc string.
                Pattern pattern = Pattern.compile("^(.*) *\\(([0-9]+)\\)$");
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idIndex);
                    ContentValues cv = new ContentValues(4);
                    String simOperator = cursor.getString(simOperatorIndex);
                    String networkOperator = cursor.getString(networkOperatorIndex);
                    cv.putAll(getMccMncUpdate(pattern, simOperator, NetMonColumns.SIM_OPERATOR, NetMonColumns.SIM_MCC, NetMonColumns.SIM_MNC));
                    cv.putAll(getMccMncUpdate(pattern, networkOperator, NetMonColumns.NETWORK_OPERATOR, NetMonColumns.NETWORK_MCC, NetMonColumns.NETWORK_MNC));
                    if (cv.size() > 0) updates.put(id, cv);
                }
            } finally {
                cursor.close();
            }
            if (updates.size() > 0) {
                db.beginTransaction();
                try {
                    for (long id : updates.keySet()) {
                        db.update(NetMonColumns.TABLE_NAME, updates.get(id), NetMonColumns._ID + "=?", new String[] { String.valueOf(id) });
                    }
                } finally {
                    db.endTransaction();
                }
            }
        }
    }

    /**
     * @param operator a string of the format "BYTEL (20820)"
     * @return a ContentValues to update the sim or network operator fields: BYTEL will go into the X_OPERATOR field, 208 into the X_MCC field and 20 into the
     *         X_MNC field.
     */
    private ContentValues getMccMncUpdate(Pattern pattern, String operator, String operatorColumn, String mccColumn, String mncColumn) {
        Matcher matcher = pattern.matcher(operator);
        ContentValues cv = new ContentValues(2);
        if (matcher.matches() && matcher.groupCount() == 2) {
            String operatorMatch = matcher.group(1);
            String mccMncMatch = matcher.group(2);
            String[] mccMnc = TelephonyUtil.getMccMnc(mccMncMatch);
            cv.put(mccColumn, mccMnc[0]);
            cv.put(mncColumn, mccMnc[1]);
            cv.put(operatorColumn, operatorMatch);
        }
        return cv;
    }
}
