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
 * Copyright (C) 2015-2019 Carmen Alvarez
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

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.NetMonSignalStrength;
import ca.rmen.android.networkmonitor.util.TelephonyUtil;

/**
 * Determines if a speed test should be executed or not.
 */
public class SpeedTestExecutionDecider {
    private static final String TAG = Constants.TAG + SpeedTestExecutionDecider.class.getSimpleName();
    private static final int SIGNAL_STRENGTH_VARIATION_THRESHOLD_DBM = 5;

    private final Context mContext;
    private final SpeedTestPreferences mPreferences;

    private final NetMonSignalStrength mNetMonSignalStrength;
    private int mCurrentCellSignalStrengthDbm;

    // For fetching data regarding the network such as signal strength, network type etc.
    private final TelephonyManager mTelephonyManager;
    private final WifiManager mWifiManager;
    private final ConnectivityManager mConnectivityManager;

    // Using this selection in a query will make the query only return results in which a speed test was performed.
    private static final String QUERY_FILTER_HAS_SPEED_TEST = "CAST(" + NetMonColumns.DOWNLOAD_SPEED + " AS REAL) > 0 OR CAST(" + NetMonColumns.UPLOAD_SPEED + " AS REAL) > 0";

    public SpeedTestExecutionDecider(Context context) {
        Log.v(TAG, "onCreate");
        mContext = context;
        mPreferences = SpeedTestPreferences.getInstance(context);
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mNetMonSignalStrength = new NetMonSignalStrength(context);
        if (mPreferences.isEnabled() && mPreferences.getSpeedTestInterval() == SpeedTestPreferences.PREF_SPEED_TEST_INTERVAL_DBM_OR_NETWORK_CHANGE) {
            registerPhoneStateListener();
        }
        PreferenceManager.getDefaultSharedPreferences(mContext).registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
    }

    // Need to make sure we do not listen after we are done
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        unregisterPhoneStateListener();
        PreferenceManager.getDefaultSharedPreferences(mContext).unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
    }

    /**
     * @return true if a speed test should be executed.
     */
    public boolean shouldExecute() {
        Log.v(TAG, "shouldExecute");

        if (!mPreferences.isEnabled()) return false;

        int speedTestInterval = mPreferences.getSpeedTestInterval();
        if (speedTestInterval == SpeedTestPreferences.PREF_SPEED_TEST_INTERVAL_NETWORK_CHANGE) {
            // check for change in network
            return hasNetworkTypeChanged();
        } else if (speedTestInterval == SpeedTestPreferences.PREF_SPEED_TEST_INTERVAL_DBM_OR_NETWORK_CHANGE) {
            // check for change in network and for a difference in dbm by 5
            return hasSignalStrengthChanged() || hasNetworkTypeChanged();
        } else {
            return isIntervalExceeded();
        }
    }

    /**
     * @return true if the current network type is different from the network type during the last speed test.
     */
    private boolean hasNetworkTypeChanged() {
        String lastLoggedNetworkType = readLastLoggedValue(NetMonColumns.NETWORK_TYPE);
        String currentNetworkType = TelephonyUtil.getNetworkType(mContext);

        if (currentNetworkType == null) return false;

        Log.v(TAG, "hasNetworkTypeChanged: from " + lastLoggedNetworkType + " to " + currentNetworkType);
        return !currentNetworkType.equals(lastLoggedNetworkType);
    }

    /**
     * @return true if the current wifi or mobile signal strength in dbm has changed significantly compared to the value during the last speed test.
     * Only checks wifi signal strength if we are on wifi, and cell signal strength, if we are on mobile data.
     */
    private boolean hasSignalStrengthChanged() {
        NetworkInfo activeNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo == null) return false;
        if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return hasWifiSignalStrengthChanged();
        } else {
            return hasCellSignalStrengthChanged();
        }
    }

    /**
     * @return true if the current cell signal strength has changed significantly compared to the cell signal strength during the last speed test.
     */
    private boolean hasCellSignalStrengthChanged() {
        Log.v(TAG, "hasCellSignalStrengthChanged by: " + SIGNAL_STRENGTH_VARIATION_THRESHOLD_DBM + '?');
        String lastLoggedCellSignalStrength = readLastLoggedValue(NetMonColumns.CELL_SIGNAL_STRENGTH_DBM);
        return lastLoggedCellSignalStrength != null &&
                signalStrengthChangeExceedsThreshold(Integer.valueOf(lastLoggedCellSignalStrength), mCurrentCellSignalStrengthDbm);
    }

    /**
     * @return true if the current wifi signal strength has changed significantly compared to the wifi signal strength during the last speed test.
     */
    private boolean hasWifiSignalStrengthChanged() {
        Log.v(TAG, "hasWifiSignalStrengthChanged by: " + SIGNAL_STRENGTH_VARIATION_THRESHOLD_DBM + '?');
        WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
        int currentWifiSignalStrengthDbm = connectionInfo.getRssi();
        String lastLoggedWifiSignalStrength = readLastLoggedValue(NetMonColumns.WIFI_RSSI);
        return lastLoggedWifiSignalStrength != null &&
                signalStrengthChangeExceedsThreshold(Integer.valueOf(lastLoggedWifiSignalStrength), currentWifiSignalStrengthDbm);
    }

    /**
     * @return true if the difference between the two values exceeds the threshold defined by #SIGNAL_STRENGTH_VARIATION_THRESHOLD_DBM.
     */
    private static boolean signalStrengthChangeExceedsThreshold(int previousValue, int currentValue) {
        Log.v(TAG, "signal strength has been changed from " + previousValue + " to " + currentValue);
        if (previousValue != NetMonSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN) {
            if (currentValue != NetMonSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN) {
                return currentValue >= previousValue + SIGNAL_STRENGTH_VARIATION_THRESHOLD_DBM
                        || currentValue <= previousValue - SIGNAL_STRENGTH_VARIATION_THRESHOLD_DBM;
            }
        }
        return false;
    }

    /**
     * @return true if enough network monitor tests, without speed tests, have been logged.
     */
    private boolean isIntervalExceeded() {
        int numberOfRecordsSinceLastSpeedTest = readNumberOfRecordsSinceLastSpeedTest();
        Log.v(TAG, "isIntervalExceeded: numberOfRecordsSinceLastSpeedTest: " + numberOfRecordsSinceLastSpeedTest
                + " vs speed test interval: " + mPreferences.getSpeedTestInterval());
        return (numberOfRecordsSinceLastSpeedTest < 0)
                || (numberOfRecordsSinceLastSpeedTest >= mPreferences.getSpeedTestInterval() - 1);
    }

    private int readNumberOfRecordsSinceLastSpeedTest() {
        String idOfLatestSpeedTest = readLastLoggedValue(BaseColumns._ID);
        if (idOfLatestSpeedTest == null) return 0;
        String orderBy = BaseColumns._ID + " DESC";
        String selection = BaseColumns._ID + " > " + idOfLatestSpeedTest;
        Cursor cursor = mContext.getContentResolver().query(NetMonColumns.CONTENT_URI, null, selection, null, orderBy);
        if (cursor != null) {
            try {
                return cursor.getCount();
            } finally {
                cursor.close();
            }
        }
        return 0;
    }

    private void registerPhoneStateListener() {
        Log.v(TAG, "registerPhoneStateListener");
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS | PhoneStateListener.LISTEN_SERVICE_STATE);
    }

    private void unregisterPhoneStateListener() {
        Log.v(TAG, "unregisterPhoneStateListener");
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    /**
     * @return the most recent value we logged for the given columnName, which matches the given selection.  May return null.
     */
    private String readLastLoggedValue(String columnName) {
        String[] projection = new String[]{columnName};
        String orderBy = BaseColumns._ID + " DESC";
        Cursor cursor = mContext.getContentResolver().query(NetMonColumns.CONTENT_URI, projection, QUERY_FILTER_HAS_SPEED_TEST, null, orderBy);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            Log.v(TAG, "onSignalStrengthsChanged: " + signalStrength);
            mCurrentCellSignalStrengthDbm = mNetMonSignalStrength.getDbm(signalStrength);
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Log.v(TAG, "onServiceStateChanged " + serviceState);
            if (serviceState.getState() != ServiceState.STATE_IN_SERVICE) {
                mCurrentCellSignalStrengthDbm = NetMonSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
            }
        }
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (SpeedTestPreferences.PREF_SPEED_TEST_ENABLED.equals(key) || SpeedTestPreferences.PREF_SPEED_TEST_INTERVAL.equals(key)) {
                if (mPreferences.isEnabled() && mPreferences.getSpeedTestInterval() == SpeedTestPreferences.PREF_SPEED_TEST_INTERVAL_DBM_OR_NETWORK_CHANGE) {
                    registerPhoneStateListener();
                } else {
                    unregisterPhoneStateListener();
                }
            }
        }
    };
}

