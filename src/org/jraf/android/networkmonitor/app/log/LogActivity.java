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
import java.io.IOException;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import org.jraf.android.networkmonitor.app.dialog.ConfirmDialogFragment.DialogButtonListener;
import org.jraf.android.networkmonitor.app.dialog.DialogFragmentFactory;
import org.jraf.android.networkmonitor.app.dialog.PreferenceDialog;
import org.jraf.android.networkmonitor.app.export.HTMLExport;
import org.jraf.android.networkmonitor.app.prefs.FilterColumnActivity;
import org.jraf.android.networkmonitor.app.prefs.NetMonPreferences;
import org.jraf.android.networkmonitor.app.prefs.SelectFieldsActivity;
import org.jraf.android.networkmonitor.app.prefs.SortPreferences;
import org.jraf.android.networkmonitor.app.prefs.SortPreferences.SortOrder;
import org.jraf.android.networkmonitor.util.Log;

public class LogActivity extends FragmentActivity implements DialogButtonListener {
    private static final String TAG = Constants.TAG + LogActivity.class.getSimpleName();
    private WebView mWebView;
    private static final int REQUEST_CODE_CLEAR = 1;
    private static final int REQUEST_CODE_SELECT_FIELDS = 2;
    private static final int REQUEST_CODE_FILTER_COLUMN = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) setDisplayHomeAsUpEnabled(true);
        loadHTMLFile();
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause");
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume");
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.log, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Only show the menu item to clear filters if we have filters.
        menu.findItem(R.id.action_reset_filters).setVisible(NetMonPreferences.getInstance(this).hasColumnFilters());
        return super.onPrepareOptionsMenu(menu);
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
            case R.id.action_select_fields:
                Intent intentSelectFields = new Intent(this, SelectFieldsActivity.class);
                startActivityForResult(intentSelectFields, REQUEST_CODE_SELECT_FIELDS);
                return true;
            case R.id.action_filter:
                PreferenceDialog.showFilterRecordCountChoiceDialog(this, mPreferenceChoiceDialogListener);
                return true;
            case R.id.action_cell_id_format:
                PreferenceDialog.showCellIdFormatChoiceDialog(this, mPreferenceChoiceDialogListener);
                return true;
            case R.id.action_reset_filters:
                DialogFragmentFactory.showConfirmDialog(this, getString(R.string.clear_filters_confirm_dialog_title),
                        getString(R.string.clear_filters_confirm_dialog_message), R.id.action_reset_filters, null);
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
                } catch (IOException e) {
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

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        Log.v(TAG, "url: " + url);
                        // If the user clicked on one of the column names, let's update
                        // the sorting preference (column name, ascending or descending order).
                        if (url.startsWith(HTMLExport.URL_SORT)) {
                            NetMonPreferences prefs = NetMonPreferences.getInstance(LogActivity.this);
                            SortPreferences oldSortPreferences = prefs.getSortPreferences();
                            // The new column used for sorting will be the one the user tapped on.
                            String newSortColumnName = url.substring(HTMLExport.URL_SORT.length());
                            SortOrder newSortOrder = oldSortPreferences.sortOrder;
                            // If the user clicked on the column which is already used for sorting,
                            // toggle the sort order between ascending and descending.
                            if (newSortColumnName.equals(oldSortPreferences.sortColumnName)) {
                                if (oldSortPreferences.sortOrder == SortOrder.DESC) newSortOrder = SortOrder.ASC;
                                else
                                    newSortOrder = SortOrder.DESC;
                            }
                            // Update the sorting preferences (our shared preference change listener will be notified
                            // and reload the page).
                            prefs.setSortPreferences(new SortPreferences(newSortColumnName, newSortOrder));
                            return true;
                        }
                        // If the user clicked on the filter icon, start the filter activity for this column.
                        else if (url.startsWith(HTMLExport.URL_FILTER)) {
                            Intent intent = new Intent(LogActivity.this, FilterColumnActivity.class);
                            String columnName = url.substring(HTMLExport.URL_FILTER.length());
                            intent.putExtra(FilterColumnActivity.EXTRA_COLUMN_NAME, columnName);
                            startActivityForResult(intent, REQUEST_CODE_FILTER_COLUMN);
                            return true;
                        } else {
                            return super.shouldOverrideUrlLoading(view, url);
                        }
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
        if ((requestCode == REQUEST_CODE_CLEAR || requestCode == REQUEST_CODE_SELECT_FIELDS || requestCode == REQUEST_CODE_FILTER_COLUMN)
                && resultCode == RESULT_OK) loadHTMLFile();
    }

    /**
     * Reload the page when the user accepts a preference choice dialog.
     */
    private final PreferenceDialog.PreferenceChoiceDialogListener mPreferenceChoiceDialogListener = new PreferenceDialog.PreferenceChoiceDialogListener() {

        @Override
        public void onPreferenceValueSelected(final String value) {
            loadHTMLFile();
        }

        @Override
        public void onCancel() {}
    };

    /**
     * Refresh the screen when certain shared preferences change.
     */
    private final OnSharedPreferenceChangeListener mSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(NetMonPreferences.PREF_SORT_COLUMN_NAME) || key.equals(NetMonPreferences.PREF_SORT_ORDER)) loadHTMLFile();
        }
    };

    @Override
    public void onOkClicked(int actionId, Bundle extras) {
        // The user confirmed to clear the logs.  Let's do that and refresh the screen.
        if (actionId == R.id.action_reset_filters) {
            NetMonPreferences.getInstance(this).resetColumnFilters();
            loadHTMLFile();
        }
    }

    @Override
    public void onCancelClicked(int actionId, Bundle extras) {}

}
