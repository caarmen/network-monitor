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
package ca.rmen.android.networkmonitor.app.dbops.ui;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.CSVExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.DBExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.ExcelExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.FileExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.HTMLExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.SummaryExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.kml.KMLExport;
import ca.rmen.android.networkmonitor.app.dialog.PreferenceDialog;
import ca.rmen.android.networkmonitor.util.Log;

/**
 *
 */
public class Share {
    private static final String TAG = Constants.TAG + Share.class.getSimpleName();

    /**
     * Export the log file in the given format, and display the list of apps to share the file.
     *
     * @param activity The progress of the file export will be displayed in a progress dialog on this activity.
     * @param selectedShareFormat the label of the file format selected by the user.
     */
    public static void share(FragmentActivity activity, String selectedShareFormat) {
        Log.v(TAG, "share " + selectedShareFormat);
        FileExport fileExport = null;
        if (activity.getString(R.string.export_choice_csv).equals(selectedShareFormat)) {
            fileExport = new CSVExport(activity);
        } else if (activity.getString(R.string.export_choice_html).equals(selectedShareFormat)) {
            fileExport = new HTMLExport(activity, true);
        } else if (activity.getString(R.string.export_choice_kml).equals(selectedShareFormat)) {
            // The KML export requires a second dialog before we can share, so we return here.
            shareKml(activity);
            return;
        } else if (activity.getString(R.string.export_choice_excel).equals(selectedShareFormat)) {
            fileExport = new ExcelExport(activity);
        } else if (activity.getString(R.string.export_choice_db).equals(selectedShareFormat)) {
            fileExport = new DBExport(activity);
        } else {
            // Text summary only
        }
        shareFile(activity, fileExport);
    }

    /**
     * Prompt a user for the field they want to export to KML, then do the export.
     */
    private static void shareKml(final FragmentActivity activity) {
        Log.v(TAG, "shareKml");

        PreferenceDialog.showKMLExportColumnChoiceDialog(activity, new PreferenceDialog.PreferenceChoiceDialogListener() {

            @Override
            public void onPreferenceValueSelected(String value) {
                KMLExport kmlExport = new KMLExport(activity, value);
                shareFile(activity, kmlExport);
            }

            @Override
            public void onCancel() {
                activity.setResult(Activity.RESULT_CANCELED);
                activity.finish();
            }
        });
    }

    /**
     * Run the given file export, then bring up the chooser intent to share the exported file.
     * The progress will be displayed in a progress dialog on the given activity.
     */
    private static void shareFile(final FragmentActivity activity, final FileExport fileExport) {
        Log.v(TAG, "shareFile " + fileExport);

        Bundle bundle = new Bundle(1);
        bundle.putString(DBOpAsyncTask.EXTRA_DIALOG_MESSAGE, activity.getString(R.string.export_progress_preparing_export));
        new DBOpAsyncTask<File>(activity, fileExport, bundle) {


            @Override
            protected File doInBackground(Void... params) {
                Log.v(TAG, "doInBackground");
                File file = null;
                if (fileExport != null) {
                    if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) return null;
                    file = super.doInBackground(params);
                    if (file == null) return null;
                }

                String reportSummary = SummaryExport.getSummary(activity);
                // Bring up the chooser to share the file.
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.export_subject_send_log));

                String dateRange = SummaryExport.getDataCollectionDateRange(activity);

                String messageBody = activity.getString(R.string.export_message_text, dateRange);
                if (file != null) {
                    sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file.getAbsolutePath()));
                    sendIntent.setType("message/rfc822");
                    messageBody += activity.getString(R.string.export_message_text_file_attached);
                } else {
                    sendIntent.setType("text/plain");
                }
                messageBody += reportSummary;
                sendIntent.putExtra(Intent.EXTRA_TEXT, messageBody);

                activity.startActivity(Intent.createChooser(sendIntent, activity.getResources().getText(R.string.action_share)));
                return file;
            }

            @Override
            protected void onPostExecute(File result) {
                Log.v(TAG, "onPostExecute");
                // Show a toast if we failed to export a file.
                if (fileExport != null && result == null) Toast.makeText(activity, R.string.export_error_sdcard_unmounted, Toast.LENGTH_LONG).show();
                super.onPostExecute(result);
            }
        }.execute();
    }

}
