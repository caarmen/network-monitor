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

public class SpeedTestResult {
    public enum SpeedTestStatus {
        SUCCESS, INVALID_FILE, FAILURE, AUTH_FAILURE, UNKNOWN
    }

    public final int bytes;
    public final long transferTime;
    public final SpeedTestStatus status;

    public SpeedTestResult(int bytes, long transferTime, SpeedTestStatus status) {
        this.bytes = bytes;
        this.transferTime = transferTime;
        this.status = status;
    }

    public float getSpeedMbps() {
        return bytes * 8000 / transferTime;
    }

    @Override
    public String toString() {
        return SpeedTestResult.class.getSimpleName() + "[bytes=" + bytes + ", transferTime=" + transferTime + ", status=" + status + "]";
    }

}