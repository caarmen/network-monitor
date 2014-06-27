/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
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
package ca.rmen.android.networkmonitor.app.speedtest;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestPreferences.SpeedTestDownloadConfig;
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestResult.SpeedTestStatus;
import ca.rmen.android.networkmonitor.util.Log;


/**
 * Downloads a file and calculates the download speed.
 */
public class DownloadSpeedTest {
    private static final String TAG = Constants.TAG + DownloadSpeedTest.class.getSimpleName();

    // The maximum connection and read timeout 
    private static final int TIMEOUT = 5000;

    public static SpeedTestResult download(SpeedTestDownloadConfig config) {
        Log.v(TAG, "download " + config);
        URL url;
        try {
            url = new URL(config.url);
        } catch (MalformedURLException e) {
            Log.e(TAG, "download: incorrect url " + config.url, e);
            return new SpeedTestResult(0, 0, SpeedTestStatus.INVALID_FILE);
        }

        SpeedTestStatus status = SpeedTestStatus.UNKNOWN;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        int totalRead = 0;
        long before = System.currentTimeMillis();
        try {
            URLConnection connection = url.openConnection();
            Log.v(TAG, "Opened connection");
            outputStream = new FileOutputStream(config.file);
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.addRequestProperty("Cache-Control", "no-cache");
            connection.setUseCaches(false);
            Log.v(TAG, "Will open input stream");
            inputStream = connection.getInputStream();
            byte[] buffer = new byte[1024];
            int read = 0;
            do {
                read = inputStream.read(buffer);
                if (read > 0) outputStream.write(buffer, 0, read);
                totalRead += read;

            } while (read > 0);
            if (totalRead > 0) status = SpeedTestStatus.SUCCESS;
            long after = System.currentTimeMillis();
            return new SpeedTestResult(totalRead, after - before, status);
        } catch (Throwable t) {
            Log.d(TAG, "download: Caught an exception", t);
            long after = System.currentTimeMillis();
            return new SpeedTestResult(totalRead, after - before, SpeedTestStatus.FAILURE);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.w(TAG, "download: Could not close stream", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.w(TAG, "download: Could not close stream", e);
                }
            }
            Log.v(TAG, "download: END");
        }
    }
}
