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

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.TelephonyUtil;

/**
 * Retrieves information from the currently active {@link NetworkInfo}.
 */
public class ActiveNetworkInfoDataSource implements NetMonDataSource {
    private static final String TAG = Constants.TAG + ActiveNetworkInfoDataSource.class.getSimpleName();
    private Context mContext;
    private ConnectivityManager mConnectivityManager;

    @Override
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        mContext = context;
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public void onDestroy() {}

    @Override
    public ContentValues getContentValues() {
        Log.v(TAG, "getContentValues");
        ContentValues values = new ContentValues();

        NetworkInfo activeNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo == null) return values;
        String networkType = TelephonyUtil.getNetworkType(mContext);
        values.put(NetMonColumns.NETWORK_TYPE, networkType);
        values.put(NetMonColumns.IS_ROAMING, activeNetworkInfo.isRoaming());
        values.put(NetMonColumns.IS_AVAILABLE, activeNetworkInfo.isAvailable());
        values.put(NetMonColumns.IS_CONNECTED, activeNetworkInfo.isConnected());
        values.put(NetMonColumns.IS_FAILOVER, activeNetworkInfo.isFailover());
        values.put(NetMonColumns.DETAILED_STATE, activeNetworkInfo.getDetailedState().toString());
        values.put(NetMonColumns.REASON, activeNetworkInfo.getReason());
        values.put(NetMonColumns.EXTRA_INFO, activeNetworkInfo.getExtraInfo());
        if (Build.VERSION.SDK_INT >= 16) values.put(NetMonColumns.IS_NETWORK_METERED, isActiveNetworkMetered());
        return values;
    }

    @TargetApi(16)
    private boolean isActiveNetworkMetered() {
        return mConnectivityManager.isActiveNetworkMetered();
    }
}
