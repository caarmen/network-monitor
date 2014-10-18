/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014 Benoit 'BoD' Lubek (BoD@JRAF.org)
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
package ca.rmen.android.networkmonitor.app.service.datasources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.app.service.NetMonNotification;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.Log;
import ca.rmen.android.networkmonitor.util.TelephonyUtil;

/**
 * Performs network connection tests and provides the results of each test.
 */
class ConnectionTesterDataSource implements NetMonDataSource {
    private static final String TAG = Constants.TAG + ConnectionTesterDataSource.class.getSimpleName();
    private Context mContext;

    private enum NetworkTestResult {
        PASS, FAIL, SLOW
    };

    private static final int PORT = 80;
    private static final int DURATION_SLOW = 5000;

    // The maximum connection and read timeout for a connection test, in ms.  We may actually set a lower timeout if the user has set the app to test very frequently (ex: every 10 seconds).
    private static final int MAX_TIMEOUT_PER_TEST = 15000;

    private static final String HTTP_GET = "GET / HTTP/1.1\r\n\r\n";

    // The timeout for each connection test, in ms.
    private int mTimeout;

    public ConnectionTesterDataSource() {
        Log.v(TAG, "Constructor");
    }

    @Override
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        mContext = context;
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(mPrefListener);
        int updateInterval = NetMonPreferences.getInstance(context).getUpdateInterval();
        setTimeout(updateInterval);
    }

    @Override
    public void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(mContext).unregisterOnSharedPreferenceChangeListener(mPrefListener);
    }

    /**
     * @param maxTimeout the maximum total time it should take to perform all connection tests.
     */
    private void setTimeout(int timeout) {
        Log.v(TAG, "setTimeout " + timeout);
        // Divide the total timeout by the total number of connection tests, to get the maximum timeout for each test.
        mTimeout = Math.min(timeout / 2, MAX_TIMEOUT_PER_TEST);
        Log.v(TAG, "setTimeout: set timeout to " + mTimeout);
    }

    /**
     * @return Run the different connection tests and return their results. The keys are db column names and values the results of the tests as strings.
     */
    @Override
    public ContentValues getContentValues() {
        Log.v(TAG, "getContentValues");
        ContentValues values = new ContentValues(2);
        NetworkTestResult socketTestResult = getSocketTestResult();
        NetworkTestResult httpTestResult = getHttpTestResult();
        values.put(NetMonColumns.SOCKET_CONNECTION_TEST, socketTestResult.name());
        values.put(NetMonColumns.HTTP_CONNECTION_TEST, httpTestResult.name());
        if ((socketTestResult == NetworkTestResult.FAIL || httpTestResult == NetworkTestResult.FAIL) && shouldHaveDataConnection()) {
            Log.v(TAG, "A connection test failed even though we expect to have a data connection");
            NetMonNotification.showFailedTestNotification(mContext);
        } else {
            NetMonNotification.dismissFailedTestNotification(mContext);
        }
        return values;
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
        Log.v(TAG, "getSocketTestResult BEGIN");
        Socket socket = null;
        try {
            // Prevent the system from closing the connection after 30 minutes of screen off.
            long before = System.currentTimeMillis();
            socket = new Socket();
            socket.setSoTimeout(mTimeout);
            String host = NetMonPreferences.getInstance(mContext).getTestServer();
            Log.d(TAG, "getSocketTestResult Resolving " + host);
            InetSocketAddress remoteAddr = new InetSocketAddress(host, PORT);
            InetAddress address = remoteAddr.getAddress();
            if (address == null) {
                Log.d(TAG, "getSocketTestResult Could not resolve");
                return NetworkTestResult.FAIL;
            }
            Log.d(TAG, "getSocketTestResult Resolved " + address.getHostAddress());
            Log.d(TAG, "getSocketTestResult Connecting...");
            socket.connect(remoteAddr, mTimeout);
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
            Log.v(TAG, "getSocketTestResult END");
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
        Log.v(TAG, "getHttpTestResult BEGIN");
        InputStream inputStream = null;
        try {
            long before = System.currentTimeMillis();
            String host = NetMonPreferences.getInstance(mContext).getTestServer();
            URL url = new URL("http", host, PORT, "/");
            URLConnection connection = url.openConnection();
            Log.v(TAG, "Opened connection");
            connection.setConnectTimeout(mTimeout);
            connection.setReadTimeout(mTimeout);
            connection.addRequestProperty("Cache-Control", "no-cache");
            connection.setUseCaches(false);
            Log.v(TAG, "Will open input stream");
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
            Log.v(TAG, "getHttpTestResult END");
        }
    }

    private OnSharedPreferenceChangeListener mPrefListener = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            // Issue #20: We should respect the testing interval.  We shouldn't wait for more than this interval for
            // the connection tests to timeout.
            if (NetMonPreferences.PREF_UPDATE_INTERVAL.equals(key)) {
                String valueStr = sharedPreferences.getString(key, NetMonPreferences.PREF_UPDATE_INTERVAL_DEFAULT);
                int updateInterval = Integer.valueOf(valueStr);
                Log.v(TAG, "updateInterval changed to " + updateInterval);
                setTimeout(updateInterval);
            }
        }
    };

    /**
     * @return true if we should have an internet connection
     */
    private boolean shouldHaveDataConnection() {
        // If we're connected to a WiFi access point, we should have an internet connection.
        WifiManager wifiMgr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo connectionInfo = wifiMgr.getConnectionInfo();
        if (connectionInfo != null && connectionInfo.getNetworkId() > -1) return true;

        // If we're in airplane mode, we can't have an Internet connection.
        if (TelephonyUtil.isAirplaneModeOn(mContext)) return false;

        // We're not on WiFi, and we're not in airplane mode.
        // Assume we should have Internet access if mobile data is enabled.
        return TelephonyUtil.isMobileDataEnabled(mContext);
    }


}
