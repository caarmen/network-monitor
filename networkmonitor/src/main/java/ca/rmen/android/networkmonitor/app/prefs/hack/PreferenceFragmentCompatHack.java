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
package ca.rmen.android.networkmonitor.app.prefs.hack;

import android.os.Build;
import androidx.preference.MultiSelectListPreference;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class PreferenceFragmentCompatHack {
    private static final String FRAGMENT_TAG_DIALOG = "android.support.v7.preference.PreferenceFragment.DIALOG";

    private PreferenceFragmentCompatHack() {
        // prevent instantiation
    }

    /**
     * Displays preference dialogs which aren't supported by default in PreferenceFragmentCompat.
     *
     * @return true if we managed a preference which isn't supported by default, false otherwise.
     */
    public static boolean onDisplayPreferenceDialog(PreferenceFragmentCompat preferenceFragmentCompat, Preference preference) {
        FragmentManager fragmentManager = preferenceFragmentCompat.getFragmentManager();
        if (fragmentManager == null) return false;

        DialogFragment dialogFragment = (DialogFragment) fragmentManager.findFragmentByTag(FRAGMENT_TAG_DIALOG);
        if (dialogFragment != null) return false;

        // Hack to allow a MultiSelectListPreference
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH
                && preference instanceof MultiSelectListPreference) {
            dialogFragment = MultiSelectListPreferenceDialogFragmentCompat.newInstance(preference.getKey());
        }
        // Hack to allow a PasswordPreference
        else if (preference instanceof PasswordPreference) {
            dialogFragment = PasswordPreferenceDialogFragmentCompat.newInstance(preference.getKey());
        }

        // We've created our own fragment:
        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(preferenceFragmentCompat, 0);
            dialogFragment.show(fragmentManager, FRAGMENT_TAG_DIALOG);
            return true;
        }

        return false;
    }
}
