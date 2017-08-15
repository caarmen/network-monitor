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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;

import android.net.TrafficStats;
import android.text.TextUtils;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTP;

import ca.rmen.android.networkmonitor.BuildConfig;
import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestResult.SpeedTestStatus;
import ca.rmen.android.networkmonitor.util.IoUtil;
import android.util.Log;


/**
 * Uploads a file and calculates the upload speed.
 */
public class SpeedTestUpload {
    private static final String TAG = Constants.TAG + SpeedTestUpload.class.getSimpleName();

    private static final int TIMEOUT = 5000;

    public static SpeedTestResult upload(SpeedTestUploadConfig uploadConfig) {
        Log.v(TAG, "upload " + uploadConfig);
        // Make sure we have a file to upload
        if (!uploadConfig.file.exists()) return new SpeedTestResult(0, 0, 0, SpeedTestStatus.INVALID_FILE);

        FTPClient ftp = new FTPClient();
        ftp.setConnectTimeout(TIMEOUT);
        ftp.setDataTimeout(TIMEOUT);
        ftp.setDefaultTimeout(TIMEOUT);
        // For debugging, we'll log all the ftp commands
        if (BuildConfig.DEBUG) {
            PrintCommandListener printCommandListener = new PrintCommandListener(System.out);
            ftp.addProtocolCommandListener(printCommandListener);
        }
        InputStream is = null;
        try {
            // Set buffer size of FTP client
            ftp.setBufferSize(1048576);
            // Open a connection to the FTP server
            ftp.connect(uploadConfig.server, uploadConfig.port);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                return new SpeedTestResult(0, 0, 0, SpeedTestStatus.FAILURE);
            }
            // Login to the FTP server
            if (!ftp.login(uploadConfig.user, uploadConfig.password)) {
                ftp.disconnect();
                return new SpeedTestResult(0, 0, 0, SpeedTestStatus.AUTH_FAILURE);
            }
            // Try to change directories
            if (!TextUtils.isEmpty(uploadConfig.path) && !ftp.changeWorkingDirectory(uploadConfig.path)) {
                Log.v(TAG, "Upload: could not change path to " + uploadConfig.path);
                return new SpeedTestResult(0, 0, 0, SpeedTestStatus.INVALID_FILE);
            }

            // set the file type to be read as a binary file
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            ftp.enterLocalPassiveMode();

            // Upload the file
            is = new FileInputStream(uploadConfig.file);
            long before = System.currentTimeMillis();
            long txBytesBefore = TrafficStats.getTotalTxBytes();
            if (!ftp.storeFile(uploadConfig.file.getName(), is)) {
                ftp.disconnect();
                Log.v(TAG,
                        "Upload: could not store file to " + uploadConfig.path + ". Error code: " + ftp.getReplyCode() + ", error string: "
                                + ftp.getReplyString());
                return new SpeedTestResult(0, 0, 0, SpeedTestStatus.FAILURE);
            }

            // Calculate stats
            long after = System.currentTimeMillis();
            long txBytesAfter = TrafficStats.getTotalTxBytes();
            ftp.logout();
            ftp.disconnect();
            Log.v(TAG, "Upload complete");
            return new SpeedTestResult(txBytesAfter - txBytesBefore, uploadConfig.file.length(), after - before, SpeedTestStatus.SUCCESS);
        } catch (SocketException e) {
            Log.e(TAG, "upload " + e.getMessage(), e);
            return new SpeedTestResult(0, 0, 0, SpeedTestStatus.FAILURE);
        } catch (IOException e) {
            Log.e(TAG, "upload " + e.getMessage(), e);
            return new SpeedTestResult(0, 0, 0, SpeedTestStatus.FAILURE);
        } finally {
            IoUtil.closeSilently(is);
        }
    }
}
