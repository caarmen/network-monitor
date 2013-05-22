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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.app.export.CSVExport;
import org.jraf.android.networkmonitor.app.export.DBExport;
import org.jraf.android.networkmonitor.app.export.ExcelExport;
import org.jraf.android.networkmonitor.app.export.FileExport;
import org.jraf.android.networkmonitor.app.export.HTMLExport;
import org.jraf.android.networkmonitor.app.export.SummaryExport;
import org.jraf.android.networkmonitor.provider.NetMonColumns;

public class LogActivity extends Activity {
    private static final String TAG = Constants.TAG + LogActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log);
        loadHTMLFile();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.log, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                share();
                return true;
            case R.id.action_refresh:
                loadHTMLFile();
                return true;
            case R.id.action_reset:
                resetLogs();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
                        FileExport fileExport;
                        try {
                            if (getString(R.string.export_choice_csv).equals(exportChoices[which])) {
                                fileExport = new CSVExport(LogActivity.this);
                            } else if (getString(R.string.export_choice_html).equals(exportChoices[which])) {
                                fileExport = new HTMLExport(LogActivity.this);
                            } else if (getString(R.string.export_choice_excel).equals(exportChoices[which])) {
                                fileExport = new ExcelExport(LogActivity.this);
                            } else if (getString(R.string.export_choice_db).equals(exportChoices[which])) {
                                fileExport = new DBExport(LogActivity.this);
                            } else {
                                Log.w(TAG, "Invalid file format chosen: " + which);
                                return;
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
     * Run the given file export, then bring up the chooser intent to share the exported file.
     */
    private void shareFile(final FileExport fileExport) {
        Log.v(TAG, "shareFile " + fileExport);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        AsyncTask<Void, Void, File> asyncTask = new AsyncTask<Void, Void, File>() {

            @Override
            protected File doInBackground(Void... params) {
                if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) return null;
                // Export the file in the background.
                File file = fileExport.export();
                if (file == null) return null;

                String reportSummary = SummaryExport.getSummary(LogActivity.this);
                // Bring up the chooser to share the file.
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.subject_send_log));
                String mailBody = getString(R.string.mail_body);
                String body = mailBody + reportSummary;
                sendIntent.putExtra(Intent.EXTRA_TEXT, body);
                sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file.getAbsolutePath()));
                sendIntent.setType("message/rfc822");
                startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.action_share)));
                return file;
            }

            @Override
            protected void onPostExecute(File result) {
                super.onPostExecute(result);
                progressBar.setVisibility(View.GONE);
                if (result == null) Toast.makeText(LogActivity.this, R.string.error_sdcard_unmounted, Toast.LENGTH_LONG).show();
            }

        };
        asyncTask.execute();
    }

    /**
     * Read the data from the DB, export it to an HTML file, and load the HTML file in the WebView.
     */
    private void loadHTMLFile() {
        Log.v(TAG, "loadHTMLFile");
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);
        AsyncTask<Void, Void, File> asyncTask = new AsyncTask<Void, Void, File>() {

            @Override
            protected File doInBackground(Void... params) {
                Log.v(TAG, "loadHTMLFile:doInBackground");
                try {
                    // Export the DB to the HTML file.
                    HTMLExport htmlExport = new HTMLExport(LogActivity.this);
                    File file = htmlExport.export();
                    return file;
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "doInBackground Could not load data into html file: " + e.getMessage(), e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(File result) {
                Log.v(TAG, "loadHTMLFile:onPostExecute, result=" + result);
                super.onPostExecute(result);
                if (result == null) {
                    Toast.makeText(LogActivity.this, R.string.error_reading_log, Toast.LENGTH_LONG).show();
                    return;
                }
                // Load the exported HTML file into the WebView.
                final WebView webView = (WebView) findViewById(R.id.web_view);
                webView.getSettings().setUseWideViewPort(true);
                webView.getSettings().setBuiltInZoomControls(true);
                webView.loadUrl("file://" + result.getAbsolutePath());
                webView.setWebViewClient(new WebViewClient() {

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        progressBar.setVisibility(View.GONE);
                        webView.pageDown(true);
                    }
                });
            }
        };
        asyncTask.execute();
    }

    // TODO cleanup copy/paste between here and MainActivity.resetLogs
    /**
     * Purge the DB.
     */
    private void resetLogs() {
        Log.v(TAG, "resetLogs");

        // Bring up a confirmation dialog.
        new AlertDialog.Builder(this).setTitle(R.string.action_reset).setMessage(R.string.confirm_logs_reset)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // If the user agrees to delete the logs, run
                        // the delete in the background.
                        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
                        progressBar.setVisibility(View.VISIBLE);
                        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {

                            @Override
                            protected Void doInBackground(Void... params) {
                                Log.v(TAG, "resetLogs:doInBackground");
                                getContentResolver().delete(NetMonColumns.CONTENT_URI, null, null);
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void result) {
                                // Once the DB is deleted, reload the WebView.
                                Log.v(TAG, "resetLogs:onPostExecute");
                                super.onPostExecute(result);
                                Toast.makeText(LogActivity.this, R.string.success_logs_reset, Toast.LENGTH_LONG).show();
                                loadHTMLFile();
                            }
                        };
                        asyncTask.execute();
                    }
                }).setNegativeButton(android.R.string.no, null).show();
    }
}
