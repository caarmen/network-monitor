/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2019 Carmen Alvarez (c@rmen.ca)
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

import androidx.annotation.NonNull;

public class SpeedTestResult {
    public enum SpeedTestStatus {
        SUCCESS, INVALID_FILE, FAILURE, AUTH_FAILURE, UNKNOWN
    }

    final long fileBytes;
    public final SpeedTestStatus status;
    public final long totalBytes;
    public final long transferTime;

    /**
     * @param totalBytes the total bytes transferred (either received or sent) during the time the file was being transferred.
     * @param fileBytes the size of the file which was transferred
     * @param transferTime the time in milliseconds it took to transfer the file
     * @param status the result of the file transfer
     */
    SpeedTestResult(long totalBytes, long fileBytes, long transferTime, SpeedTestStatus status) {
        this.totalBytes = totalBytes;
        this.fileBytes = fileBytes;
        this.transferTime = transferTime;
        this.status = status;
    }

    /**
     * @return the transfer speed in megabits per second.
     */
    public float getSpeedMbps() {
        long bytesTransferred = totalBytes > 0 ? totalBytes : fileBytes;
        float seconds = (float) transferTime / 1000;
        long bits = bytesTransferred * 8;
        float megabits = (float) bits / 1000000;
        return megabits / seconds;
    }

    @Override
    @NonNull
    public String toString() {
        return SpeedTestResult.class.getSimpleName() + "[totalBytes=" + totalBytes + ", fileBytes=" + fileBytes + ", transferTime=" + transferTime
                + ", status=" + status + "]";
    }


}