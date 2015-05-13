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
package ca.rmen.android.networkmonitor.app.service.datasources;

import android.content.ContentValues;
import android.content.Context;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestDownload;
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestDownloadConfig;
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestPreferences;
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestResult;
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestResult.SpeedTestStatus;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Tests download speed by downloading a file.
 */
public class DownloadSpeedTestDataSource implements NetMonDataSource {
    private static final String TAG = Constants.TAG + DownloadSpeedTestDataSource.class.getSimpleName();

    private SpeedTestPreferences mPreferences;
    private String mDisabledValue;
    private int mIntervalCounter;

    @Override
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        mPreferences = SpeedTestPreferences.getInstance(context);
        mDisabledValue = context.getString(R.string.speed_test_disabled);
        mIntervalCounter = 0;
    }

    @Override
    public void onDestroy() {}

    @Override
    public ContentValues getContentValues() {
        Log.v(TAG, "getContentValues");
        ContentValues values = new ContentValues();

        if (mPreferences.isEnabled() && doUpdate()) {
            SpeedTestDownloadConfig downloadConfig = mPreferences.getDownloadConfig();
            if (!downloadConfig.isValid()) return values;
            SpeedTestResult result = SpeedTestDownload.download(downloadConfig);
            mPreferences.setLastDownloadResult(result);
            if (result.status == SpeedTestStatus.SUCCESS) values.put(NetMonColumns.DOWNLOAD_SPEED, String.format("%.3f", result.getSpeedMbps()));
        } else {
            values.put(NetMonColumns.DOWNLOAD_SPEED, mDisabledValue);
        }
        return values;
    }
    // Determines if the function should do a download test this call
    private boolean doUpdate() {
        Log.v(TAG, "doUpdate() " + mPreferences.getAdvancedSpeedInterval() + " : " + mIntervalCounter );
        if (!mPreferences.isAdvancedEnabled()) {
            return true;
        }
        else {
            // Switch case since I have different types of modes for the speed interval
            int mode = Integer.parseInt(mPreferences.getAdvancedSpeedInterval());
            switch (mode){
                case -2: // check for change in network
                    break;
                case -1: // check for change in network and for a difference in dbm by 5
                    break;
                case 2:
                    mIntervalCounter++;
                    if (2 <= mIntervalCounter) {
                        mIntervalCounter=0;
                        return true;
                    }
                    break;
                case 5:
                    mIntervalCounter++;
                    if (5 <= mIntervalCounter) {
                        mIntervalCounter=0;
                        return true;
                    }
                    break;
                case 10:
                    mIntervalCounter++;
                    if (10 <= mIntervalCounter) {
                        mIntervalCounter=0;
                        return true;
                    }
                    break;
                case 20:
                    mIntervalCounter++;
                    if (20 <= mIntervalCounter) {
                        mIntervalCounter=0;
                        return true;
                    }
                    break;
                case 30:
                    mIntervalCounter++;
                    if (30 <= mIntervalCounter) {
                        mIntervalCounter=0;
                        return true;
                    }
                    break;
                case 60:
                    mIntervalCounter++;
                    if (60 <= mIntervalCounter) {
                        mIntervalCounter=0;
                        return true;
                    }
                    break;
                case 100:
                    mIntervalCounter++;
                    if (100 <= mIntervalCounter) {
                        mIntervalCounter=0;
                        return true;
                    }
                    break;
                case 1000:
                    mIntervalCounter++;
                    if (1000 <= mIntervalCounter) {
                        mIntervalCounter=0;
                        return true;
                    }
                    break;
                default:
                    return false;
            }
            return false;
        }
    }
}
