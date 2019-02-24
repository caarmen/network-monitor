/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2015-2017 Carmen Alvarez (c@rmen.ca)
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import android.util.Log;

import java.io.File;
import java.util.Map;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.ui.Share;
import ca.rmen.android.networkmonitor.util.IoUtil;

/**
 * Export and import shared preferences.
 */
final class SettingsExportImport {
    private static final String TAG = Constants.TAG + SettingsExportImport.class.getSimpleName();
    private static final String PREF_IMPORT_VERIFICATION = "import_verification";

    interface SettingsImportCallback {
        /**
         * Called when the settings have been successfully imported.
         */
        void onSettingsImported();
    }

    private SettingsExportImport() {
        // prevent instantiation
    }

    /**
     * Copies the shared preferences file to the sd card, and prompts the user to share it.
     */
    static void exportSettings(final Activity activity) {
        AsyncTask.execute(() -> {
            File outputFile = exportFile(activity);
            activity.runOnUiThread(() -> {
                if (outputFile != null) {
                    // Bring up the chooser to share the file.
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.export_subject_send_settings));

                    Share.addFileToShareIntent(activity, sendIntent, outputFile.getName());
                    sendIntent.putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.export_settings_message_text));
                    Intent chooserIntent = Intent.createChooser(sendIntent, activity.getResources().getText(R.string.action_share));
                    activity.startActivity(chooserIntent);
                } else {
                    Snackbar.make(activity.getWindow().getDecorView().getRootView(), R.string.export_settings_failure, Snackbar.LENGTH_LONG).show();
                }
            });
        });
    }

    private static File exportFile(Activity activity) {
        final File inputFile = getSharedPreferencesFile(activity);
        File outputFile = Share.getExportFile(activity, inputFile.getName());
        if (outputFile == null) return null;
        // Just in case: make sure we don't have our temp setting.
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (sharedPrefs.contains(PREF_IMPORT_VERIFICATION)) {
            Log.w(TAG, "Didn't expect to see the " + PREF_IMPORT_VERIFICATION + " setting when exporting");
            sharedPrefs.edit().remove(PREF_IMPORT_VERIFICATION).apply();
        }
        if (IoUtil.copy(inputFile, outputFile)) {
            return outputFile;
        } else {
            return null;
        }
    }

    private static void rollback(Activity activity, File outputFile, File backupFile) {
        IoUtil.copy(backupFile, outputFile);
        reloadSettings(activity);
    }
    @SuppressLint("ApplySharedPref")
    private static boolean importSettings(Activity activity, Uri uri, File outputFile, File backupFile) {
        // Make a backup of our shared prefs in case the import file is corrupt.
        if (!IoUtil.copy(outputFile, backupFile)) {
            return false;
        }

        // Set a temp preference now. We expect it to disappear after importing.
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(PREF_IMPORT_VERIFICATION, true)
                .commit();

        // Attempt the copy
        if (IoUtil.copy(activity, uri, outputFile)) {
            Log.v(TAG, "Copy successful");
        } else {
            rollback(activity, outputFile, backupFile);
            return false;
        }

        // Check that we have valid settings.
        if (!reloadSettings(activity)) {
            rollback(activity, outputFile, backupFile);
            return false;
        }
        return true;

    }
    /**
     * Overwrites the app's default shared preferences file with the file at the given uri.
     */
    static void importSettings(final Activity activity, final Uri uri, final SettingsImportCallback callback) {
        final File outputFile = getSharedPreferencesFile(activity);
        final File backupFile = new File(activity.getCacheDir(), outputFile.getName() + ".bak");
        Snackbar.make(activity.getWindow().getDecorView().getRootView(), R.string.import_settings_starting, Snackbar.LENGTH_LONG).show();
        AsyncTask.execute(() -> {
            String fileDisplayName = Share.readDisplayName(activity, uri);
            boolean result = importSettings(activity, uri, outputFile, backupFile);
            activity.runOnUiThread(() -> {
                if (result) {
                    Snackbar.make(activity.getWindow().getDecorView().getRootView(), activity.getString(R.string.import_notif_complete_content, fileDisplayName), Snackbar.LENGTH_LONG).show();
                    callback.onSettingsImported();
                } else {
                    Snackbar.make(activity.getWindow().getDecorView().getRootView(), activity.getString(R.string.import_notif_error_content, fileDisplayName), Snackbar.LENGTH_LONG).show();
                }
            });
        });
    }

    private static boolean reloadSettings(Context context) {
        String sharedPreferencesName = getSharedPreferencesName(context);
        // This mode is deprecated but we still need this it in order to force a reread of preferences
        // from the disk.
        //noinspection deprecation
        SharedPreferences sharedPreferences = context.getSharedPreferences(sharedPreferencesName, Context.MODE_MULTI_PROCESS);
        Map<String, ?> allSettings = sharedPreferences.getAll();
        Log.v(TAG, "allSettings: " + allSettings);
        if (allSettings.isEmpty()) {
            return false;
        }

        // We expect our temporary preference to have been erased.
        return !PreferenceManager.getDefaultSharedPreferences(context).contains(PREF_IMPORT_VERIFICATION);
    }

    private static String getSharedPreferencesName(Context context) {
        return context.getPackageName() + "_preferences";
    }

    @NonNull
    private static File getSharedPreferencesFile(Context context) {
        File dataDir = new File(context.getApplicationInfo().dataDir);
        File sharedPrefsDir = new File(dataDir, "shared_prefs");
        String sharedPreferencesName = getSharedPreferencesName(context);
        return new File(sharedPrefsDir, sharedPreferencesName + ".xml");
    }

}
