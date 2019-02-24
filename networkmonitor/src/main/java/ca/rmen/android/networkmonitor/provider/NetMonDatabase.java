/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013-2017 Carmen Alvarez (c@rmen.ca)
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.collection.LongSparseArray;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.Constants.ConnectionType;
import ca.rmen.android.networkmonitor.util.TelephonyUtil;

public class NetMonDatabase extends SQLiteOpenHelper {
    private static final String TAG = Constants.TAG + NetMonDatabase.class.getSimpleName();

    public static final String DATABASE_NAME = "networkmonitor.db";
    private static final int DATABASE_VERSION = 18;

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
            + NetMonColumns.SERVICE_STATE + " TEXT, "
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
            + NetMonColumns.WIFI_FREQUENCY+ " INTEGER, "
            + NetMonColumns.WIFI_CHANNEL+ " INTEGER, "
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
            + NetMonColumns.DEVICE_SPEED + " REAL, "
            + NetMonColumns.CELL_SIGNAL_STRENGTH + " INTEGER, "
            + NetMonColumns.CELL_SIGNAL_STRENGTH_DBM + " INTEGER, "
            + NetMonColumns.CELL_ASU_LEVEL + " INTEGER, "
            + NetMonColumns.GSM_BER+ " INTEGER, "
            //+ NetMonColumns.EVDO_ECIO + " INTEGER, "
            + NetMonColumns.LTE_RSRQ + " INTEGER, "
            + NetMonColumns.CDMA_CELL_BASE_STATION_ID + " INTEGER, "
            + NetMonColumns.CDMA_CELL_LATITUDE + " INTEGER, "
            + NetMonColumns.CDMA_CELL_LONGITUDE + " INTEGER, "
            + NetMonColumns.CDMA_CELL_NETWORK_ID + " INTEGER, "
            + NetMonColumns.CDMA_CELL_SYSTEM_ID + " INTEGER, "
            + NetMonColumns.GSM_FULL_CELL_ID + " INTEGER, "
            + NetMonColumns.GSM_RNC + " INTEGER, "
            + NetMonColumns.GSM_SHORT_CELL_ID + " INTEGER, "
            + NetMonColumns.GSM_CELL_LAC + " INTEGER, "
            + NetMonColumns.GSM_CELL_PSC + " INTEGER, "
            + NetMonColumns.LTE_CELL_CI + " INTEGER, "
            + NetMonColumns.LTE_CELL_EARFCN + " INTEGER, "
            + NetMonColumns.LTE_CELL_PCI + " INTEGER, "
            + NetMonColumns.LTE_CELL_TAC + " INTEGER, "
            + NetMonColumns.NETWORK_INTERFACE+ " TEXT, "
            + NetMonColumns.IPV4_ADDRESS+ " TEXT,"
            + NetMonColumns.IPV6_ADDRESS+ " TEXT,"
            + NetMonColumns.BATTERY_LEVEL+ " INTEGER, "
            + NetMonColumns.MOST_CONSUMING_APP_NAME + " TEXT, "
            + NetMonColumns.MOST_CONSUMING_APP_BYTES + " INTEGER, "
            + NetMonColumns.DOWNLOAD_SPEED+ " TEXT, "
            + NetMonColumns.UPLOAD_SPEED+ " TEXt"
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

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V9_GSM_RNC = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN " + NetMonColumns.GSM_RNC
            + " INTEGER";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V9_GSM_RNC_UPDATE = "UPDATE " + NetMonColumns.TABLE_NAME + " SET " + NetMonColumns.GSM_RNC
            + " = (" + NetMonColumns.GSM_FULL_CELL_ID + " >> 16) & " + 0xFFFF + " WHERE " + NetMonColumns.GSM_FULL_CELL_ID + " > " + 0xFFFF;

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V10_NETWORK_INTERFACE = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.NETWORK_INTERFACE + " TEXT";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V10_IPV4_ADDRESS = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.IPV4_ADDRESS + " TEXT";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V10_IPV6_ADDRESS = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.IPV6_ADDRESS + " TEXT";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V11_BATTERY_LEVEL = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.BATTERY_LEVEL + " INTEGER";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V12_WIFI_FREQUENCY = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.WIFI_FREQUENCY + " INTEGER";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V12_WIFI_CHANNEL = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.WIFI_CHANNEL + " INTEGER";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V13_DOWNLOAD_SPEED = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.DOWNLOAD_SPEED + " TEXT";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V13_UPLOAD_SPEED = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.UPLOAD_SPEED + " TEXT";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V14_GSM_BER = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.GSM_BER + " INTEGER";

    //private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V14_EVDO_ECIO = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
    //        + NetMonColumns.EVDO_ECIO + " INTEGER";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V14_LTE_RSRQ = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.LTE_RSRQ + " INTEGER";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V15_SERVICE_STATE = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.SERVICE_STATE + " TEXT";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V16_DEVICE_SPEED = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.DEVICE_SPEED + " REAL";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V17_MOST_CONSUMING_APP_NAME = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.MOST_CONSUMING_APP_NAME + " TEXT";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V17_MOST_CONSUMING_APP_BYTES = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.MOST_CONSUMING_APP_BYTES + " INTEGER";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V18_LTE_CELL_CI = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.LTE_CELL_CI + " INTEGER";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V18_LTE_CELL_EARFCN = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.LTE_CELL_EARFCN + " INTEGER";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V18_LTE_CELL_PCI = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.LTE_CELL_PCI + " INTEGER";

    private static final String SQL_UPDATE_TABLE_NETWORKMONITOR_V18_LTE_CELL_TAC = "ALTER TABLE " + NetMonColumns.TABLE_NAME + " ADD COLUMN "
            + NetMonColumns.LTE_CELL_TAC+ " INTEGER";

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

    @SuppressWarnings("ConstantConditions") // It's not THAT hard to analyze...
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

        if (oldVersion < 9) {
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V9_GSM_RNC);
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V9_GSM_RNC_UPDATE);
        }

        if (oldVersion < 10) {
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V10_NETWORK_INTERFACE);
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V10_IPV4_ADDRESS);
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V10_IPV6_ADDRESS);
        }

        if (oldVersion < 11) {
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V11_BATTERY_LEVEL);
        }

        if (oldVersion < 12) {
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V12_WIFI_FREQUENCY);
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V12_WIFI_CHANNEL);
        }

        if (oldVersion < 13) {
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V13_DOWNLOAD_SPEED);
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V13_UPLOAD_SPEED);
        }

        if (oldVersion < 14) {
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V14_GSM_BER);
            //db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V14_EVDO_ECIO);
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V14_LTE_RSRQ);
        }

        if (oldVersion < 15) {
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V15_SERVICE_STATE);
        }

        if (oldVersion < 16) {
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V16_DEVICE_SPEED);
        }

        if (oldVersion < 17) {
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V17_MOST_CONSUMING_APP_NAME);
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V17_MOST_CONSUMING_APP_BYTES);
        }

        if (oldVersion < 18) {
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V18_LTE_CELL_CI);
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V18_LTE_CELL_EARFCN);
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V18_LTE_CELL_PCI);
            db.execSQL(SQL_UPDATE_TABLE_NETWORKMONITOR_V18_LTE_CELL_TAC);
        }
    }

    /**
     * @return a query to retrieve the stats of the connection test results.
     */
    private static String buildConnectionTestQuery() {
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
    private static String buildConnectionTestSubQuery(ConnectionType type, String id1Column, String id2Column, String id3Column, String labelColumn,
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
        try {
            // Get all SIM and network operators
            Cursor cursor = db.query(false, NetMonColumns.TABLE_NAME, new String[] { NetMonColumns._ID, NetMonColumns.SIM_OPERATOR,
                    NetMonColumns.NETWORK_OPERATOR }, NetMonColumns.SIM_OPERATOR + " NOT NULL OR " + NetMonColumns.NETWORK_OPERATOR + " NOT NULL", null, null,
                    null, null, null);
            if (cursor != null) {
                // Keep track of all the rows we need to update.
                LongSparseArray<ContentValues> mccMncUpdates = new LongSparseArray<>();
                try {
                    int idIndex = cursor.getColumnIndex(NetMonColumns._ID);
                    int simOperatorIndex = cursor.getColumnIndex(NetMonColumns.SIM_OPERATOR);
                    int networkOperatorIndex = cursor.getColumnIndex(NetMonColumns.NETWORK_OPERATOR);
                    // Pattern to extract the operator name, and the mccMnc string.
                    // In the example of "BYTEL (20820)", $1 will match BYTEL, and $2 will match 20820.
                    Pattern pattern = Pattern.compile("^(.*) *\\(([0-9]+)\\)$");
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idIndex);
                        ContentValues cv = new ContentValues(4);
                        String simOperator = cursor.getString(simOperatorIndex);
                        String networkOperator = cursor.getString(networkOperatorIndex);
                        cv.putAll(getMccMncUpdate(pattern, simOperator, NetMonColumns.SIM_OPERATOR, NetMonColumns.SIM_MCC, NetMonColumns.SIM_MNC));
                        cv.putAll(getMccMncUpdate(pattern, networkOperator, NetMonColumns.NETWORK_OPERATOR, NetMonColumns.NETWORK_MCC,
                                NetMonColumns.NETWORK_MNC));
                        if (cv.size() > 0) mccMncUpdates.put(id, cv);
                    }
                } finally {
                    cursor.close();
                }
                if (mccMncUpdates.size() > 0) {
                    Log.v(TAG, "Will update MCC/MNC columns for " + mccMncUpdates.size() + " records");
                    for (int i = 0; i < mccMncUpdates.size(); i++) {
                        long id = mccMncUpdates.keyAt(i);
                        db.update(NetMonColumns.TABLE_NAME, mccMncUpdates.get(id), NetMonColumns._ID + "=?", new String[]{String.valueOf(id)});
                    }
                    Log.v(TAG, "MCC/MNC update complete");
                } else {
                    Log.v(TAG, "No MCC/MNC records to update");
                }
            }
        } catch (Throwable t) {
            // Yes, this is a cheap way to handle errors.
            // Migrating the mcc/mnc columns is not really that important.  If a user has some funky data that we can't anticipate, just don't migrate these fields.  We don't want to prevent the user from using the app because of this.
            Log.w(TAG, "Error trying to migrate mcc/mnc columns: " + t.getMessage(), t);
        }
    }

    /**
     * @param operator a string of the format "BYTEL (20820)"
     * @return a ContentValues to update the sim or network operator values: BYTEL will go into the X_operator column, 208 into the X_mcc column and 20 into the
     *         X_mnc column.
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
