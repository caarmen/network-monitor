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
import android.telephony.TelephonyManager;

import ca.rmen.android.networkmonitor.util.AndroidConstantsUtil;
import android.util.Log;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;

/**
 * Retrieves attributes of the mobile data connection.
 */
public class MobileDataConnectionDataSource implements NetMonDataSource {

    private static final String TAG = Constants.TAG + MobileDataConnectionDataSource.class.getSimpleName();
    private TelephonyManager mTelephonyManager;

    @Override
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public void onDestroy() {}

    @Override
    public ContentValues getContentValues() {
        Log.v(TAG, "getContentValues");
        ContentValues values = new ContentValues(3);
        values.put(NetMonColumns.MOBILE_DATA_NETWORK_TYPE, AndroidConstantsUtil.getConstantName(TelephonyManager.class, "NETWORK_TYPE", null, mTelephonyManager.getNetworkType()));
        values.put(NetMonColumns.DATA_ACTIVITY, AndroidConstantsUtil.getConstantName(TelephonyManager.class, "DATA_ACTIVITY", null, mTelephonyManager.getDataActivity()));
        values.put(NetMonColumns.DATA_STATE, AndroidConstantsUtil.getConstantName(TelephonyManager.class, "DATA", "DATA_ACTIVITY", mTelephonyManager.getDataState()));
        return values;
    }

}
