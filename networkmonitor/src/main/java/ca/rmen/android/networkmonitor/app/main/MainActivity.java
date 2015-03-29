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
package ca.rmen.android.networkmonitor.app.main;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.jraf.android.backport.switchwidget.SwitchPreference;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dialog.PreferenceDialog;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.app.prefs.PreferencesCompat;
import ca.rmen.android.networkmonitor.app.service.NetMonService;
import ca.rmen.android.networkmonitor.app.speedtest.SpeedTestPreferences;
import ca.rmen.android.networkmonitor.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class MainActivity extends PreferenceActivity {
    private static final String TAG = Constants.TAG + MainActivity.class.getSimpleName();
    private Dialog mGPSDialog;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferencesCompat.setupActionBar(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        addPreferencesFromResource(R.xml.preferences);
        if (NetMonPreferences.getInstance(this).isServiceEnabled()) startService(new Intent(MainActivity.this, NetMonService.class));
        // Use strict mode for monkey tests. We can't enable strict mode for normal use
        // because, when sharing (exporting), the mail app may read the attachment in
        // the main thread.
        if (ActivityManager.isUserAMonkey())
            StrictMode.setThreadPolicy(new ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().penaltyLog().penaltyDeath().build());
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
        if (mGPSDialog != null) mGPSDialog.dismiss();
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

    @Override
    public void onBackPressed() {
        Log.v(TAG, "onBackPressed");
        // Prevent the monkey from exiting the app, to maximize the time the monkey spends testing the app.
        if (ActivityManager.isUserAMonkey()) {
            Log.v(TAG, "Sorry, monkeys must stay in the cage");
            return;
        }
        super.onBackPressed();
    }

    private final OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            NetMonPreferences prefs = NetMonPreferences.getInstance(MainActivity.this);
            if (NetMonPreferences.PREF_SERVICE_ENABLED.equals(key)) {
                if (sharedPreferences.getBoolean(NetMonPreferences.PREF_SERVICE_ENABLED, NetMonPreferences.PREF_SERVICE_ENABLED_DEFAULT)) {
                    int playServicesAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(MainActivity.this);
                    if (playServicesAvailable != ConnectionResult.SUCCESS) {
                        if (mGPSDialog != null) {
                            mGPSDialog.dismiss();
                            mGPSDialog = null;
                        }

                        if (GooglePlayServicesUtil.isUserRecoverableError(playServicesAvailable)) {
                            mGPSDialog = GooglePlayServicesUtil.getErrorDialog(playServicesAvailable, MainActivity.this, 1);
                        }
                        if (mGPSDialog != null) {
                            mGPSDialog.show();
                        } else {
                            Toast.makeText(MainActivity.this, "Google Play Services must be installed", Toast.LENGTH_LONG).show();
                        }
                    }
                    startService(new Intent(MainActivity.this, NetMonService.class));
                }
            } else if (NetMonPreferences.PREF_UPDATE_INTERVAL.equals(key)) {
                if (prefs.isFastPollingEnabled()) {
                    prefs.setConnectionTestEnabled(false);
                    if (prefs.getDBRecordCount() < 0) prefs.setDBRecordCount(10000);
                    SpeedTestPreferences.getInstance(MainActivity.this).setEnabled(false);
                    PreferenceDialog.showWarningDialog(MainActivity.this, getString(R.string.warning_fast_polling_title),
                            getString(R.string.warning_fast_polling_message));
                }
            }
        }
    };


}
