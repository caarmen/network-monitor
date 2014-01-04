/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013 Benoit 'BoD' Lubek (BoD@JRAF.org)
 * Copyright (C) 2013 Carmen Alvarez (c@rmen.ca)
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
package org.jraf.android.networkmonitor.app.service;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.Log;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.app.log.LogActivity;
import org.jraf.android.networkmonitor.app.main.MainActivity;
import org.jraf.android.networkmonitor.provider.NetMonColumns;
import org.jraf.android.networkmonitor.util.TelephonyUtil;

public class NetMonService extends Service {
    private static final String TAG = Constants.TAG + NetMonService.class.getSimpleName();
    private static final String PREFIX = NetMonService.class.getName() + ".";

    public static final String ACTION_PREF_CHANGED = PREFIX + "ACTION_PREF_CHANGED";
    private static final String ACTION_DISABLE = PREFIX + "ACTION_DISABLE";

    private static final int NOTIFICATION_ID = 1;


    private PowerManager mPowerManager;
    private TelephonyManager mTelephonyManager;
    private ConnectivityManager mConnectivityManager;
    private WifiManager mWifiManager;
    private long mLastWakeUp = 0;
    private volatile boolean mDestroyed;
    private ScheduledExecutorService mExecutorService;
    private Future<?> mMonitorLoopFuture;
    private ConnectionTester mConnectionTester;
    private CellSignalStrengthMonitor mCellSignalStrengthMonitor;
    private LocationMonitor mLocationMonitor;
    private WakeLock mWakeLock = null;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        if (!isServiceEnabled()) {
            Log.d(TAG, "onCreate Service is disabled: stopping now");
            stopSelf();
            return;
        }
        Log.d(TAG, "onCreate Service is enabled: starting monitor loop");

        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_PREF_CHANGED));
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);


        mExecutorService = Executors.newSingleThreadScheduledExecutor();

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mCellSignalStrengthMonitor = new CellSignalStrengthMonitor(this);
        mLocationMonitor = new LocationMonitor(this);
        mTelephonyManager.listen(mCellSignalStrengthMonitor, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS | PhoneStateListener.LISTEN_SERVICE_STATE);
        // Prevent the system from closing the connection after 30 minutes of screen off.
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mConnectionTester = new ConnectionTester();
        mWakeLock.acquire();
        reScheduleMonitorLoop();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    private void reScheduleMonitorLoop() {
        int updateInterval = getUpdateInterval();
        Log.d(TAG, "monitorLoop Sleeping " + updateInterval / 1000 + " seconds...");
        if (mMonitorLoopFuture != null) mMonitorLoopFuture.cancel(true);
        // Issue #20: We should respect the testing interval.  We shouldn't wait for more than this interval for
        // the connection tests to timeout.  
        mConnectionTester.setTimeout(updateInterval);
        mMonitorLoopFuture = mExecutorService.scheduleAtFixedRate(mMonitorLoop, 0, updateInterval, TimeUnit.MILLISECONDS);
    }

    /*
     * Broadcast.
     */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            reScheduleMonitorLoop();
        }
    };

    /*
     * Notification.
     */
    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setOngoing(true);
        builder.setSmallIcon(R.drawable.ic_stat_service_running);
        builder.setTicker(getString(R.string.service_notification_ticker));
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText(getString(R.string.service_notification_text));
        builder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
        builder.addAction(R.drawable.ic_action_stop, getString(R.string.service_notification_action_stop),
                PendingIntent.getBroadcast(this, 0, new Intent(ACTION_DISABLE), PendingIntent.FLAG_CANCEL_CURRENT));
        builder.addAction(R.drawable.ic_action_logs, getString(R.string.service_notification_action_logs),
                PendingIntent.getActivity(this, 0, new Intent(this, LogActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
        Notification notification = builder.build();
        return notification;
    }

    private void dismissNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private boolean isServiceEnabled() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_SERVICE_ENABLED, Constants.PREF_SERVICE_ENABLED_DEFAULT);
    }

    private Runnable mMonitorLoop = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "monitorLoop iteration: destroyed = " + mDestroyed);
            if (mDestroyed) {
                Log.d(TAG, "mDestroyed is true, exiting");
                return;
            }
            WakeLock wakeLock = null;
            try {
                long wakeInterval = getWakeInterval();
                long now = System.currentTimeMillis();
                long timeSinceLastWake = now - mLastWakeUp;
                Log.d(TAG, "wakeInterval = " + wakeInterval + ", lastWakeUp = " + mLastWakeUp + ", timeSinceLastWake = " + timeSinceLastWake);
                if (wakeInterval > 0 && timeSinceLastWake > wakeInterval) {
                    Log.d(TAG, "acquiring lock");
                    wakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
                    wakeLock.acquire();
                    mLastWakeUp = now;
                }

                // Insert this ContentValues into the DB.
                Log.v(TAG, "Inserting data into DB");
                ContentValues values = fetchAllValues();
                getContentResolver().insert(NetMonColumns.CONTENT_URI, values);

            } catch (Throwable t) {
                Log.v(TAG, "Error in monitorLoop: " + t.getMessage(), t);
            } finally {
                if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
            }

            // Loop if service is still enabled, otherwise stop
            if (!isServiceEnabled()) {
                Log.d(TAG, "onCreate Service is disabled: stopping now");
                stopSelf();
                return;
            }
        }
    };

    /**
     * @return all the values we will insert into the DB for this connection test.
     */
    private ContentValues fetchAllValues() {
        // Put all the data we want to log, into a ContentValues.
        ContentValues values = new ContentValues();
        values.put(NetMonColumns.TIMESTAMP, System.currentTimeMillis());
        values.putAll(mConnectionTester.performConnectionTests());
        values.putAll(mCellSignalStrengthMonitor.getSignalStrengths());
        values.putAll(mLocationMonitor.getLatestDeviceLocation());
        values.putAll(getCellLocation());
        values.putAll(getActiveNetworkInfo());
        values.putAll(getWifiInfo());
        values.putAll(getMobileDataInfo());
        values.putAll(getSIMInfo());
        return values;
    }

    private int getIntPreference(String key, String defaultValue) {
        String valueStr = PreferenceManager.getDefaultSharedPreferences(this).getString(key, defaultValue);
        int valueInt = Integer.valueOf(valueStr);
        return valueInt;
    }

    private int getUpdateInterval() {
        return getIntPreference(Constants.PREF_UPDATE_INTERVAL, Constants.PREF_UPDATE_INTERVAL_DEFAULT);
    }

    private int getWakeInterval() {
        return getIntPreference(Constants.PREF_WAKE_INTERVAL, Constants.PREF_WAKE_INTERVAL_DEFAULT);
    }

    /**
     * @return information from the currently active {@link NetworkInfo}.
     */
    private ContentValues getActiveNetworkInfo() {
        ContentValues values = new ContentValues();

        NetworkInfo activeNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo == null) return values;
        String networkType = activeNetworkInfo.getTypeName();
        String networkSubType = activeNetworkInfo.getSubtypeName();
        if (!TextUtils.isEmpty(networkSubType)) networkType += "/" + networkSubType;
        values.put(NetMonColumns.NETWORK_TYPE, networkType);
        values.put(NetMonColumns.IS_ROAMING, activeNetworkInfo.isRoaming());
        values.put(NetMonColumns.IS_AVAILABLE, activeNetworkInfo.isAvailable());
        values.put(NetMonColumns.IS_CONNECTED, activeNetworkInfo.isConnected());
        values.put(NetMonColumns.IS_FAILOVER, activeNetworkInfo.isFailover());
        values.put(NetMonColumns.DETAILED_STATE, activeNetworkInfo.getDetailedState().toString());
        values.put(NetMonColumns.REASON, activeNetworkInfo.getReason());
        values.put(NetMonColumns.EXTRA_INFO, activeNetworkInfo.getExtraInfo());
        if (Build.VERSION.SDK_INT >= 16) values.put(NetMonColumns.IS_NETWORK_METERED, isActiveNetworkMetered());
        return values;
    }

    private ContentValues getSIMInfo() {
        ContentValues values = new ContentValues(3);
        values.put(NetMonColumns.SIM_OPERATOR, mTelephonyManager.getSimOperatorName());
        String[] simMccMnc = TelephonyUtil.getMccMnc(mTelephonyManager.getSimOperator());
        values.put(NetMonColumns.SIM_MCC, simMccMnc[0]);
        values.put(NetMonColumns.SIM_MNC, simMccMnc[1]);
        values.put(NetMonColumns.NETWORK_OPERATOR, mTelephonyManager.getNetworkOperatorName());
        String[] networkMccMnc = TelephonyUtil.getMccMnc(mTelephonyManager.getNetworkOperator());
        values.put(NetMonColumns.NETWORK_MCC, networkMccMnc[0]);
        values.put(NetMonColumns.NETWORK_MNC, networkMccMnc[1]);
        int simState = mTelephonyManager.getSimState();
        values.put(NetMonColumns.SIM_STATE, TelephonyUtil.getConstantName("SIM_STATE", null, simState));
        return values;
    }

    private ContentValues getMobileDataInfo() {
        ContentValues values = new ContentValues(3);
        values.put(NetMonColumns.MOBILE_DATA_NETWORK_TYPE, TelephonyUtil.getConstantName("NETWORK_TYPE", null, mTelephonyManager.getNetworkType()));
        values.put(NetMonColumns.DATA_ACTIVITY, TelephonyUtil.getConstantName("DATA_ACTIVITY", null, mTelephonyManager.getDataActivity()));
        values.put(NetMonColumns.DATA_STATE, TelephonyUtil.getConstantName("DATA", "DATA_ACTIVITY", mTelephonyManager.getDataState()));
        return values;
    }

    /**
     * @return the SSID, BSSID and signal strength of the currently connected WiFi network, if any.
     */
    private ContentValues getWifiInfo() {
        WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
        ContentValues result = new ContentValues(2);
        if (connectionInfo == null || connectionInfo.getNetworkId() < 0) return result;
        result.put(NetMonColumns.WIFI_SSID, connectionInfo.getSSID());
        result.put(NetMonColumns.WIFI_BSSID, connectionInfo.getBSSID());
        int signalLevel = WifiManager.calculateSignalLevel(connectionInfo.getRssi(), 5);
        result.put(NetMonColumns.WIFI_SIGNAL_STRENGTH, signalLevel);
        result.put(NetMonColumns.WIFI_RSSI, connectionInfo.getRssi());

        return result;
    }

    /**
     * @return information from the current cell we are connected to.
     */
    private ContentValues getCellLocation() {
        ContentValues values = new ContentValues();
        CellLocation cellLocation = mTelephonyManager.getCellLocation();
        if (cellLocation instanceof GsmCellLocation) {
            GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
            int cid = gsmCellLocation.getCid();
            // The javadoc says the cell id should be less than FFFF, but this
            // isn't always so. We'll report both the full cell id returned by
            // Android, and the truncated one (taking only the last 2 bytes).
            int shortCid = cid > 0 ? cid & 0xFFFF : cid;
            int rnc = cid > 0 ? cid >> 16 & 0xFFFF : 0;
            values.put(NetMonColumns.GSM_FULL_CELL_ID, cid);
            if (rnc > 0) values.put(NetMonColumns.GSM_RNC, rnc);
            values.put(NetMonColumns.GSM_SHORT_CELL_ID, shortCid);
            values.put(NetMonColumns.GSM_CELL_LAC, gsmCellLocation.getLac());
            if (Build.VERSION.SDK_INT >= 9) values.put(NetMonColumns.GSM_CELL_PSC, getPsc(gsmCellLocation));
        } else if (cellLocation instanceof CdmaCellLocation) {
            CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLocation;
            values.put(NetMonColumns.CDMA_CELL_BASE_STATION_ID, cdmaCellLocation.getBaseStationId());
            values.put(NetMonColumns.CDMA_CELL_LATITUDE, cdmaCellLocation.getBaseStationLatitude());
            values.put(NetMonColumns.CDMA_CELL_LONGITUDE, cdmaCellLocation.getBaseStationLongitude());
            values.put(NetMonColumns.CDMA_CELL_NETWORK_ID, cdmaCellLocation.getNetworkId());
            values.put(NetMonColumns.CDMA_CELL_SYSTEM_ID, cdmaCellLocation.getSystemId());
        }
        return values;
    }

    @TargetApi(9)
    private int getPsc(GsmCellLocation gsmCellLocation) {
        return gsmCellLocation.getPsc();
    }

    @TargetApi(16)
    private boolean isActiveNetworkMetered() {
        return mConnectivityManager.isActiveNetworkMetered();
    }

    @Override
    public void onDestroy() {
        mDestroyed = true;
        if (mLocationMonitor != null) mLocationMonitor.onDestroy();
        if (mTelephonyManager != null) mTelephonyManager.listen(mCellSignalStrengthMonitor, PhoneStateListener.LISTEN_NONE);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        dismissNotification();
        if (mWakeLock != null && mWakeLock.isHeld()) mWakeLock.release();
        super.onDestroy();
    }
}
