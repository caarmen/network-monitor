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
package ca.rmen.android.networkmonitor.app.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dialog.ChoiceDialogFragment.DialogItemListener;
import ca.rmen.android.networkmonitor.app.dialog.ConfirmDialogFragment.DialogButtonListener;
import ca.rmen.android.networkmonitor.app.dialog.DialogFragmentFactory;
import ca.rmen.android.networkmonitor.app.dialog.PreferenceDialog;
import ca.rmen.android.networkmonitor.app.dialog.ProgressDialogFragment;
import ca.rmen.android.networkmonitor.app.export.CSVExport;
import ca.rmen.android.networkmonitor.app.export.DBExport;
import ca.rmen.android.networkmonitor.app.export.ExcelExport;
import ca.rmen.android.networkmonitor.app.export.FileExport;
import ca.rmen.android.networkmonitor.app.export.HTMLExport;
import ca.rmen.android.networkmonitor.app.export.SummaryExport;
import ca.rmen.android.networkmonitor.app.export.kml.KMLExport;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Provides actions on the network monitor log: sharing and clearing the log file.
 * This activity has a transparent theme. The only thing the user will see will be alert dialogs that this activity creates.
 */
public class LogActionsActivity extends FragmentActivity implements DialogButtonListener, DialogItemListener, OnCancelListener, OnDismissListener { // NO_UCD (use default)
    static final String ACTION_SHARE = LogActionsActivity.class.getPackage().getName() + "_share";
    static final String ACTION_CLEAR = LogActionsActivity.class.getPackage().getName() + "_clear";

    private static final String TAG = Constants.TAG + LogActionsActivity.class.getSimpleName();
    private static final String PROGRESS_DIALOG_TAG = ProgressDialogFragment.class.getSimpleName();
    private boolean mListItemSelected = false;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        String action = getIntent().getAction();
        if (ACTION_SHARE.equals(action)) {
            DialogFragmentFactory.showChoiceDialog(this, getString(R.string.export_choice_title), getResources().getStringArray(R.array.export_choices), -1,
                    R.id.action_share);
        } else if (ACTION_CLEAR.equals(action)) {
            DialogFragmentFactory.showConfirmDialog(this, getString(R.string.action_clear), getString(R.string.confirm_logs_clear), R.id.action_clear, null);
        } else {
            Log.w(TAG, "Activity created without a known action.  Action=" + action);
            finish();
        }
    }

    /**
     * Prompt a user for the field they want to export to KML, then do the export.
     */
    private void shareKml() {
        Log.v(TAG, "shareKml");

        PreferenceDialog.showKMLExportColumnChoiceDialog(this, new PreferenceDialog.PreferenceChoiceDialogListener() {

            @Override
            public void onPreferenceValueSelected(String value) {
                try {
                    KMLExport kmlExport = new KMLExport(LogActionsActivity.this, mExportProgressListener, value);
                    shareFile(kmlExport);
                } catch (FileNotFoundException e) {
                    Log.w(TAG, "Error sharing file: " + e.getMessage(), e);
                }
            }

            @Override
            public void onCancel() {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    /**
     * Run the given file export, then bring up the chooser intent to share the exported file.
     */
    private void shareFile(final FileExport fileExport) {
        Log.v(TAG, "shareFile " + fileExport);
        // Use a horizontal progress bar style if we can show progress of the export.
        String dialogMessage = getString(R.string.export_progress_preparing_export);
        int dialogStyle = fileExport != null ? ProgressDialog.STYLE_HORIZONTAL : ProgressDialog.STYLE_SPINNER;
        DialogFragmentFactory.showProgressDialog(this, dialogMessage, dialogStyle, PROGRESS_DIALOG_TAG);

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
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.export_subject_send_log));

                String dateRange = SummaryExport.getDataCollectionDateRange(LogActionsActivity.this);

                String messageBody = getString(R.string.export_message_text, dateRange);
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
                if (fileExport != null && result == null)
                    Toast.makeText(LogActionsActivity.this, R.string.export_error_sdcard_unmounted, Toast.LENGTH_LONG).show();
                finish();
            }

        };
        asyncTask.execute();
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

    @Override
    public void onOkClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onOkClicked, actionId = " + actionId);
        // The user confirmed to clear the logs.
        if (actionId == R.id.action_clear) {
            Log.v(TAG, "Clicked ok to clear log");
            DialogFragmentFactory.showProgressDialog(LogActionsActivity.this, getString(R.string.progress_dialog_message), ProgressDialog.STYLE_SPINNER,
                    PROGRESS_DIALOG_TAG);
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
    }

    @Override
    public void onItemSelected(int actionId, CharSequence[] choices, int which) {
        Log.v(TAG, "onItemSelected: actionId =  " + actionId + ", choices = " + Arrays.toString(choices) + ", which = " + which);
        mListItemSelected = true;
        // The user picked a file format to export.
        if (actionId == R.id.action_share) {

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
            } catch (IOException e) {
                Log.w(TAG, "Error sharing file: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onCancelClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onCancelClicked, actionId = " + actionId);
        if (actionId == R.id.action_clear || actionId == R.id.action_share) dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Log.v(TAG, "onDismiss");
        if (mListItemSelected) {
            // Ignore, the share choice dialog was dismissed because the user selected one of the file formats
        } else {
            dismiss();
        }
    }

    /**
     * Listener to finish this activity with a canceled result when the user presses back on a dialog.
     */
    @Override
    public void onCancel(DialogInterface dialog) {
        Log.v(TAG, "onCancel");
        dismiss();
    }

    private void dismiss() {
        Log.v(TAG, "dismiss");
        setResult(RESULT_CANCELED);
        finish();
    }
}
