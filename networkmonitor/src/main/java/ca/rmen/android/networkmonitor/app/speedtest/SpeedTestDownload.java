/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2015 Carmen Alvarez (c@rmen.ca)
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

import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestResult.SpeedTestStatus;


/**
 * Downloads a file and calculates the download speed.
 */
public class SpeedTestDownload {
    private static final String TAG = Constants.TAG + SpeedTestDownload.class.getSimpleName();

    // The maximum connection and read timeout 
    private static final int TIMEOUT = 5000;
    private static final String PREF_SPEED_TEST_LAST_DOWNLOAD_RESULT = "PREF_SPEED_TEST_LAST_DOWNLOAD_RESULT";

    interface SpeedTestDownloadCallback {
        void onSpeedTestResult(@NonNull SpeedTestResult result);
    }

    public static void download(SpeedTestDownloadConfig config, SpeedTestDownloadCallback callback) {
        new DownloadAsyncTask(callback).execute(config);
    }

    private static class DownloadAsyncTask extends AsyncTask<SpeedTestDownloadConfig, Void, SpeedTestResult> {

        private final SpeedTestDownloadCallback mCallback;

        DownloadAsyncTask(SpeedTestDownloadCallback callback) {
            mCallback = callback;
        }

        @Override
        protected SpeedTestResult doInBackground(SpeedTestDownloadConfig... speedTestDownloadConfigs) {
            return download(speedTestDownloadConfigs[0]);
        }

        @Override
        protected void onPostExecute(@NonNull SpeedTestResult speedTestResult) {
            mCallback.onSpeedTestResult(speedTestResult);
        }
    }

    @NonNull
    public static SpeedTestResult download(SpeedTestDownloadConfig config) {
        Log.v(TAG, "download " + config);
        URL url;
        try {
            url = new URL(config.url);
        } catch (MalformedURLException e) {
            Log.e(TAG, "download: incorrect url " + config.url, e);
            return new SpeedTestResult(0, 0, 0, SpeedTestStatus.INVALID_FILE);
        }

        SpeedTestStatus status = SpeedTestStatus.UNKNOWN;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        long totalRead = 0;
        long before = System.currentTimeMillis();
        long rxBytesBefore = TrafficStats.getTotalRxBytes();
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
            byte[] buffer = new byte[1048576];
            int read;
            do {
                read = inputStream.read(buffer);
                if (read > 0) outputStream.write(buffer, 0, read);
                totalRead += read;

            } while (read > 0);
            if (totalRead > 0) status = SpeedTestStatus.SUCCESS;
            long after = System.currentTimeMillis();
            long rxBytesAfter = TrafficStats.getTotalRxBytes();
            SpeedTestResult result = new SpeedTestResult(rxBytesAfter - rxBytesBefore, totalRead, after - before, status);
            Log.v(TAG, "success: " + result);
            return result;
        } catch (Throwable t) {
            Log.d(TAG, "download: Caught an exception", t);
            long after = System.currentTimeMillis();
            return new SpeedTestResult(0, totalRead, after - before, SpeedTestStatus.FAILURE);
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

    /**
     * Persist this speed test result to the shared preferences.
     *
     * @param result the speed test result to save.
     */
    static void save(SharedPreferences prefs, SpeedTestResult result) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(PREF_SPEED_TEST_LAST_DOWNLOAD_RESULT + "_TOTAL_BYTES", result.totalBytes);
        editor.putLong(PREF_SPEED_TEST_LAST_DOWNLOAD_RESULT + "_FILE_BYTES", result.fileBytes);
        editor.putLong(PREF_SPEED_TEST_LAST_DOWNLOAD_RESULT + "_TRANSFER_TIME", result.transferTime);
        editor.putInt(PREF_SPEED_TEST_LAST_DOWNLOAD_RESULT + "_STATUS", result.status.ordinal());
        editor.apply();
    }

    /**
     * @return a speed test result which was stored in the shared preferences.
     */
    static SpeedTestResult read(SharedPreferences prefs) {
        long totalBytes = prefs.getLong(PREF_SPEED_TEST_LAST_DOWNLOAD_RESULT + "_TOTAL_BYTES", 0);
        long fileBytes = prefs.getLong(PREF_SPEED_TEST_LAST_DOWNLOAD_RESULT + "_FILE_BYTES", 0);
        long transferTime = prefs.getLong(PREF_SPEED_TEST_LAST_DOWNLOAD_RESULT + "_TRANSFER_TIME", 0);
        int statusInt = prefs.getInt(PREF_SPEED_TEST_LAST_DOWNLOAD_RESULT + "_STATUS", SpeedTestStatus.UNKNOWN.ordinal());
        return new SpeedTestResult(totalBytes, fileBytes, transferTime, SpeedTestStatus.values()[statusInt]);
    }

}
