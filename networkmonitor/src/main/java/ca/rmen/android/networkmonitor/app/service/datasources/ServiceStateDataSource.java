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
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.AndroidConstantsUtil;

/**
 * Retrieves attributes of the service state.
 */
public class ServiceStateDataSource implements NetMonDataSource {

    private static final String TAG = Constants.TAG + ServiceStateDataSource.class.getSimpleName();
    private TelephonyManager mTelephonyManager;
    private volatile ServiceState mLastServiceState;

    @Override
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        }
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        if (mTelephonyManager != null)
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    @Override
    public ContentValues getContentValues() {
        Log.v(TAG, "getContentValues");
        ContentValues values = new ContentValues(1);
        if (mLastServiceState == null)
            return values;
        String lastServiceState = AndroidConstantsUtil.getConstantName(ServiceState.class, "STATE", null, mLastServiceState.getState());
        values.put(NetMonColumns.SERVICE_STATE, lastServiceState);
        return values;
    }

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Log.v(TAG, "onServiceStateChanged " + serviceState);
            mLastServiceState = serviceState;
        }
    };

}
