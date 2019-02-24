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
 * Copyright (C) 2013-2017 Carmen Alvarez (c@rmen.ca)
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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.core.app.TaskStackBuilder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.Theme;
import ca.rmen.android.networkmonitor.app.bus.NetMonBus;
import ca.rmen.android.networkmonitor.app.dbops.backend.DBOpIntentService;
import ca.rmen.android.networkmonitor.app.dialog.ConfirmDialogFragment;
import ca.rmen.android.networkmonitor.app.dialog.DialogFragmentFactory;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences.LocationFetchingStrategy;
import ca.rmen.android.networkmonitor.app.service.NetMonNotification;

public class AdvancedPreferencesActivity extends AppCompatActivity implements ConfirmDialogFragment.DialogButtonListener {
    private static final String TAG = Constants.TAG + AdvancedPreferencesActivity.class.getSimpleName();
    private static final int ACTIVITY_REQUEST_CODE_IMPORT_DB = 1;
    private static final int ACTIVITY_REQUEST_CODE_RINGTONE = 2;
    private static final int ACTIVITY_REQUEST_CODE_IMPORT_SETTINGS = 3;
    private static final String EXTRA_IMPORT_URI = AdvancedPreferencesActivity.class.getPackage().getName() + "_db_url";
    private static final int ID_ACTION_IMPORT_DB = 3;
    private static final int ID_ACTION_LOCATION_SETTINGS = 4;
    private static final int ID_ACTION_COMPRESS = 5;
    private static final int ID_ACTION_IMPORT_SETTINGS = 6;
    private static final String PREF_COMPRESS = "PREF_COMPRESS";
    private static final String PREF_IMPORT_DB = "PREF_IMPORT_DB";
    private static final String PREF_EXPORT_SETTINGS = "PREF_EXPORT_SETTINGS";
    private static final String PREF_IMPORT_SETTINGS = "PREF_IMPORT_SETTINGS";

    private NetMonPreferenceFragmentCompat mPreferenceFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        // The first time the user sees the notification preferences, we'll set the ringtone preference
        // to the default notification ringtone.
        if (!sharedPrefs.contains(NetMonPreferences.PREF_NOTIFICATION_RINGTONE)) NetMonPreferences.getInstance(this).setDefaultNotificationSoundUri();

        PreferenceManager.setDefaultValues(this, R.xml.adv_preferences, false);
        loadPreferences();
    }

    private void loadPreferences() {
        mPreferenceFragment = NetMonPreferenceFragmentCompat.newInstance(R.xml.adv_preferences);
        getSupportFragmentManager().
                beginTransaction().
                replace(android.R.id.content, mPreferenceFragment).
                commit();
        getSupportFragmentManager().executePendingTransactions();
        updatePreferenceSummary(NetMonPreferences.PREF_TEST_SERVER, R.string.pref_summary_test_server);
        updatePreferenceSummary(NetMonPreferences.PREF_NOTIFICATION_RINGTONE, R.string.pref_summary_notification_ringtone);
        Preference enableConnectionTest = mPreferenceFragment.findPreference(NetMonPreferences.PREF_ENABLE_CONNECTION_TEST);
        NetMonPreferences prefs = NetMonPreferences.getInstance(this);
        if (prefs.isFastPollingEnabled()) enableConnectionTest.setEnabled(false);
        mPreferenceFragment.findPreference(NetMonPreferences.PREF_TEST_SERVER).setOnPreferenceChangeListener(mEnsureNonEmptyPreferenceChangeListener);
        setOnPreferenceClickListeners(PREF_IMPORT_DB, PREF_COMPRESS, NetMonPreferences.PREF_NOTIFICATION_RINGTONE, PREF_IMPORT_SETTINGS, PREF_EXPORT_SETTINGS);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            Preference notificationPriorityPreference = mPreferenceFragment.findPreference(NetMonPreferences.PREF_NOTIFICATION_PRIORITY);
            notificationPriorityPreference.setVisible(false);
        }

    }

    private void setOnPreferenceClickListeners(String... keys) {
        for (String key : keys) {
            Preference preference = mPreferenceFragment.findPreference(key);
            preference.setOnPreferenceClickListener(mOnPreferenceClickListener);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        NetMonBus.getBus().register(this);
    }

    @Override
    protected void onStop() {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        NetMonBus.getBus().unregister(this);
        super.onStop();
    }


    private final OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = (sharedPreferences, key) -> {
        final NetMonPreferences prefs = NetMonPreferences.getInstance(AdvancedPreferencesActivity.this);
        Log.v(TAG, "onSharedPreferenceChanged: key = " + key);
        if (NetMonPreferences.PREF_TEST_SERVER.equals(key)) {
            updatePreferenceSummary(key, R.string.pref_summary_test_server);
        } else if (NetMonPreferences.PREF_NOTIFICATION_RINGTONE.equals(key)) {
            updatePreferenceSummary(key, R.string.pref_summary_notification_ringtone);
        } else if (NetMonPreferences.PREF_LOCATION_FETCHING_STRATEGY.equals(key)) {
            if (prefs.getLocationFetchingStrategy() == LocationFetchingStrategy.HIGH_ACCURACY
                    || prefs.getLocationFetchingStrategy() == LocationFetchingStrategy.HIGH_ACCURACY_GMS) {
                checkLocationSettings();
            }
        } else if (NetMonPreferences.PREF_NOTIFICATION_ENABLED.equals(key)) {
            if (!prefs.getShowNotificationOnTestFailure()) NetMonNotification.dismissFailedTestNotification(AdvancedPreferencesActivity.this);
        } else if (NetMonPreferences.PREF_DB_RECORD_COUNT.equals(key)) {
            int rowsToKeep = NetMonPreferences.getInstance(AdvancedPreferencesActivity.this).getDBRecordCount();
            if (rowsToKeep > 0) DBOpIntentService.startActionPurge(AdvancedPreferencesActivity.this, rowsToKeep);
        } else if (NetMonPreferences.PREF_THEME.equals(key)) {
            // When the theme changes, restart the activity
            Theme.setThemeFromSettings(getApplicationContext());
            Intent intent = new Intent(AdvancedPreferencesActivity.this, AdvancedPreferencesActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(AdvancedPreferencesActivity.this);
            stackBuilder.addNextIntentWithParentStack(intent);
            stackBuilder.startActivities();
        }
    };

    private void updatePreferenceSummary(final String key, final int summaryResId) {
        final Preference pref = mPreferenceFragment.findPreference(key);
        // I don't see how this is possible, but a crash was reported for a null pref...
        if (pref == null) {
            Log.v(TAG, "updatePreferenceSummary: No preference found for " + key);
            return;
        }

        if (pref instanceof EditTextPreference) {
            pref.setSummary(getString(summaryResId, ((EditTextPreference) pref).getText()));
        } else if (pref.getKey().equals(NetMonPreferences.PREF_NOTIFICATION_RINGTONE)) {
            new RingtonePreferenceSummaryUpdater().execute(pref);
        }
    }

    private static String getFileOpenIntentAction() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) return Intent.ACTION_OPEN_DOCUMENT;
        return Intent.ACTION_GET_CONTENT;
    }
    private final Preference.OnPreferenceClickListener mOnPreferenceClickListener = preference -> {
        Log.v(TAG, "onPreferenceClick: " + preference);
        if (PREF_IMPORT_DB.equals(preference.getKey())) {
            Intent importIntent = new Intent(getFileOpenIntentAction());
            importIntent.setType("*/*");
            importIntent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(importIntent, getResources().getText(R.string.pref_summary_import)), ACTIVITY_REQUEST_CODE_IMPORT_DB);
        } else if (PREF_COMPRESS.equals(preference.getKey())) {
            DialogFragmentFactory.showConfirmDialog(AdvancedPreferencesActivity.this, getString(R.string.compress_confirm_title), getString(R.string.compress_confirm_message),
                    ID_ACTION_COMPRESS, null);

        } else if (NetMonPreferences.PREF_NOTIFICATION_RINGTONE.equals(preference.getKey())) {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, NetMonPreferences.getInstance(getApplicationContext()).getNotificationSoundUri());

            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.pref_title_notification_ringtone));
            startActivityForResult(intent, ACTIVITY_REQUEST_CODE_RINGTONE);
        } else if (PREF_IMPORT_SETTINGS.equals(preference.getKey())) {
            Intent importIntent = new Intent(getFileOpenIntentAction());
            importIntent.setType("*/*");
            importIntent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(importIntent, getResources().getText(R.string.pref_title_import_settings)), ACTIVITY_REQUEST_CODE_IMPORT_SETTINGS);
        } else if (PREF_EXPORT_SETTINGS.equals(preference.getKey())) {
            SettingsExportImport.exportSettings(AdvancedPreferencesActivity.this);
        }
        return false;
    };

    private final Preference.OnPreferenceChangeListener mEnsureNonEmptyPreferenceChangeListener = (preference, newValue) -> {
        // Ignore the value if it is empty.
        String newValueStr = (String) newValue;
        return !TextUtils.isEmpty(newValueStr);
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.v(TAG, "onActivityResult: requestCode =  " + requestCode + ", resultCode = " + resultCode + ", data=" + data);
        /*
         * Allow the user to choose a DB to import
         */
        if (requestCode == ACTIVITY_REQUEST_CODE_IMPORT_DB) {
            if (resultCode == Activity.RESULT_OK) {
                // Get the file the user selected, and show a dialog asking for confirmation to import the file.
                Uri importFile = data.getData();
                if (importFile != null) {
                    Bundle extras = new Bundle(1);
                    extras.putParcelable(EXTRA_IMPORT_URI, importFile);
                    DialogFragmentFactory.showConfirmDialog(this, getString(R.string.import_confirm_title),
                            getString(R.string.import_confirm_message, importFile.getPath()), ID_ACTION_IMPORT_DB, extras);
                }
            }
        } else if (requestCode == ACTIVITY_REQUEST_CODE_RINGTONE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                NetMonPreferences.getInstance(this).setNotificationSoundUri(uri);
                updatePreferenceSummary(NetMonPreferences.PREF_NOTIFICATION_RINGTONE, R.string.pref_summary_notification_ringtone);
            }
        } else if (requestCode == ACTIVITY_REQUEST_CODE_IMPORT_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                Uri importFile = data.getData();
                if (importFile != null) {
                    Bundle extras = new Bundle(1);
                    extras.putParcelable(EXTRA_IMPORT_URI, importFile);
                    DialogFragmentFactory.showConfirmDialog(this, getString(R.string.import_confirm_title),
                            getString(R.string.import_settings_confirm_message, importFile.getPath()), ID_ACTION_IMPORT_SETTINGS, extras);
                }
            }
        }
    }

    @Override
    public void onOkClicked(int actionId, Bundle extras) {
        // Import the database in a background thread.
        if (actionId == ID_ACTION_IMPORT_DB) {
            final Uri uri = extras.getParcelable(EXTRA_IMPORT_URI);
            DBOpIntentService.startActionImport(this, uri);
        }
        // Compress the database in a background thread
        else if (actionId == ID_ACTION_COMPRESS) {
            DBOpIntentService.startActionCompress(this);
        } else if (actionId == ID_ACTION_LOCATION_SETTINGS) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        } else if (actionId == ID_ACTION_IMPORT_SETTINGS) {
            final Uri uri = extras.getParcelable(EXTRA_IMPORT_URI);
            SettingsExportImport.importSettings(this, uri, this::loadPreferences);
        }
    }

    @Override
    public void onCancelClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onCancelClicked, actionId=" + actionId + ", extras = " + extras);
    }

    @SuppressWarnings("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onDBOperationStarted(NetMonBus.DBOperationStarted event) {
        Log.d(TAG, "onDBOperationStarted() called with " + "event = [" + event + "]");
        mPreferenceFragment.findPreference(PREF_IMPORT_DB).setEnabled(false);
        mPreferenceFragment.findPreference(PREF_COMPRESS).setEnabled(false);
        mPreferenceFragment.findPreference(NetMonPreferences.PREF_DB_RECORD_COUNT).setEnabled(false);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onDBOperationEnded(NetMonBus.DBOperationEnded event) {
        Log.d(TAG, "onDBOperationEnded() called with " + "event = [" + event + "]");
        mPreferenceFragment.findPreference(PREF_IMPORT_DB).setEnabled(true);
        mPreferenceFragment.findPreference(PREF_COMPRESS).setEnabled(true);
        mPreferenceFragment.findPreference(NetMonPreferences.PREF_DB_RECORD_COUNT).setEnabled(true);
    }

    /**
     * Checks if we have either the GPS or Network location provider enabled. If not, shows a popup dialog telling the user they should go to the system
     * settings to enable location tracking.
     */
    private void checkLocationSettings() {
        // If the user chose high accuracy, make sure we have at least one location provider.
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return;
        if (!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))) {
            DialogFragmentFactory.showConfirmDialog(this, getString(R.string.no_location_confirm_dialog_title),
                    getString(R.string.no_location_confirm_dialog_message), ID_ACTION_LOCATION_SETTINGS, null);
        }
    }

}
