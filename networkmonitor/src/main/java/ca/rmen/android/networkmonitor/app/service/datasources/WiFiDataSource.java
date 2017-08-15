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

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.SparseIntArray;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import android.util.Log;
import ca.rmen.android.networkmonitor.util.PermissionUtil;

/**
 * retrieves the SSID, BSSID, signal strength, and RSSI of the currently connected WiFi network, if any.
 */
public class WiFiDataSource implements NetMonDataSource {

    private static final String TAG = Constants.TAG + WiFiDataSource.class.getSimpleName();
    private static final SparseIntArray CHANNEL_FREQUENCIES = new SparseIntArray(14);
    private WifiManager mWifiManager;
    private Context mContext;

    static {
        CHANNEL_FREQUENCIES.append(2412, 1);
        CHANNEL_FREQUENCIES.append(2417, 2);
        CHANNEL_FREQUENCIES.append(2422, 3);
        CHANNEL_FREQUENCIES.append(2427, 4);
        CHANNEL_FREQUENCIES.append(2432, 5);
        CHANNEL_FREQUENCIES.append(2437, 6);
        CHANNEL_FREQUENCIES.append(2442, 7);
        CHANNEL_FREQUENCIES.append(2447, 8);
        CHANNEL_FREQUENCIES.append(2452, 9);
        CHANNEL_FREQUENCIES.append(2457, 10);
        CHANNEL_FREQUENCIES.append(2462, 11);
        CHANNEL_FREQUENCIES.append(2467, 12);
        CHANNEL_FREQUENCIES.append(2472, 13);
        CHANNEL_FREQUENCIES.append(2484, 14);
    }

    @Override
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        mContext = context;
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public void onDestroy() {}

    @Override
    public ContentValues getContentValues() {
        Log.v(TAG, "getContentValues");
        WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
        ContentValues result = new ContentValues(2);
        if (connectionInfo == null || connectionInfo.getNetworkId() < 0) return result;
        result.put(NetMonColumns.WIFI_SSID, connectionInfo.getSSID());
        result.put(NetMonColumns.WIFI_BSSID, connectionInfo.getBSSID());
        int signalLevel = WifiManager.calculateSignalLevel(connectionInfo.getRssi(), 5);
        result.put(NetMonColumns.WIFI_SIGNAL_STRENGTH, signalLevel);
        result.put(NetMonColumns.WIFI_RSSI, connectionInfo.getRssi());

        if (PermissionUtil.hasLocationPermission(mContext)) {
            List<ScanResult> scanResults = mWifiManager.getScanResults();
            if (scanResults != null) {
                for (ScanResult scanResult : scanResults) {
                    if (scanResult.BSSID != null && scanResult.BSSID.equals(connectionInfo.getBSSID())) {
                        int channel = CHANNEL_FREQUENCIES.get(scanResult.frequency);
                        result.put(NetMonColumns.WIFI_FREQUENCY, scanResult.frequency);
                        result.put(NetMonColumns.WIFI_CHANNEL, channel);
                        break;
                    }
                }
            }
        }

        return result;
    }
}
