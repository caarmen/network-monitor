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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
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
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;

public class NetMonService extends Service {
    private static final String TAG = Constants.TAG + NetMonService.class.getSimpleName();
    private static final String PREFIX = NetMonService.class.getName() + ".";

    public static final String ACTION_PREF_CHANGED = PREFIX + "ACTION_PREF_CHANGED";
    private static final String ACTION_DISABLE = PREFIX + "ACTION_DISABLE";

    // private static final String HOST = "www.google.com";
    private static final String HOST = "173.194.34.16";
    private static final int PORT = 80;
    private static final int TIMEOUT = 15000;
    private static final int DURATION_SLOW = 5000;
    private static final String HTTP_GET = "GET / HTTP/1.1\r\n\r\n";
    private static final String UNKNOWN = "";
    private static final int NOTIFICATION_ID = 1;

    private enum NetworkTestResult {
        PASS, FAIL, SLOW
    };

    private PowerManager mPowerManager;
    private TelephonyManager mTelephonyManager;
    private ConnectivityManager mConnectivityManager;
    private LocationManager mLocationManager;
    private LocationClient mLocationClient;
    private NetMonPhoneStateListener mPhoneStateListener;
    private WifiManager mWifiManager;
    private long mLastWakeUp = 0;
    private int mLastSignalStrength;
    private int mLastSignalStrengthDbm;
    private volatile boolean mDestroyed;
    private ScheduledExecutorService mExecutorService;
    private Future<?> mMonitorLoopFuture;
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
        mPhoneStateListener = new NetMonPhoneStateListener(NetMonService.this);

        registerBroadcastReceiver();
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);


        mExecutorService = Executors.newSingleThreadScheduledExecutor();

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mLocationClient = new LocationClient(NetMonService.this, mConnectionCallbacks, mConnectionFailedListener);
        mLocationClient.connect();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS | PhoneStateListener.LISTEN_SERVICE_STATE);
        // Prevent the system from closing the connection after 30 minutes of screen off.
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.acquire();
        reScheduleMonitorLoop();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    private void reScheduleMonitorLoop() {
        long updateInterval = getUpdateInterval();
        Log.d(TAG, "monitorLoop Sleeping " + updateInterval / 1000 + " seconds...");
        if (mMonitorLoopFuture != null) mMonitorLoopFuture.cancel(true);
        mMonitorLoopFuture = mExecutorService.scheduleAtFixedRate(mMonitorLoop, 0, updateInterval, TimeUnit.MILLISECONDS);
    }

    /*
     * Broadcast.
     */

    private void registerBroadcastReceiver() {
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_PREF_CHANGED));
    }

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
            try {
                // Put all the data we want to log, into a ContentValues.
                ContentValues values = new ContentValues();
                values.put(NetMonColumns.TIMESTAMP, System.currentTimeMillis());
                values.put(NetMonColumns.SOCKET_CONNECTION_TEST, getSocketTestResult().name());
                values.put(NetMonColumns.HTTP_CONNECTION_TEST, getHttpTestResult().name());
                values.putAll(getActiveNetworkInfo());
                values.put(NetMonColumns.CELL_SIGNAL_STRENGTH, mLastSignalStrength);
                if (mLastSignalStrengthDbm != NetMonSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN)
                    values.put(NetMonColumns.CELL_SIGNAL_STRENGTH_DBM, mLastSignalStrengthDbm);
                values.put(NetMonColumns.MOBILE_DATA_NETWORK_TYPE, getDataNetworkType());
                values.putAll(getWifiInfo());
                values.putAll(getCellLocation());
                values.put(NetMonColumns.DATA_ACTIVITY, getDataActivity());
                values.put(NetMonColumns.DATA_STATE, getDataState());
                values.put(NetMonColumns.SIM_STATE, getSimState());
                if (Build.VERSION.SDK_INT >= 16) values.put(NetMonColumns.IS_NETWORK_METERED, isActiveNetworkMetered());
                double[] location = getLatestLocation();
                if (location != null) {
                    values.put(NetMonColumns.DEVICE_LATITUDE, location[0]);
                    values.put(NetMonColumns.DEVICE_LONGITUDE, location[1]);
                }

                // Insert this ContentValues into the DB.
                Log.v(TAG, "Inserting data into DB");
                getContentResolver().insert(NetMonColumns.CONTENT_URI, values);

            } catch (Throwable t) {
                Log.v(TAG, "Error in monitorLoop: " + t.getMessage(), t);
            }

            // Loop if service is still enabled, otherwise stop
            if (!isServiceEnabled()) {
                Log.d(TAG, "onCreate Service is disabled: stopping now");
                stopSelf();
                return;
            }
        }

    };

    private long getLongPreference(String key, String defaultValue) {
        String valueStr = PreferenceManager.getDefaultSharedPreferences(this).getString(key, defaultValue);
        long valueLong = Long.valueOf(valueStr);
        return valueLong;
    }

    private long getUpdateInterval() {
        return getLongPreference(Constants.PREF_UPDATE_INTERVAL, Constants.PREF_UPDATE_INTERVAL_DEFAULT);
    }

    private long getWakeInterval() {
        return getLongPreference(Constants.PREF_WAKE_INTERVAL, Constants.PREF_WAKE_INTERVAL_DEFAULT);
    }

    /**
     * Try to open a connection to an HTTP server, and execute a simple GET request. If we can read a response to the GET request, we consider that the network
     * is up. This test uses a basic socket connection.
     * 
     * @return
     * 
     * @return {@link NetworkTestResult#PASS} if we were able to read a response to a GET request quickly, {@link NetworkTestResult#FAIL} if any error occurred
     *         trying to execute the GET, or {@link NetworkTestResult#SLOW} if we were able to read a response, but it took too long.
     */
    private NetworkTestResult getSocketTestResult() {
        Socket socket = null;
        WakeLock wakeLock = null;
        try {
            // Prevent the system from closing the connection after 30 minutes of screen off.
            long now = System.currentTimeMillis();
            long wakeInterval = getWakeInterval();
            long timeSinceLastWake = now - mLastWakeUp;
            Log.d(TAG, "wakeInterval = " + wakeInterval + ", lastWakeUp = " + mLastWakeUp + ", timeSinceLastWake = " + timeSinceLastWake);
            if (wakeInterval > 0 && timeSinceLastWake > wakeInterval) {
                Log.d(TAG, "acquiring lock");
                wakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
                wakeLock.acquire();
                mLastWakeUp = now;
            }
            long before = System.currentTimeMillis();
            socket = new Socket();
            socket.setSoTimeout(TIMEOUT);
            Log.d(TAG, "getSocketTestResult Resolving " + HOST);
            InetSocketAddress remoteAddr = new InetSocketAddress(HOST, PORT);
            InetAddress address = remoteAddr.getAddress();
            if (address == null) {
                Log.d(TAG, "getSocketTestResult Could not resolve");
                return NetworkTestResult.FAIL;
            }
            Log.d(TAG, "getSocketTestResult Resolved " + address.getHostAddress());
            Log.d(TAG, "getSocketTestResult Connecting...");
            socket.connect(remoteAddr, TIMEOUT);
            Log.d(TAG, "getSocketTestResult Connected");

            Log.d(TAG, "getSocketTestResult Sending GET...");
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(HTTP_GET.getBytes("utf-8"));
            outputStream.flush();
            Log.d(TAG, "getSocketTestResult Sent GET");
            InputStream inputStream = socket.getInputStream();
            Log.d(TAG, "getSocketTestResult Reading...");
            int read = inputStream.read();
            Log.d(TAG, "getSocketTestResult Read read=" + read);
            long after = System.currentTimeMillis();
            if (read != -1) {
                if (after - before > DURATION_SLOW) return NetworkTestResult.SLOW;
                else
                    return NetworkTestResult.PASS;
            }
            return NetworkTestResult.FAIL;
        } catch (Throwable t) {
            Log.d(TAG, "getSocketTestResult Caught an exception", t);
            return NetworkTestResult.FAIL;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.w(TAG, "getSocketTestResult Could not close socket", e);
                }
            }
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        }
    }

    /**
     * Try to open a connection to an HTTP server, and execute a simple GET request. If we can read a response to the GET request, we consider that the network
     * is up. This test uses an HttpURLConnection.
     * 
     * @return
     * 
     * @return {@link NetworkTestResult#PASS} if we were able to read a response to a GET request quickly, {@link NetworkTestResult#FAIL} if any error occurred
     *         trying to execute the GET, or {@link NetworkTestResult#SLOW} if we were able to read a response, but it took too long.
     */
    private NetworkTestResult getHttpTestResult() {
        InputStream inputStream = null;
        try {
            long before = System.currentTimeMillis();
            URL url = new URL("http", HOST, PORT, "/");
            URLConnection connection = url.openConnection();
            connection.setReadTimeout(TIMEOUT);
            connection.addRequestProperty("Cache-Control", "no-cache");
            connection.setUseCaches(false);
            inputStream = connection.getInputStream();
            long after = System.currentTimeMillis();
            if (inputStream.read() > 0) {
                if (after - before > DURATION_SLOW) return NetworkTestResult.SLOW;
                else
                    return NetworkTestResult.PASS;
            } else {
                return NetworkTestResult.FAIL;
            }
        } catch (Throwable t) {
            Log.d(TAG, "getHttpTestResult Caught an exception", t);
            return NetworkTestResult.FAIL;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.w(TAG, "getHttpTestResult Could not close stream", e);
                }
            }
        }
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
        values.put(NetMonColumns.SIM_OPERATOR, mTelephonyManager.getSimOperatorName() + "(" + mTelephonyManager.getSimOperator() + ")");
        values.put(NetMonColumns.NETWORK_OPERATOR, mTelephonyManager.getNetworkOperatorName() + "(" + mTelephonyManager.getNetworkOperator() + ")");
        return values;
    }

    private String getDataNetworkType() {
        int networkType = mTelephonyManager.getNetworkType();
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return "1xRTT";
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return "CDMA";
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "EDGE";
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return "EHRPD";
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return "EVDO_0";
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return "EVDO_A";
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return "EVDO_B";
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "GPRS";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return "HSDPA";
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return "HSPA";
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "HSPAP";
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return "HSUPA";
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "IDEN";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "LTE";
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return "UMTS";
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                return "UNKNOWN";
            default:
                return UNKNOWN;
        }
    }

    private String getDataActivity() {
        int dataActivity = mTelephonyManager.getDataActivity();
        switch (dataActivity) {
            case TelephonyManager.DATA_ACTIVITY_DORMANT:
                return "DORMANT";
            case TelephonyManager.DATA_ACTIVITY_IN:
                return "IN";
            case TelephonyManager.DATA_ACTIVITY_INOUT:
                return "INOUT";
            case TelephonyManager.DATA_ACTIVITY_NONE:
                return "NONE";
            case TelephonyManager.DATA_ACTIVITY_OUT:
                return "OUT";
            default:
                return UNKNOWN;
        }
    }

    private String getDataState() {
        int dataState = mTelephonyManager.getDataState();
        switch (dataState) {
            case TelephonyManager.DATA_CONNECTED:
                return "CONNECTED";
            case TelephonyManager.DATA_CONNECTING:
                return "CONNECTING";
            case TelephonyManager.DATA_DISCONNECTED:
                return "DISCONNECTED";
            case TelephonyManager.DATA_SUSPENDED:
                return "SUSPENDED";
            default:
                return UNKNOWN;
        }
    }

    private String getSimState() {
        int simState = mTelephonyManager.getSimState();
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
                return "ABSENT";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                return "NETWORK LOCKED";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                return "PIN REQUIRED";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                return "PUK REQUIRED";
            case TelephonyManager.SIM_STATE_READY:
                return "READY";
            case TelephonyManager.SIM_STATE_UNKNOWN:
                return "UNKNOWN";
            default:
                return UNKNOWN;
        }
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
            values.put(NetMonColumns.GSM_FULL_CELL_ID, cid);
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

    /**
     * @return the last location the device recorded as an array of latitude and longitude. Tries to use Google Play Services if available. Otherwise falls back
     *         to the most recently retrieved location among all the providers.
     */
    private double[] getLatestLocation() {
        Location mostRecentLocation = null;
        // Try getting the location from the LocationClient
        if (mLocationClient.isConnected()) {
            mostRecentLocation = mLocationClient.getLastLocation();
            Log.v(TAG, "Got location from LocationClient: " + mostRecentLocation);
        }
        // Fall back to the old way.
        if (mostRecentLocation == null) {
            List<String> providers = mLocationManager.getProviders(true);
            long mostRecentFix = 0;
            for (String provider : providers) {
                Location location = mLocationManager.getLastKnownLocation(provider);
                Log.v(TAG, "Location for provider " + provider + ": " + location);
                if (location == null) continue;
                long time = location.getTime();
                if (time > mostRecentFix) {
                    time = mostRecentFix;
                    mostRecentLocation = location;
                }
            }
        }
        Log.v(TAG, "Most recent location: " + mostRecentLocation);
        if (mostRecentLocation != null) return new double[] { mostRecentLocation.getLatitude(), mostRecentLocation.getLongitude() };
        return null;
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
        if (mLocationClient != null) mLocationClient.disconnect();
        if (mTelephonyManager != null) mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        dismissNotification();
        if (mWakeLock != null && mWakeLock.isHeld()) mWakeLock.release();
        super.onDestroy();
    }

    private ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            Log.v(TAG, "onConnected: " + bundle);
        }

        @Override
        public void onDisconnected() {
            Log.v(TAG, "onDisconnected");
        }
    };

    private OnConnectionFailedListener mConnectionFailedListener = new OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Log.v(TAG, "onConnectionFailed: " + result);
        }
    };

    private class NetMonPhoneStateListener extends PhoneStateListener {
        private final NetMonSignalStrength mNetMonSignalStrength;

        public NetMonPhoneStateListener(Context context) {
            mNetMonSignalStrength = new NetMonSignalStrength(context);
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            mLastSignalStrength = mNetMonSignalStrength.getLevel(signalStrength);
            mLastSignalStrengthDbm = mNetMonSignalStrength.getDbm(signalStrength);
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Log.v(TAG, "onServiceStateChanged " + serviceState);
            if (serviceState.getState() != ServiceState.STATE_IN_SERVICE) {
                mLastSignalStrength = NetMonSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
                mLastSignalStrengthDbm = NetMonSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
            }
        }
    }
}
