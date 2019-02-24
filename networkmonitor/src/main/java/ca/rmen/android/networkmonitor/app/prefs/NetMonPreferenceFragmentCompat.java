/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2015 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.prefs;

import android.app.Activity;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import ca.rmen.android.networkmonitor.app.prefs.hack.PreferenceFragmentCompatHack;


public class NetMonPreferenceFragmentCompat extends PreferenceFragmentCompat {

    private static final String EXTRA_PREFERENCE_FILE_RES_ID = NetMonPreferenceFragmentCompat.class.getPackage().getName() + "_preference_file_res_id";

    public static NetMonPreferenceFragmentCompat newInstance(int preferenceFileResId) {
        NetMonPreferenceFragmentCompat result = new NetMonPreferenceFragmentCompat();
        Bundle arguments = new Bundle(1);
        arguments.putInt(EXTRA_PREFERENCE_FILE_RES_ID, preferenceFileResId);
        result.setArguments(arguments);
        return result;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        Bundle arguments = getArguments();
        Activity activity = getActivity();
        if (activity != null && arguments != null) {
            int preferenceFileResId = arguments.getInt(EXTRA_PREFERENCE_FILE_RES_ID);
            addPreferencesFromResource(preferenceFileResId);
            PreferenceManager.setDefaultValues(activity, preferenceFileResId, false);
        }
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (!PreferenceFragmentCompatHack.onDisplayPreferenceDialog(this, preference)) {
            super.onDisplayPreferenceDialog(preference);
        }
    }
}
