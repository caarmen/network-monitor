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

import android.os.Build;
import android.os.Bundle;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;

import ca.rmen.android.networkmonitor.app.dialog.MultiSelectListPreferenceDialogFragment;


public class NetMonPreferenceFragment extends PreferenceFragmentCompat {

    private static final String FRAGMENT_TAG_DIALOG = "android.support.v7.preference.PreferenceFragment.DIALOG";
    private static final String EXTRA_PREFERENCE_FILE_RES_ID = NetMonPreferenceFragment.class.getPackage().getName() + "_preference_file_res_id";

    public static NetMonPreferenceFragment newInstance(int preferenceFileResId) {
        NetMonPreferenceFragment result = new NetMonPreferenceFragment();
        Bundle arguments = new Bundle(1);
        arguments.putInt(EXTRA_PREFERENCE_FILE_RES_ID, preferenceFileResId);
        result.setArguments(arguments);
        return result;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        int preferenceFileResId = getArguments().getInt(EXTRA_PREFERENCE_FILE_RES_ID);
        addPreferencesFromResource(preferenceFileResId);
        PreferenceManager.setDefaultValues(getActivity(), preferenceFileResId, false);
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        // Hack to allow a MultiSelectListPreference
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH
                && preference instanceof MultiSelectListPreference) {
            if (getFragmentManager().findFragmentByTag(FRAGMENT_TAG_DIALOG) == null) {
                MultiSelectListPreferenceDialogFragment dialogFragment = MultiSelectListPreferenceDialogFragment.newInstance(preference.getKey());
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(getFragmentManager(), FRAGMENT_TAG_DIALOG);
            }
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }
}
