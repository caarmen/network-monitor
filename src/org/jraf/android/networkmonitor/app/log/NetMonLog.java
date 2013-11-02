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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.app.dialog.ProgressDialogFragment;
import org.jraf.android.networkmonitor.app.export.CSVExport;
import org.jraf.android.networkmonitor.app.export.DBExport;
import org.jraf.android.networkmonitor.app.export.ExcelExport;
import org.jraf.android.networkmonitor.app.export.FileExport;
import org.jraf.android.networkmonitor.app.export.HTMLExport;
import org.jraf.android.networkmonitor.app.export.SummaryExport;
import org.jraf.android.networkmonitor.provider.NetMonColumns;

/**
 * Provides actions on the network monitor log: sharing and resetting the log file.
 */
public class NetMonLog {
    private static final String TAG = Constants.TAG + NetMonLog.class.getSimpleName();
    private static final String PROGRESS_DIALOG_TAG = ProgressDialogFragment.class.getSimpleName();
    private NetMonLogListener mListener = null;

    public interface NetMonLogListener {
        void logReset();
    }

    private final FragmentActivity mActivity;


    public NetMonLog(FragmentActivity activity) {
        mActivity = activity;
    }

    public void setListener(NetMonLogListener listener) {
        mListener = listener;
    }

    /**
     * Ask the user to choose an export format, generate the file in that format, then bring up the share chooser intent so the user can choose how to share
     * the file.
     */
    public void share() {
        Log.v(TAG, "share");
        // Build a chooser dialog for the file format.
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity).setTitle(R.string.export_choice_title).setItems(R.array.export_choices,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String[] exportChoices = mActivity.getResources().getStringArray(R.array.export_choices);
                        FileExport fileExport = null;
                        try {
                            if (mActivity.getString(R.string.export_choice_csv).equals(exportChoices[which])) {
                                fileExport = new CSVExport(mActivity, mExportProgressListener);
                            } else if (mActivity.getString(R.string.export_choice_html).equals(exportChoices[which])) {
                                fileExport = new HTMLExport(mActivity, true, mExportProgressListener);
                            } else if (mActivity.getString(R.string.export_choice_excel).equals(exportChoices[which])) {
                                fileExport = new ExcelExport(mActivity, mExportProgressListener);
                            } else if (mActivity.getString(R.string.export_choice_db).equals(exportChoices[which])) {
                                fileExport = new DBExport(mActivity, mExportProgressListener);
                            } else {
                                // Text summary only
                            }
                            shareFile(fileExport);
                        } catch (FileNotFoundException e) {
                            Log.w(TAG, "Error sharing file: " + e.getMessage(), e);
                        }
                    }
                });
        builder.create().show();
    }

    /**
     * Purge the DB.
     */
    public void purge() {
        Log.v(TAG, "resetLogs");

        // Bring up a confirmation dialog.
        new AlertDialog.Builder(mActivity).setTitle(R.string.action_reset).setMessage(R.string.confirm_logs_reset)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // If the user agrees to delete the logs, run
                        // the delete in the background.
                        final ProgressBar progressBar = (ProgressBar) mActivity.findViewById(R.id.progress_bar);
                        progressBar.setVisibility(View.VISIBLE);
                        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {

                            @Override
                            protected Void doInBackground(Void... params) {
                                Log.v(TAG, "resetLogs:doInBackground");
                                mActivity.getContentResolver().delete(NetMonColumns.CONTENT_URI, null, null);
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void result) {
                                // Once the DB is deleted, reload the WebView.
                                Log.v(TAG, "resetLogs:onPostExecute");
                                super.onPostExecute(result);
                                Toast.makeText(mActivity, R.string.success_logs_reset, Toast.LENGTH_LONG).show();
                                if (mListener != null) mListener.logReset();
                            }
                        };
                        asyncTask.execute();
                    }
                }).setNegativeButton(android.R.string.no, null).show();
    }

    /**
     * Run the given file export, then bring up the chooser intent to share the exported file.
     */
    private void shareFile(final FileExport fileExport) {
        Log.v(TAG, "shareFile " + fileExport);
        DialogFragment dialogFragment = new ProgressDialogFragment();
        // Use a horizontal progress bar style if we can show progress of the export.
        Bundle fragmentArgs = new Bundle(1);
        fragmentArgs.putInt(ProgressDialogFragment.EXTRA_PROGRESS_DIALOG_STYLE, fileExport != null ? ProgressDialog.STYLE_HORIZONTAL
                : ProgressDialog.STYLE_SPINNER);
        dialogFragment.setArguments(fragmentArgs);
        dialogFragment.setCancelable(false);
        dialogFragment.show(mActivity.getSupportFragmentManager(), PROGRESS_DIALOG_TAG);

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

                String reportSummary = SummaryExport.getSummary(mActivity);
                // Bring up the chooser to share the file.
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, mActivity.getString(R.string.subject_send_log));
                String messageBody = mActivity.getString(R.string.export_message_text);
                if (file != null) {
                    sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file.getAbsolutePath()));
                    sendIntent.setType("message/rfc822");
                    messageBody += mActivity.getString(R.string.export_message_text_file_attached);
                } else {
                    sendIntent.setType("text/plain");
                }
                messageBody += reportSummary;
                sendIntent.putExtra(Intent.EXTRA_TEXT, messageBody);

                mActivity.startActivity(Intent.createChooser(sendIntent, mActivity.getResources().getText(R.string.action_share)));
                return file;
            }

            @Override
            protected void onPostExecute(File result) {
                super.onPostExecute(result);
                DialogFragment fragment = (DialogFragment) mActivity.getSupportFragmentManager().findFragmentByTag(PROGRESS_DIALOG_TAG);
                if (fragment != null) fragment.dismissAllowingStateLoss();
                // Show a toast if we failed to export a file.
                if (fileExport != null && result == null) Toast.makeText(mActivity, R.string.error_sdcard_unmounted, Toast.LENGTH_LONG).show();
            }

        };
        asyncTask.execute();
    }



    private final FileExport.ExportProgressListener mExportProgressListener = new FileExport.ExportProgressListener() {

        @Override
        public void onExportProgress(final int progress, final int max) {
            Log.v(TAG, "onRowExported: " + progress + "/" + max);
            mActivity.runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    ProgressDialogFragment fragment = (ProgressDialogFragment) mActivity.getSupportFragmentManager().findFragmentByTag(PROGRESS_DIALOG_TAG);
                    if (fragment != null) {
                        fragment.setProgress(progress, max);
                    }
                }
            });
        }
    };
}
