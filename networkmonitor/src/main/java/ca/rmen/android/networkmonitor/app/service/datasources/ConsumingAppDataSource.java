/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2016-2019 Carmen Alvarez (c@rmen.ca)
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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.PermissionUtil;

/**
 * Retrieves the app which has consumed the most data since device boot.
 */
public class ConsumingAppDataSource implements NetMonDataSource {

    private static final String TAG = Constants.TAG + ConsumingAppDataSource.class.getSimpleName();
    private Context mContext;
    private TelephonyManager mTelephonyManager;
    private NetworkStatsManager mNetworkStatsManager;
    private PackageManager mPackageManager;
    private ConnectivityManager mConnectivityManager;
    private NetMonPreferences mPrefs;

    @Override
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        mContext = context;
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mNetworkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        }
        mPackageManager = context.getPackageManager();
        mPrefs = NetMonPreferences.getInstance(context);
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
    }

    @Override
    public ContentValues getContentValues() {
        Log.v(TAG, "getContentValues");
        ContentValues values = new ContentValues(2);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return values;
        }

        if (mPrefs.isFastPollingEnabled()) {
            return values;
        }

        if (!PermissionUtil.hasUsageStatsPermission(mContext)) {
            return values;
        }

        NetworkInfo activeNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo == null) {
            return values;
        }

        List<ApplicationInfo> packages = mPackageManager.getInstalledApplications(
                PackageManager.GET_META_DATA);

        long maxBytes = 0;
        String processName = null;

        int activeNetworkType = activeNetworkInfo.getType();
        for (ApplicationInfo packageInfo : packages) {
            long bytes = getBytesForUid(packageInfo.uid, activeNetworkType);
            if (bytes > maxBytes) {
                maxBytes = bytes;
                processName = packageInfo.processName;
            }
        }

        if (!TextUtils.isEmpty(processName)) {
            values.put(NetMonColumns.MOST_CONSUMING_APP_NAME, processName);
            values.put(NetMonColumns.MOST_CONSUMING_APP_BYTES, maxBytes);
        }
        Log.v(TAG, "getContentValues end");
        return values;
    }

    @SuppressLint({"HardwareIds", "MissingPermission"})
    @TargetApi(Build.VERSION_CODES.M)
    private long getBytesForUid(int uid, int networkType) {
        Log.v(TAG, "getBytesForUid, uid = " + uid + ", networkType = " + networkType);

        try {
            String subscriberId;
            if (PermissionUtil.hasReadPhoneStatePermission(mContext)) {
                subscriberId = mTelephonyManager.getSubscriberId();
            } else {
                subscriberId = "";
            }
            NetworkStats stats = mNetworkStatsManager.queryDetailsForUid(
                    networkType,
                    subscriberId,
                    Long.MIN_VALUE,
                    Long.MAX_VALUE,
                    uid);
            long total = 0;
            if (stats == null) return 0;
            while (stats.hasNextBucket()) {
                NetworkStats.Bucket bucket = new NetworkStats.Bucket();
                stats.getNextBucket(bucket);
                total += bucket.getRxBytes() + bucket.getTxBytes();
            }
            Log.v(TAG, "getBytesForUid, uid = " + uid + ", networkType = " + networkType + ", returning " + total);
            return total;
        }
        // I know it's not good to catch a generic RuntimeException, but I saw some undocumented
        // IllegalArgumentExceptions using the queryDetailsForUid() method.
        catch (RuntimeException e) {
            Log.v(TAG, "Error getting network stats for uid " + uid + ": " + e.getMessage(), e);
            return 0;
        }
    }

}
