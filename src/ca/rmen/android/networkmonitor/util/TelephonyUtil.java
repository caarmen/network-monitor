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
package ca.rmen.android.networkmonitor.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

public class TelephonyUtil {
    private static final String TAG = TelephonyUtil.class.getSimpleName();

    private static final Map<String, String> sConstantsCache = new HashMap<String, String>();

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

    /**
     * @param mccMnc A string which should be 5 or 6 characters long, containing digits. This string is the concatenation of an MCC and MNC.
     * @return two strings: the first is the MCC, the second is the MNC. Will return two empty strings if the mccMnc parameter is invalid.
     */
    public static String[] getMccMnc(String mccMnc) {
        if (TextUtils.isEmpty(mccMnc) || mccMnc.length() < 5) return new String[] { "", "" };
        String mcc = mccMnc.substring(0, 3);
        String mnc = mccMnc.substring(3);
        return new String[] { mcc, mnc };
    }

    /**
     * @return true if the device is in airplane mode
     */
    public static boolean isAirplaneModeOn(Context context) {
        if (Build.VERSION.SDK_INT < 17) return isAirplaneModeOnDeprecated(context);
        else
            return isAirplaneModeOnApi17(context);
    }

    private static boolean isAirplaneModeOnDeprecated(Context context) {
        try {
            @SuppressWarnings("deprecation")
            int isAirplaneModeOnDeprecated = Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON);
            return isAirplaneModeOnDeprecated == 1;
        } catch (SettingNotFoundException e) {
            // Verbose warning instead of error, because we don't want this polluting the logs.
            Log.v(TAG, "Could not determine if we're in airplane mode", e);
            return false;
        }
    }

    @TargetApi(17)
    private static boolean isAirplaneModeOnApi17(Context context) {
        try {
            int isAirplaneModeOn = Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON);
            return isAirplaneModeOn == 1;
        } catch (SettingNotFoundException e) {
            // Verbose warning instead of error, because we don't want this polluting the logs.
            Log.v(TAG, "Could not determine if we're in airplane mode in API level 17+", e);
            return false;
        }

    }

    /**
     * @return true if mobile data is enabled (regardless of whether or not mobile data is being used).
     */
    public static boolean isMobileDataEnabled(Context context) {
        // If we have no SIM card, then then mobile data can't be enabled
        TelephonyManager telMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
        if (simState == TelephonyManager.SIM_STATE_ABSENT) return false;

        // http://stackoverflow.com/questions/12806709/android-how-to-tell-if-mobile-network-data-is-enabled-or-disabled-even-when
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Class<?> cmClass = Class.forName(cm.getClass().getName());
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true); // Make the method callable
            // get the setting for "mobile data"
            boolean mobileDataEnabled = (Boolean) method.invoke(cm);
            return mobileDataEnabled;
        } catch (Exception e) {
            // Verbose warning instead of error, because we don't want this polluting the logs.
            Log.v(TAG, "Could not determine if we have mobile data enabled", e);
            return true;
        }
    }
}
