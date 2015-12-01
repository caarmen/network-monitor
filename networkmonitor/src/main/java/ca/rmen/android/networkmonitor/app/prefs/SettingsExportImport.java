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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.File;
import java.util.Map;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.util.IoUtil;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Export and import shared preferences.
 */
public final class SettingsExportImport {
    private static final String TAG = Constants.TAG + SettingsExportImport.class.getSimpleName();
    private static final String PREF_IMPORT_VERIFICATION = "import_verifcation";

    private SettingsExportImport() {
        // prevent instantiation
    }

    /**
     * Copies the shared preferences file to the sd card, and prompts the user to share it.
     */
    public static void exportSettings(final Context context) {
        final File inputFile = getSharedPreferencesFile(context);
        final File outputFile = new File(context.getExternalFilesDir(null), inputFile.getName());
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                // Just in case: make sure we don't have our temp setting.
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
                if (sharedPrefs.contains(PREF_IMPORT_VERIFICATION)) {
                    Log.w(TAG, "Didn't expect to see the " + PREF_IMPORT_VERIFICATION + " setting when exporting");
                    sharedPrefs.edit().remove(PREF_IMPORT_VERIFICATION).commit();
                }
                return IoUtil.copy(inputFile, outputFile);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    // Bring up the chooser to share the file.
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.export_subject_send_settings));

                    sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + outputFile.getAbsolutePath()));
                    sendIntent.setType("message/rfc822");
                    sendIntent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.export_settings_message_text));
                    Intent chooserIntent = Intent.createChooser(sendIntent, context.getResources().getText(R.string.action_share));
                    context.startActivity(chooserIntent);
                } else {
                    Toast.makeText(context, R.string.export_settings_failure, Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    /**
     * Overwrites the app's default shared preferences file with the file at the given uri.
     */
    public static void importSettings(final Context context, Uri uri) {
        final File inputFile = new File(uri.getEncodedPath());
        final File outputFile = getSharedPreferencesFile(context);
        final File backupFile = new File(context.getCacheDir(), outputFile.getName() + ".bak");

        new AsyncTask<Void, Void, Boolean>() {

            private void rollback() {
                IoUtil.copy(backupFile, outputFile);
                reloadSettings(context);
            }

            @Override
            protected void onPreExecute() {
                Toast.makeText(context, R.string.import_settings_starting, Toast.LENGTH_LONG).show();
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                // Make a backup of our shared prefs in case the import file is corrupt.
                if (!IoUtil.copy(outputFile, backupFile)) {
                    return false;
                }

                // Set a temp preference now. We expect it to disappear after importing.
                PreferenceManager.getDefaultSharedPreferences(context)
                        .edit()
                        .putBoolean(PREF_IMPORT_VERIFICATION, true)
                        .commit();

                // Attempt the copy
                if (!IoUtil.copy(inputFile, outputFile)) {
                    rollback();
                    return false;
                }

                // Check that we have valid settings.
                if (!reloadSettings(context)) {
                    rollback();
                    return false;
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    Toast.makeText(context, context.getString(R.string.import_notif_complete_content, inputFile), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, context.getString(R.string.import_notif_error_content, inputFile), Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    private static boolean reloadSettings(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            if (!reloadSettingsPreV11(context)) return false;
        } else {
            if (!reloadSettingsV11(context)) return false;
        }

        // We expect our temporary preference to have been erased.
        return !PreferenceManager.getDefaultSharedPreferences(context).contains(PREF_IMPORT_VERIFICATION);
    }

    private static boolean reloadSettingsPreV11(Context context) {
        String sharedPreferencesName = getSharedPreferencesName(context);
        SharedPreferences sharedPreferences = context.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE);
        Map<String, ?> allSettings = sharedPreferences.getAll();
        Log.v(TAG, "allSettings: " + allSettings);
        return !allSettings.isEmpty();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static boolean reloadSettingsV11(Context context) {
        String sharedPreferencesName = getSharedPreferencesName(context);
        SharedPreferences sharedPreferences = context.getSharedPreferences(sharedPreferencesName, Context.MODE_MULTI_PROCESS);
        Map<String, ?> allSettings = sharedPreferences.getAll();
        Log.v(TAG, "allSettings: " + allSettings);
        return !allSettings.isEmpty();
    }

    private static String getSharedPreferencesName(Context context) {
        return context.getPackageName() + "_preferences";
    }

    private static File getSharedPreferencesFile(Context context) {
        File dataDir = new File(context.getApplicationInfo().dataDir);
        File sharedPrefsDir = new File(dataDir, "shared_prefs");
        String sharedPreferencesName = getSharedPreferencesName(context);
        return new File(sharedPrefsDir, sharedPreferencesName + ".xml");
    }

}
