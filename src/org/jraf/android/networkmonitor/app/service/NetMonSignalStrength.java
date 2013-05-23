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
 * Copyright (C) 2010 The Android Open Source Project
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
package org.jraf.android.networkmonitor.app.service;

import android.content.Context;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.util.TelephonyUtil;

/**
 * The logic in this class comes from the Android source code. It is copied here because some of this logic is available only on API level 17+.
 */
public class NetMonSignalStrength {
    private static final String TAG = Constants.TAG + NetMonSignalStrength.class.getSimpleName();

    private Context mContext;
    public static final int SIGNAL_STRENGTH_NONE_OR_UNKNOWN = 0;
    public static final int SIGNAL_STRENGTH_POOR = 1;
    public static final int SIGNAL_STRENGTH_MODERATE = 2;
    public static final int SIGNAL_STRENGTH_GOOD = 3;
    public static final int SIGNAL_STRENGTH_GREAT = 4;

    private static final int GSM_SIGNAL_STRENGTH_GREAT = 12;
    private static final int GSM_SIGNAL_STRENGTH_GOOD = 8;
    private static final int GSM_SIGNAL_STRENGTH_MODERATE = 8;// WTF? good = moderate?

    public NetMonSignalStrength(Context context) {
        mContext = context;
    }

    /**
     * @return a value between 0 {@link #SIGNAL_STRENGTH_NONE_OR_UNKNOWN} and 4 {@link #SIGNAL_STRENGTH_GREAT}.
     */
    public int getLevel(SignalStrength signalStrength) {
        Log.v(TAG, "getLevel " + signalStrength);
        int phoneType = TelephonyUtil.getDeviceType(mContext);
        if (phoneType == TelephonyManager.PHONE_TYPE_GSM) {
            return getGSMSignalStrength(signalStrength);
        } else if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
            return getCDMASignalStrength(signalStrength);
        } else {
            Log.w(TAG, "Unknown phone type: " + phoneType);
            return SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        }
    }

    private int getCDMASignalStrength(SignalStrength signalStrength) {
        Log.v(TAG, "getCDMASignalStrength " + signalStrength);
        int level;

        int cdmaLevel = getCdmaLevel(signalStrength);
        int evdoLevel = getEvdoLevel(signalStrength);
        if (evdoLevel == SIGNAL_STRENGTH_NONE_OR_UNKNOWN) {
            /* We don't know evdo, use cdma */
            level = getCdmaLevel(signalStrength);
        } else if (cdmaLevel == SIGNAL_STRENGTH_NONE_OR_UNKNOWN) {
            /* We don't know cdma, use evdo */
            level = getEvdoLevel(signalStrength);
        } else {
            /* We know both, use the lowest level */
            level = cdmaLevel < evdoLevel ? cdmaLevel : evdoLevel;
        }
        Log.v(TAG, "getLevel=" + level);
        return level;
    }

    /**
     * Get cdma as level 0..4
     */
    private int getCdmaLevel(SignalStrength signalStrength) {
        final int cdmaDbm = signalStrength.getCdmaDbm();
        final int cdmaEcio = signalStrength.getCdmaEcio();
        int levelDbm;
        int levelEcio;

        if (cdmaDbm >= -75) levelDbm = SIGNAL_STRENGTH_GREAT;
        else if (cdmaDbm >= -85) levelDbm = SIGNAL_STRENGTH_GOOD;
        else if (cdmaDbm >= -95) levelDbm = SIGNAL_STRENGTH_MODERATE;
        else if (cdmaDbm >= -100) levelDbm = SIGNAL_STRENGTH_POOR;
        else
            levelDbm = SIGNAL_STRENGTH_NONE_OR_UNKNOWN;

        // Ec/Io are in dB*10
        if (cdmaEcio >= -90) levelEcio = SIGNAL_STRENGTH_GREAT;
        else if (cdmaEcio >= -110) levelEcio = SIGNAL_STRENGTH_GOOD;
        else if (cdmaEcio >= -130) levelEcio = SIGNAL_STRENGTH_MODERATE;
        else if (cdmaEcio >= -150) levelEcio = SIGNAL_STRENGTH_POOR;
        else
            levelEcio = SIGNAL_STRENGTH_NONE_OR_UNKNOWN;

        int level = levelDbm < levelEcio ? levelDbm : levelEcio;
        Log.v(TAG, "getCdmaLevel=" + level);
        return level;
    }

    /**
     * Get Evdo as level 0..4
     */
    private int getEvdoLevel(SignalStrength signalStrength) {
        int evdoDbm = signalStrength.getEvdoDbm();
        int evdoSnr = signalStrength.getEvdoSnr();
        int levelEvdoDbm;
        int levelEvdoSnr;

        if (evdoDbm >= -65) levelEvdoDbm = SIGNAL_STRENGTH_GREAT;
        else if (evdoDbm >= -75) levelEvdoDbm = SIGNAL_STRENGTH_GOOD;
        else if (evdoDbm >= -90) levelEvdoDbm = SIGNAL_STRENGTH_MODERATE;
        else if (evdoDbm >= -105) levelEvdoDbm = SIGNAL_STRENGTH_POOR;
        else
            levelEvdoDbm = SIGNAL_STRENGTH_NONE_OR_UNKNOWN;

        if (evdoSnr >= 7) levelEvdoSnr = SIGNAL_STRENGTH_GREAT;
        else if (evdoSnr >= 5) levelEvdoSnr = SIGNAL_STRENGTH_GOOD;
        else if (evdoSnr >= 3) levelEvdoSnr = SIGNAL_STRENGTH_MODERATE;
        else if (evdoSnr >= 1) levelEvdoSnr = SIGNAL_STRENGTH_POOR;
        else
            levelEvdoSnr = SIGNAL_STRENGTH_NONE_OR_UNKNOWN;

        int level = levelEvdoDbm < levelEvdoSnr ? levelEvdoDbm : levelEvdoSnr;
        Log.v(TAG, "getEvdoLevel=" + level);
        return level;
    }

    /**
     * Get signal level as an int from 0..4
     */
    private int getGSMSignalStrength(SignalStrength signalStrength) {
        Log.v(TAG, "getGSMSignalStrength " + signalStrength);

        // ASU ranges from 0 to 31 - TS 27.007 Sec 8.5
        // asu = 0 (-113dB or less) is very weak
        // signal, its better to show 0 bars to the user in such cases.
        // asu = 99 is a special case, where the signal strength is unknown.
        int asu = signalStrength.getGsmSignalStrength();
        if (asu <= 2 || asu == 99) return SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        else if (asu >= GSM_SIGNAL_STRENGTH_GREAT) return SIGNAL_STRENGTH_GREAT;
        else if (asu >= GSM_SIGNAL_STRENGTH_GOOD) return SIGNAL_STRENGTH_GOOD;
        else if (asu >= GSM_SIGNAL_STRENGTH_MODERATE) return SIGNAL_STRENGTH_MODERATE;
        else
            return SIGNAL_STRENGTH_POOR;
    }
}
