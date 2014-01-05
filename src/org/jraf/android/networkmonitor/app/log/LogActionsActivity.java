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
package org.jraf.android.networkmonitor.app.log;

import java.io.File;
import java.io.FileNotFoundException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.app.dialog.ProgressDialogFragment;
import org.jraf.android.networkmonitor.app.export.CSVExport;
import org.jraf.android.networkmonitor.app.export.DBExport;
import org.jraf.android.networkmonitor.app.export.ExcelExport;
import org.jraf.android.networkmonitor.app.export.FileExport;
import org.jraf.android.networkmonitor.app.export.HTMLExport;
import org.jraf.android.networkmonitor.app.export.KMLExport;
import org.jraf.android.networkmonitor.app.export.SummaryExport;
import org.jraf.android.networkmonitor.provider.NetMonColumns;

/**
 * Provides actions on the network monitor log: sharing and clearing the log file.
 * This activity has a transparent theme. The only thing the user will see will be alert dialogs that this activity creates.
 */
public class LogActionsActivity extends FragmentActivity { // NO_UCD (use default)
    static final String ACTION_SHARE = LogActionsActivity.class.getPackage().getName() + "_share";
    static final String ACTION_CLEAR = LogActionsActivity.class.getPackage().getName() + "_clear";

    private static final String TAG = Constants.TAG + LogActionsActivity.class.getSimpleName();
    private static final String PROGRESS_DIALOG_TAG = ProgressDialogFragment.class.getSimpleName();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        String action = getIntent().getAction();
        if (ACTION_SHARE.equals(action)) {
            share();
        } else if (ACTION_CLEAR.equals(action)) {
            clear();
        } else {
            Log.w(TAG, "Activity created without a known action.  Action=" + action);
            finish();
        }
    }

    /**
     * Ask the user to choose an export format, generate the file in that format, then bring up the share chooser intent so the user can choose how to share
     * the file.
     */
    private void share() {
        Log.v(TAG, "share");
        // Build a chooser dialog for the file format.
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.export_choice_title).setItems(R.array.export_choices,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String[] exportChoices = getResources().getStringArray(R.array.export_choices);
                        FileExport fileExport = null;
                        try {
                            if (getString(R.string.export_choice_csv).equals(exportChoices[which])) {
                                fileExport = new CSVExport(LogActionsActivity.this, mExportProgressListener);
                            } else if (getString(R.string.export_choice_html).equals(exportChoices[which])) {
                                fileExport = new HTMLExport(LogActionsActivity.this, true, mExportProgressListener);
                            } else if (getString(R.string.export_choice_kml).equals(exportChoices[which])) {
                                // The KML export requires a second dialog before we can share, so we return here.
                                shareKml();
                                return;
                            } else if (getString(R.string.export_choice_excel).equals(exportChoices[which])) {
                                fileExport = new ExcelExport(LogActionsActivity.this, mExportProgressListener);
                            } else if (getString(R.string.export_choice_db).equals(exportChoices[which])) {
                                fileExport = new DBExport(LogActionsActivity.this, mExportProgressListener);
                            } else {
                                // Text summary only
                            }
                            shareFile(fileExport);
                        } catch (FileNotFoundException e) {
                            Log.w(TAG, "Error sharing file: " + e.getMessage(), e);
                        }
                    }
                });
        builder.show().setOnCancelListener(mDialogCancelListener);
    }

    /**
     * Prompt a user for the field they want to export to KML, then do the export.
     */
    private void shareKml() {
        Log.v(TAG, "shareKml");
        // The list of column names (aka google_connection_test) which can be exported to KML.
        final String[] columnNames = getResources().getStringArray(R.array.db_columns);
        // The last column name the user chose to export.
        String prefColumnName = PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.PREF_KML_EXPORT_COLUMN, "google_connection_test");
        // The position in the list of column names of the last column the user chose to export.
        int prefColumnIndex = 0;

        // Build the list of choices for the user.  Look up the friendly label of each column name, and pre-select the one the user chose last time.
        final String[] columnLabels = new String[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) {
            int columnLabelId = getResources().getIdentifier(columnNames[i], "string", R.class.getPackage().getName());
            columnLabels[i] = getString(columnLabelId);
            if (prefColumnName.equals(columnNames[i])) prefColumnIndex = i;
        }
        AlertDialog.Builder kmlColumnDialog = new AlertDialog.Builder(this);
        kmlColumnDialog.setSingleChoiceItems(columnLabels, prefColumnIndex, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String columnLabel = columnLabels[which];
                // Save the column the user chose to export this time.
                Editor editor = PreferenceManager.getDefaultSharedPreferences(LogActionsActivity.this).edit();
                editor.putString(Constants.PREF_KML_EXPORT_COLUMN, columnNames[which]);
                editor.commit();
                // Do the actual export.
                try {
                    KMLExport kmlExport = new KMLExport(LogActionsActivity.this, mExportProgressListener, columnLabel);
                    shareFile(kmlExport);
                } catch (FileNotFoundException e) {
                    Log.w(TAG, "Error sharing file: " + e.getMessage(), e);
                }
            }
        });
        kmlColumnDialog.setTitle(R.string.export_kml_choice_title);
        kmlColumnDialog.show().setOnCancelListener(mDialogCancelListener);
    }

    /**
     * Clear the DB.
     */
    private void clear() {
        Log.v(TAG, "clear");

        // Bring up a confirmation dialog.
        new AlertDialog.Builder(this).setTitle(R.string.action_clear).setMessage(R.string.confirm_logs_clear)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Log.v(TAG, "Clicked ok to clear log");
                        // If the user agrees to delete the logs, run
                        // the delete in the background.
                        showProgressDialog(ProgressDialog.STYLE_SPINNER, getString(R.string.progress_dialog_message));
                        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {

                            @Override
                            protected Void doInBackground(Void... params) {
                                Log.v(TAG, "clear:doInBackground");
                                getContentResolver().delete(NetMonColumns.CONTENT_URI, null, null);
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void result) {
                                // Once the DB is deleted, reload the WebView.
                                Log.v(TAG, "clear:onPostExecute");
                                Toast.makeText(LogActionsActivity.this, R.string.success_logs_clear, Toast.LENGTH_LONG).show();
                                setResult(RESULT_OK);
                                finish();
                            }
                        };
                        asyncTask.execute();
                    }
                }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.v(TAG, "Clicked cancel: not clearing logs");
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }).setOnCancelListener(mDialogCancelListener).show();
        ;
    }

    /**
     * Run the given file export, then bring up the chooser intent to share the exported file.
     */
    private void shareFile(final FileExport fileExport) {
        Log.v(TAG, "shareFile " + fileExport);
        // Use a horizontal progress bar style if we can show progress of the export.
        String dialogMessage = getString(R.string.export_progress_preparing_export);
        int dialogStyle = fileExport != null ? ProgressDialog.STYLE_HORIZONTAL : ProgressDialog.STYLE_SPINNER;
        showProgressDialog(dialogStyle, dialogMessage);

        AsyncTask<Void, Void, File> asyncTask = new AsyncTask<Void, Void, File>() {

            @Override
            protected File doInBackground(Void... params) {
                File file = null;
                if (fileExport != null) {
                    if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) return null;
                    try {
                        // Export the file in the background.
                        file = fileExport.export();
                    } catch (Throwable t) {
                        Log.e(TAG, "Error exporting file " + fileExport + ": " + t.getMessage(), t);
                    }
                    if (file == null) return null;
                }

                String reportSummary = SummaryExport.getSummary(LogActionsActivity.this);
                // Bring up the chooser to share the file.
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.subject_send_log));
                String messageBody = getString(R.string.export_message_text);
                if (file != null) {
                    sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file.getAbsolutePath()));
                    sendIntent.setType("message/rfc822");
                    messageBody += getString(R.string.export_message_text_file_attached);
                } else {
                    sendIntent.setType("text/plain");
                }
                messageBody += reportSummary;
                sendIntent.putExtra(Intent.EXTRA_TEXT, messageBody);

                startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.action_share)));
                return file;
            }

            @Override
            protected void onPostExecute(File result) {
                super.onPostExecute(result);
                DialogFragment fragment = (DialogFragment) getSupportFragmentManager().findFragmentByTag(PROGRESS_DIALOG_TAG);
                if (fragment != null) fragment.dismissAllowingStateLoss();
                // Show a toast if we failed to export a file.
                if (fileExport != null && result == null) Toast.makeText(LogActionsActivity.this, R.string.error_sdcard_unmounted, Toast.LENGTH_LONG).show();
                finish();
            }

        };
        asyncTask.execute();
    }

    private void showProgressDialog(int style, String message) {
        Log.v(TAG, "showProgressDialog: style=" + style);

        DialogFragment dialogFragment = new ProgressDialogFragment();
        // Use a horizontal progress bar style if we can show progress of the export.
        Bundle fragmentArgs = new Bundle(1);
        fragmentArgs.putInt(ProgressDialogFragment.EXTRA_PROGRESS_DIALOG_STYLE, style);
        fragmentArgs.putString(ProgressDialogFragment.EXTRA_PROGRESS_DIALOG_MESSAGE, message);
        dialogFragment.setArguments(fragmentArgs);
        dialogFragment.setCancelable(false);
        dialogFragment.show(getSupportFragmentManager(), PROGRESS_DIALOG_TAG);
    }



    private final FileExport.ExportProgressListener mExportProgressListener = new FileExport.ExportProgressListener() {

        @Override
        public void onExportProgress(final int progress, final int max) {
            Log.v(TAG, "onRowExported: " + progress + "/" + max);
            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    ProgressDialogFragment fragment = (ProgressDialogFragment) getSupportFragmentManager().findFragmentByTag(PROGRESS_DIALOG_TAG);
                    if (fragment != null) {
                        fragment.setProgress(progress, max);
                    }
                }
            });
        }
    };

    private final DialogInterface.OnCancelListener mDialogCancelListener = new DialogInterface.OnCancelListener() {
        // When a user cancels a dialog, we have to finish this activity.
        @Override
        public void onCancel(DialogInterface dialog) {
            Log.v(TAG, "dialog canceled");
            finish();
        }
    };
}
