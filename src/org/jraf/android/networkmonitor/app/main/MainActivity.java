/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013 Benoit 'BoD' Lubek (BoD@JRAF.org)
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
package org.jraf.android.networkmonitor.app.main;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.jraf.android.backport.switchwidget.SwitchPreference;
import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.app.prefs.NetMonPreferences;
import org.jraf.android.networkmonitor.app.service.NetMonService;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class MainActivity extends PreferenceActivity {
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        addPreferencesFromResource(R.xml.preferences);
        updateListPreferenceSummary(NetMonPreferences.PREF_UPDATE_INTERVAL, R.string.preferences_updateInterval_summary);
        if (NetMonPreferences.getInstance(this).isServiceEnabled()) startService(new Intent(MainActivity.this, NetMonService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        int playServicesAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (playServicesAvailable != ConnectionResult.SUCCESS) {
            Dialog errorDialog = null;

            if (GooglePlayServicesUtil.isUserRecoverableError(playServicesAvailable)) {
                errorDialog = GooglePlayServicesUtil.getErrorDialog(playServicesAvailable, this, 1);
            }
            if (errorDialog != null) {
                errorDialog.show();
            } else {
                Toast.makeText(this, "Google Play Services must be installed", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
    }

    @Override
    protected void onStop() {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        super.onStop();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            // Refresh the 'enabled' preference view
            boolean enabled = NetMonPreferences.getInstance(this).isServiceEnabled();
            ((SwitchPreference) findPreference(NetMonPreferences.PREF_SERVICE_ENABLED)).setChecked(enabled);
        }
    }

    private final OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (NetMonPreferences.PREF_SERVICE_ENABLED.equals(key)) {
                if (sharedPreferences.getBoolean(NetMonPreferences.PREF_SERVICE_ENABLED, NetMonPreferences.PREF_SERVICE_ENABLED_DEFAULT)) {
                    startService(new Intent(MainActivity.this, NetMonService.class));
                }
            } else if (NetMonPreferences.PREF_UPDATE_INTERVAL.equals(key)) {
                updateListPreferenceSummary(NetMonPreferences.PREF_UPDATE_INTERVAL, R.string.preferences_updateInterval_summary);
            }
        }
    };

    private void updateListPreferenceSummary(CharSequence key, int summaryResId) {
        @SuppressWarnings("deprecation")
        ListPreference pref = (ListPreference) getPreferenceManager().findPreference(key);
        CharSequence entry = pref.getEntry();
        String summary = getString(summaryResId, entry);
        pref.setSummary(summary);
    }

}
