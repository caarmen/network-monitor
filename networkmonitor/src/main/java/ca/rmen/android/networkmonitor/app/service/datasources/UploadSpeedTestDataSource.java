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
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestPreferences;
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestResult;
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestResult.SpeedTestStatus;
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestUpload;
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestUploadConfig;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Tests upload speed by uploading a file.
 */
public class UploadSpeedTestDataSource implements NetMonDataSource {
    private static final String TAG = Constants.TAG + UploadSpeedTestDataSource.class.getSimpleName();

    private SpeedTestPreferences mPreferences;
    private String mDisabledValue;

    private SpeedTestAdvancedInterval mAdvancedInterval;

    @Override
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        mPreferences = SpeedTestPreferences.getInstance(context);
        mDisabledValue = context.getString(R.string.speed_test_disabled);
        mAdvancedInterval = new SpeedTestAdvancedInterval(context);
    }

    @Override
    public void onDestroy() {}

    @Override
    public ContentValues getContentValues() {
        Log.v(TAG, "getContentValues");
        ContentValues values = new ContentValues();

        if (mPreferences.isEnabled() && mAdvancedInterval.doUpdate()) {
            SpeedTestUploadConfig uploadConfig = mPreferences.getUploadConfig();
            if (!uploadConfig.isValid()) return values;
            SpeedTestResult result = SpeedTestUpload.upload(uploadConfig);
            if (result.status == SpeedTestStatus.SUCCESS) values.put(NetMonColumns.UPLOAD_SPEED, String.format("%.3f", result.getSpeedMbps()));
        } else {
            values.put(NetMonColumns.UPLOAD_SPEED, mDisabledValue);
        }
        return values;
    }
}
