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
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

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

    /**
     * Advanced options below for speedtest
     */
    private NetMonSignalStrength mNetMonSignalStrength;
    private int mLastSignalStrengthDbm;

    // For fetching data regarding the network such as signal strength, network type etc.
    private static TelephonyManager mTelephonyManager;

    // For finding changes in the signal strength
    private static int mOldSignalStrength;
    private static int mDifference;

    // For finding changes in the network
    private static int mNetworkType;

    // For counting entries since last speedtest was performed
    private static int mIntervalCounter;

    @Override
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        mPreferences = SpeedTestPreferences.getInstance(context);
        mDisabledValue = context.getString(R.string.speed_test_disabled);
        // advanced settings below
        mIntervalCounter = 0;
        mDifference = 5;
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS | PhoneStateListener.LISTEN_SERVICE_STATE);
        mNetMonSignalStrength = new NetMonSignalStrength(context);
        // Some value that never should be possible so we always updates the first run
        mOldSignalStrength = 255;
        mNetworkType = 255;
    }

    // Need to make sure we do not listen after we are done
    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        if (mTelephonyManager != null) mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

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
                    return changedNetwork();
                case -1:// check for change in network and for a difference in dbm by 5
                    if (changedDbm() || changedNetwork()){
                        return true;
                    }
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

    private boolean changedNetwork() {
        if (mTelephonyManager.getNetworkType() != mNetworkType ){
            mNetworkType = mTelephonyManager.getNetworkType();
            return true;
        }
        return false;
    }

    private boolean changedDbm() {
        Log.v(TAG, "changedDbm by: " + mDifference + '?');
        if (mLastSignalStrengthDbm != NetMonSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN) {
            if (mLastSignalStrengthDbm >= mOldSignalStrength + mDifference || mLastSignalStrengthDbm <= mOldSignalStrength - mDifference ) {
                Log.v(TAG,"mOldSignalStrength has been changed from " + mOldSignalStrength + " to " + mLastSignalStrengthDbm);
                mOldSignalStrength = mLastSignalStrengthDbm;
                return true;
            }
        }
        return false;
    }

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            Log.v(TAG, "onSignalStrengthsChanged: " + signalStrength);
            mLastSignalStrengthDbm = mNetMonSignalStrength.getDbm(signalStrength);
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Log.v(TAG, "onServiceStateChanged " + serviceState);
            if (serviceState.getState() != ServiceState.STATE_IN_SERVICE) {
                mLastSignalStrengthDbm = NetMonSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
            }
        }
    };
}
