package org.jraf.android.networkmonitor.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.jraf.android.networkmonitor.Constants;

class NetMonDatabase extends SQLiteOpenHelper {
    private static final String TAG = Constants.TAG + NetMonDatabase.class.getSimpleName();

    private static final String DATABASE_NAME = "networkmonitor.db";
    private static final int DATABASE_VERSION = 1;

    // @formatter:off
    private static final String SQL_CREATE_TABLE_NETWORKMONITOR = "CREATE TABLE IF NOT EXISTS "
            + NetMonColumns.TABLE_NAME + " ( "
            + NetMonColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + NetMonColumns.TIMESTAMP + " INTEGER, "
            + NetMonColumns.GOOGLE_CONNECTION_TEST + " TEXT, "
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
            + NetMonColumns.IS_NETWORK_METERED + " INTEGER, "
            + NetMonColumns.DEVICE_LATITUDE + " REAL, "
            + NetMonColumns.DEVICE_LONGITUDE + " REAL, "
            + NetMonColumns.CDMA_CELL_BASE_STATION_ID + " INTEGER, "
            + NetMonColumns.CDMA_CELL_LATITUDE + " INTEGER, "
            + NetMonColumns.CDMA_CELL_LONGITUDE + " INTEGER, "
            + NetMonColumns.CDMA_CELL_NETWORK_ID + " INTEGER, "
            + NetMonColumns.CDMA_CELL_SYSTEM_ID + " INTEGER, "
            + NetMonColumns.GSM_CELL_ID + " INTEGER, "
            + NetMonColumns.GSM_CELL_LAC + " INTEGER, "
            + NetMonColumns.GSM_CELL_PSC + " INTEGER "
            + " );";

    // @formatter:on

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
    }
}
