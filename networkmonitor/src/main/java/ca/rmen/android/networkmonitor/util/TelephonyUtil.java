/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013-2015 Carmen Alvarez (c@rmen.ca)
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

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Method;

import ca.rmen.android.networkmonitor.Constants;

public class TelephonyUtil {
    private static final String TAG = Constants.TAG + TelephonyUtil.class.getSimpleName();

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
        if (telMgr == null) return false;
        int simState = telMgr.getSimState();
        if (simState == TelephonyManager.SIM_STATE_ABSENT) return false;

        // http://stackoverflow.com/questions/12806709/android-how-to-tell-if-mobile-network-data-is-enabled-or-disabled-even-when
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        try {
            Class<?> cmClass = Class.forName(cm.getClass().getName());
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true); // Make the method callable
            // get the setting for "mobile data"
            return (Boolean) method.invoke(cm);
        } catch (Exception e) {
            // Verbose warning instead of error, because we don't want this polluting the logs.
            Log.v(TAG, "Could not determine if we have mobile data enabled", e);
            return true;
        }
    }

    /**
     * Returns the full network type (network type and subtype if available) of the active network info, as a String.
     * For example, this may turn just "WIFI" or "MOBILE/LTE".
     * @return the full network type as a String, or null, if we couldn't retrieve the active network info.
     */
    public static String getNetworkType(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return null;
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        if (activeNetworkInfo == null) return null;
        String networkType = activeNetworkInfo.getTypeName();
        String networkSubType = activeNetworkInfo.getSubtypeName();
        if (!TextUtils.isEmpty(networkSubType)) networkType += "/" + networkSubType;
        return networkType;
    }
}
