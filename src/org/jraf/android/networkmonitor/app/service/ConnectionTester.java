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

import android.content.ContentValues;
import android.util.Log;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.provider.NetMonColumns;

/**
 * Performs network connection tests and provides the results of each test.
 */
class ConnectionTester {
    private static final String TAG = Constants.TAG + ConnectionTester.class.getSimpleName();

    private enum NetworkTestResult {
        PASS, FAIL, SLOW
    };

    // private static final String HOST = "www.google.com";
    private static final String HOST = "173.194.34.16";
    private static final int PORT = 80;
    private static final int TIMEOUT = 15000;
    private static final int DURATION_SLOW = 5000;
    private static final String HTTP_GET = "GET / HTTP/1.1\r\n\r\n";

    /**
     * @return Run the different connection tests and return their results. The keys are db column names and values the results of the tests as strings.
     */
    public ContentValues performConnectionTests() {
        ContentValues values = new ContentValues(2);
        values.put(NetMonColumns.SOCKET_CONNECTION_TEST, getSocketTestResult().name());
        values.put(NetMonColumns.HTTP_CONNECTION_TEST, getHttpTestResult().name());
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
    private static NetworkTestResult getSocketTestResult() {
        Socket socket = null;
        try {
            // Prevent the system from closing the connection after 30 minutes of screen off.
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
    private static NetworkTestResult getHttpTestResult() {
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
}
