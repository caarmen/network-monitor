/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014 Benoit 'BoD' Lubek (BoD@JRAF.org) //TODO <- replace with *your* name/email
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
package org.jraf.android.networkmonitor.app.service.datasources;

import android.content.ContentValues;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.provider.NetMonColumns;
import org.jraf.android.networkmonitor.util.TelephonyUtil;

class SIMDataSource implements NetMonDataSource {

    private static final String TAG = Constants.TAG + SIMDataSource.class.getSimpleName();
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
        values.put(NetMonColumns.SIM_OPERATOR, mTelephonyManager.getSimOperatorName());
        String[] simMccMnc = TelephonyUtil.getMccMnc(mTelephonyManager.getSimOperator());
        values.put(NetMonColumns.SIM_MCC, simMccMnc[0]);
        values.put(NetMonColumns.SIM_MNC, simMccMnc[1]);
        values.put(NetMonColumns.NETWORK_OPERATOR, mTelephonyManager.getNetworkOperatorName());
        String[] networkMccMnc = TelephonyUtil.getMccMnc(mTelephonyManager.getNetworkOperator());
        values.put(NetMonColumns.NETWORK_MCC, networkMccMnc[0]);
        values.put(NetMonColumns.NETWORK_MNC, networkMccMnc[1]);
        int simState = mTelephonyManager.getSimState();
        values.put(NetMonColumns.SIM_STATE, TelephonyUtil.getConstantName("SIM_STATE", null, simState));
        return values;
    }

}
