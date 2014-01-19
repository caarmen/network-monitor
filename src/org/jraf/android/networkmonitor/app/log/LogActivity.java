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

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.app.export.HTMLExport;
import org.jraf.android.networkmonitor.app.prefs.NetMonPreferences;
import org.jraf.android.networkmonitor.util.Log;

public class LogActivity extends FragmentActivity {
    private static final String TAG = Constants.TAG + LogActivity.class.getSimpleName();
    private WebView mWebView;
    private static final int REQUEST_CODE_CLEAR = 1;
    private static final int REQUEST_CODE_FILTER = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) setDisplayHomeAsUpEnabled(true);
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
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_share:
                Intent intentShare = new Intent(LogActionsActivity.ACTION_SHARE);
                startActivity(intentShare);
                return true;
            case R.id.action_refresh:
                loadHTMLFile();
                return true;
            case R.id.action_clear:
                Intent intentClear = new Intent(LogActionsActivity.ACTION_CLEAR);
                startActivityForResult(intentClear, REQUEST_CODE_CLEAR);
                return true;
            case R.id.action_filter:
                Intent intentFilter = new Intent(LogActionsActivity.ACTION_FILTER);
                startActivityForResult(intentFilter, REQUEST_CODE_FILTER);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setDisplayHomeAsUpEnabled(boolean enabled) {
        getActionBar().setDisplayHomeAsUpEnabled(enabled);
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
                    HTMLExport htmlExport = new HTMLExport(LogActivity.this, false, null);
                    int recordCount = NetMonPreferences.getInstance(LogActivity.this).getFilterRecordCount();
                    File file = htmlExport.export(recordCount);
                    return file;
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "doInBackground Could not load data into html file: " + e.getMessage(), e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(File result) {
                Log.v(TAG, "loadHTMLFile:onPostExecute, result=" + result);
                if (isFinishing()) {
                    Log.v(TAG, "finishing, ignoring loadHTMLFile result");
                    return;
                }
                if (result == null) {
                    Toast.makeText(LogActivity.this, R.string.error_reading_log, Toast.LENGTH_LONG).show();
                    return;
                }
                // Load the exported HTML file into the WebView.
                mWebView = (WebView) findViewById(R.id.web_view);
                mWebView.getSettings().setUseWideViewPort(true);
                mWebView.getSettings().setBuiltInZoomControls(true);
                mWebView.loadUrl("file://" + result.getAbsolutePath());
                mWebView.setWebViewClient(new WebViewClient() {

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        };
        asyncTask.execute();
    }


    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        if (mWebView != null) {
            ((ViewGroup) mWebView.getParent()).removeAllViews();
            mWebView.destroy();
            mWebView = null;
        }
        super.onDestroy();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "onActivityResult: requestCode = " + requestCode + ", resultCode = " + resultCode + ", data  " + data);
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_CODE_CLEAR || requestCode == REQUEST_CODE_FILTER) && resultCode == RESULT_OK) loadHTMLFile();
    }

}
