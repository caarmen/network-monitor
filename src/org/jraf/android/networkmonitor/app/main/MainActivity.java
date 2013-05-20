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

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.app.service.NetMonService;
import org.jraf.android.networkmonitor.provider.NetMonColumns;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends PreferenceActivity {

	private static final String TAG = Constants.TAG
			+ MainActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		addPreferencesFromResource(R.xml.preferences);
		updateIntervalSummary();
		findPreference(Constants.PREF_RESET_LOG_FILE)
				.setOnPreferenceClickListener(mOnPreferenceClickListener);
	}

	@Override
	protected void onResume() {
		super.onResume();
		int playServicesAvailable = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (playServicesAvailable != ConnectionResult.SUCCESS) {
			Dialog errorDialog = null;

			if (GooglePlayServicesUtil
					.isUserRecoverableError(playServicesAvailable)) {
				errorDialog = GooglePlayServicesUtil.getErrorDialog(
						playServicesAvailable, this, 1);
			}
			if (errorDialog != null)
				errorDialog.show();
			else
				Toast.makeText(this, "Google Play Services must be installed",
						Toast.LENGTH_LONG).show();
		}
		startService(new Intent(MainActivity.this, NetMonService.class));
	}

	@Override
	protected void onStart() {
		super.onStart();
		PreferenceManager.getDefaultSharedPreferences(this)
				.registerOnSharedPreferenceChangeListener(
						mOnSharedPreferenceChangeListener);
	}

	@Override
	protected void onStop() {
		PreferenceManager.getDefaultSharedPreferences(this)
				.unregisterOnSharedPreferenceChangeListener(
						mOnSharedPreferenceChangeListener);
		super.onStop();
	}

	private final OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			if (Constants.PREF_SERVICE_ENABLED.equals(key)) {
				if (sharedPreferences.getBoolean(
						Constants.PREF_SERVICE_ENABLED,
						Constants.PREF_SERVICE_ENABLED_DEFAULT)) {
					startService(new Intent(MainActivity.this,
							NetMonService.class));
				}
			} else if (Constants.PREF_UPDATE_INTERVAL.equals(key)) {
				updateIntervalSummary();
			}
		}
	};

	private void updateIntervalSummary() {
		String value = PreferenceManager.getDefaultSharedPreferences(this)
				.getString(Constants.PREF_UPDATE_INTERVAL,
						Constants.PREF_UPDATE_INTERVAL_DEFAULT);
		int labelIndex = 0;
		int i = 0;
		for (String v : getResources().getStringArray(
				R.array.preferences_updateInterval_values)) {
			if (v.equals(value)) {
				labelIndex = i;
				break;
			}
			i++;
		}
		String[] labels = getResources().getStringArray(
				R.array.preferences_updateInterval_labels);
		findPreference(Constants.PREF_UPDATE_INTERVAL).setSummary(
				labels[labelIndex]);
	}

	// TODO cleanup copy/paste between here and LogActivity.resetLogs
	/**
	 * Purge the DB.
	 */
	private void resetLogs() {
		Log.v(TAG, "resetLogs");
		new AlertDialog.Builder(this)
				.setTitle(R.string.action_reset)
				.setMessage(R.string.confirm_logs_reset)
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int whichButton) {
								AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {

									@Override
									protected Void doInBackground(
											Void... params) {
										Log.v(TAG, "resetLogs:doInBackground");
										getContentResolver().delete(
												NetMonColumns.CONTENT_URI,
												null, null);
										return null;
									}

									@Override
									protected void onPostExecute(Void result) {
										Log.v(TAG, "resetLogs:onPostExecute");
										super.onPostExecute(result);
										Toast.makeText(MainActivity.this,
												R.string.success_logs_reset,
												Toast.LENGTH_LONG).show();
									}
								};
								asyncTask.execute();
							}
						}).setNegativeButton(android.R.string.no, null).show();
	}

	// When the user taps on the "reset logs" item, bring up a confirmation
	// dialog, then purge the DB.
	OnPreferenceClickListener mOnPreferenceClickListener = new OnPreferenceClickListener() {

		@Override
		public boolean onPreferenceClick(Preference pref) {
			Log.v(TAG, "onPreferenceClick: " + pref.getKey());
			if (Constants.PREF_RESET_LOG_FILE.equals(pref.getKey()))
				resetLogs();
			return true;
		}
	};

}
