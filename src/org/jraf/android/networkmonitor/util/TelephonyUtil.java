/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013 Carmen Alvarez (c@rmen.ca)
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
package org.jraf.android.networkmonitor.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.Log;

public class TelephonyUtil {
    private static final String TAG = TelephonyUtil.class.getSimpleName();

    private static final Map<String, String> sConstantsCache = new HashMap<String, String>();

    /**
     * Workaround to return {@link TelephonyManager#PHONE_TYPE_GSM} or {@link TelephonyManager#PHONE_TYPE_CDMA} for tablets which have a radio for data.
     */
    public static int getDeviceType(Context context) {

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int phoneType = telephonyManager.getPhoneType();

        if (phoneType == TelephonyManager.PHONE_TYPE_GSM || telephonyManager.getCellLocation() instanceof GsmCellLocation) return TelephonyManager.PHONE_TYPE_GSM;
        else if (phoneType == TelephonyManager.PHONE_TYPE_CDMA || telephonyManager.getCellLocation() instanceof CdmaCellLocation) return TelephonyManager.PHONE_TYPE_CDMA;
        else
            return phoneType;
    }

    /**
     * Returns a TelephonyManager int constant as a string. For example, for {@link TelephonyManager#DATA_CONNECTED}, this returns the string "CONNECTED".
     * 
     * @param fieldPrefix the prefix of the TelephonyManager field name. For example, for {@link TelephonyManager#DATA_CONNECTED}, this should be "DATA"
     * @param excludePrefix in most cases this can be null. However, in the case of {@link TelephonyManager#DATA_CONNECTED}, we need to set exclude prefix to
     *            "DATA_ACTIVITY" to make sure we don't return OUT, as DATA_ACTIVITY_OUT has the same value as DATA_CONNECTED.
     * @param value the value of the constant.
     * @return the name of the constant having the given fieldPrefix, not having the given excludePrefix, and having the given value.
     */
    public static String getConstantName(String fieldPrefix, String excludePrefix, int value) {
        Log.v(TAG, "getConstantName: fieldPrefix=" + fieldPrefix + ", excludePrefix=" + excludePrefix + ", value=" + value);
        final String key = fieldPrefix + ":" + value;
        String result = sConstantsCache.get(key);
        if (result != null) {
            Log.v(TAG, "Found " + key + "=" + result + " in the cache");
            return result;
        }
        Field[] fields = TelephonyManager.class.getFields();
        for (Field field : fields) {
            try {
                String fieldName = field.getName();
                if (fieldName.startsWith(fieldPrefix)) {
                    if (!TextUtils.isEmpty(excludePrefix) && fieldName.startsWith(excludePrefix)) continue;
                    if (field.getInt(null) == value) {
                        result = field.getName().substring(fieldPrefix.length() + 1);
                        Log.v(TAG, "Adding " + key + "=" + result + " to the cache");
                        sConstantsCache.put(key, result);
                        return result;
                    }
                }
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "getConstantName Could not get constant name for prefix = " + fieldPrefix + " and value = " + value, e);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "getConstantName Could not get constant name for prefix = " + fieldPrefix + " and value = " + value, e);
            }
        }
        return "";
    }
}
