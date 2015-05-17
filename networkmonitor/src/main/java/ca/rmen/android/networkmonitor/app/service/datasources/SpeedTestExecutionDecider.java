/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2015 Rasmus Holm
 * Copyright (C) 2015 Carmen Alvarez
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

import android.content.Context;
import android.database.Cursor;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestPreferences;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Determines if a speed test should be executed or not.
 */
class SpeedTestExecutionDecider {
    private static final String TAG = Constants.TAG + SpeedTestExecutionDecider.class.getSimpleName();
    private static final int SIGNAL_STRENGTH_VARIATION_THRESHOLD_DBM = 5;
    private static final int SPEED_TEST_INTERVAL_NETWORK_CHANGE = -2;
    private static final int SPEED_TEST_INTERVAL_DBM_OR_NETWORK_CHANGE = -1;

    private final Context mContext;
    private final SpeedTestPreferences mPreferences;

    private final NetMonSignalStrength mNetMonSignalStrength;
    private int mCurrentSignalStrengthDbm;

    // For fetching data regarding the network such as signal strength, network type etc.
    private static TelephonyManager mTelephonyManager;

    // For counting entries since last speedtest was performed
    private int mIntervalCounter;

    SpeedTestExecutionDecider(Context context) {
        Log.v(TAG, "onCreate");
        mContext = context;
        mPreferences = SpeedTestPreferences.getInstance(context);
        mIntervalCounter = 0;
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS | PhoneStateListener.LISTEN_SERVICE_STATE);
        mNetMonSignalStrength = new NetMonSignalStrength(context);
    }

    // Need to make sure we do not listen after we are done
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        if (mTelephonyManager != null)
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    /**
     * @return true if a speed test should be executed.
     */
    public boolean shouldExecute() {
        Log.v(TAG, "shouldExecute() " + mPreferences.getSpeedTestInterval() + " : " + mIntervalCounter);
        int speedTestInterval = mPreferences.getSpeedTestInterval();
        if (speedTestInterval == SPEED_TEST_INTERVAL_NETWORK_CHANGE) {
            // check for change in network
            return hasNetworkTypeChanged();
        } else if (speedTestInterval == SPEED_TEST_INTERVAL_DBM_OR_NETWORK_CHANGE) {
            // check for change in network and for a difference in dbm by 5
            if (hasSignalStrengthChanged() || hasNetworkTypeChanged()) {
                return true;
            }
        } else {
            int mode = Integer.parseInt(mPreferences.getAdvancedSpeedInterval());
            if (mode == SPEED_TEST_INTERVAL_NETWORK_CHANGE) {
                // check for change in network
                return hasNetworkTypeChanged();
            } else if (mode == SPEED_TEST_INTERVAL_DBM_OR_NETWORK_CHANGE) {
                // check for change in network and for a difference in dbm by 5
                if (hasSignalStrengthChanged() || hasNetworkTypeChanged()) {
                    return true;
                }
            } else {
                mIntervalCounter++;
                if (mode <= mIntervalCounter) {
                    mIntervalCounter = 0;
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * @return true if the current network type is different from the network type of the last network monitor test.
     */
    private boolean hasNetworkTypeChanged() {
        int lastLoggedNetworkType = readLastLoggedNetworkType();
        int currentNetworkType = mTelephonyManager.getNetworkType();
        return currentNetworkType != lastLoggedNetworkType;
    }

    /**
     * @return true if the current signal strength has changed significantly compared to the last logged signal strength.
     */
    private boolean hasSignalStrengthChanged() {
        Log.v(TAG, "hasSignalStrengthChanged by: " + SIGNAL_STRENGTH_VARIATION_THRESHOLD_DBM + '?');
        if (mCurrentSignalStrengthDbm != NetMonSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN) {
            int lastLoggedSignalStrength = readLastLoggedSignalStrength();
            if (lastLoggedSignalStrength != NetMonSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN) {
                if (mCurrentSignalStrengthDbm >= lastLoggedSignalStrength + SIGNAL_STRENGTH_VARIATION_THRESHOLD_DBM
                        || mCurrentSignalStrengthDbm <= lastLoggedSignalStrength - SIGNAL_STRENGTH_VARIATION_THRESHOLD_DBM) {
                    Log.v(TAG, "mTelephonyManager has been changed from " + lastLoggedSignalStrength + " to " + mCurrentSignalStrengthDbm);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return the network type we logged to the db in the last network monitor test.
     */
    private int readLastLoggedNetworkType() {
        String[] projection = new String[]{NetMonColumns.NETWORK_TYPE};
        String orderBy = NetMonColumns.TIMESTAMP + " DESC";
        Cursor cursor = mContext.getContentResolver().query(NetMonColumns.CONTENT_URI, projection, null, null, orderBy);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
            } finally {
                cursor.close();
            }
        }
        return TelephonyManager.NETWORK_TYPE_UNKNOWN;
    }

    /**
     * @return the signal strength we logged to the db in the last network monitor test.
     */
    private int readLastLoggedSignalStrength() {
        String[] projection = new String[]{NetMonColumns.CELL_SIGNAL_STRENGTH_DBM};
        String orderBy = NetMonColumns.TIMESTAMP + " DESC";
        Cursor cursor = mContext.getContentResolver().query(NetMonColumns.CONTENT_URI, projection, null, null, orderBy);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
            } finally {
                cursor.close();
            }
        }
        return NetMonSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
    }

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            Log.v(TAG, "onSignalStrengthsChanged: " + signalStrength);
            mCurrentSignalStrengthDbm = mNetMonSignalStrength.getDbm(signalStrength);
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Log.v(TAG, "onServiceStateChanged " + serviceState);
            if (serviceState.getState() != ServiceState.STATE_IN_SERVICE) {
                mCurrentSignalStrengthDbm = NetMonSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
            }
        }
    };

}
