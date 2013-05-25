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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.jraf.android.backport.switchwidget.SwitchPreference;
import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.app.service.NetMonService;
import org.jraf.android.networkmonitor.provider.NetMonColumns;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class MainActivity extends PreferenceActivity {
    private static final String TAG = Constants.TAG + MainActivity.class.getSimpleName();

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        addPreferencesFromResource(R.xml.preferences);
        findPreference(Constants.PREF_RESET_LOG_FILE).setOnPreferenceClickListener(mOnPreferenceClickListener);
        updateListPreferenceSummary(Constants.PREF_WAKE_INTERVAL, R.string.preferences_wake_interval_summary);
        updateListPreferenceSummary(Constants.PREF_UPDATE_INTERVAL, R.string.preferences_updateInterval_summary);
        startService(new Intent(MainActivity.this, NetMonService.class));
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
            boolean enabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_SERVICE_ENABLED,
                    Constants.PREF_SERVICE_ENABLED_DEFAULT);
            ((SwitchPreference) findPreference(Constants.PREF_SERVICE_ENABLED)).setChecked(enabled);
        }
    }

    private final OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            boolean broadcastPrefChanged = false;
            if (Constants.PREF_SERVICE_ENABLED.equals(key)) {
                if (sharedPreferences.getBoolean(Constants.PREF_SERVICE_ENABLED, Constants.PREF_SERVICE_ENABLED_DEFAULT)) {
                    startService(new Intent(MainActivity.this, NetMonService.class));
                } else {
                    broadcastPrefChanged = true;
                }
            } else if (Constants.PREF_UPDATE_INTERVAL.equals(key)) {
                updateListPreferenceSummary(Constants.PREF_UPDATE_INTERVAL, R.string.preferences_updateInterval_summary);
                broadcastPrefChanged = true;

            } else if (Constants.PREF_WAKE_INTERVAL.equals(key)) {
                updateListPreferenceSummary(Constants.PREF_WAKE_INTERVAL, R.string.preferences_wake_interval_summary);
            }

            if (broadcastPrefChanged) LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(new Intent(NetMonService.ACTION_PREF_CHANGED));
        }
    };

    private void updateListPreferenceSummary(CharSequence key, int summaryResId) {
        ListPreference pref = (ListPreference) getPreferenceManager().findPreference(key);
        CharSequence entry = pref.getEntry();
        String summary = getString(summaryResId, entry);
        pref.setSummary(summary);
    }


    // TODO cleanup copy/paste between here and LogActivity.resetLogs
    /**
     * Purge the DB.
     */
    private void resetLogs() {
        Log.v(TAG, "resetLogs");
        new AlertDialog.Builder(this).setTitle(R.string.action_reset).setMessage(R.string.confirm_logs_reset)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {

                            @Override
                            protected Void doInBackground(Void... params) {
                                Log.v(TAG, "resetLogs:doInBackground");
                                getContentResolver().delete(NetMonColumns.CONTENT_URI, null, null);
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void result) {
                                Log.v(TAG, "resetLogs:onPostExecute");
                                super.onPostExecute(result);
                                Toast.makeText(MainActivity.this, R.string.success_logs_reset, Toast.LENGTH_LONG).show();
                            }
                        };
                        asyncTask.execute();
                    }
                }).setNegativeButton(android.R.string.no, null).show();
    }

    // When the user taps on the "reset logs" item, bring up a confirmation dialog, then purge the DB.
    OnPreferenceClickListener mOnPreferenceClickListener = new OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference pref) {
            Log.v(TAG, "onPreferenceClick: " + pref.getKey());
            if (Constants.PREF_RESET_LOG_FILE.equals(pref.getKey())) resetLogs();
            return true;
        }
    };

}
