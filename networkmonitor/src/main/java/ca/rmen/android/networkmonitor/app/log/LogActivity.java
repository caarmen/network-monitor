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
 * Copyright (C) 2013-2015 Carmen Alvarez (c@rmen.ca)
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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.NavUtils;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.bus.NetMonBus;
import ca.rmen.android.networkmonitor.app.dbops.backend.DBOpIntentService;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.HTMLExport;
import ca.rmen.android.networkmonitor.app.dbops.ui.Share;
import ca.rmen.android.networkmonitor.app.dialog.ChoiceDialogFragment;
import ca.rmen.android.networkmonitor.app.dialog.ConfirmDialogFragment.DialogButtonListener;
import ca.rmen.android.networkmonitor.app.dialog.DialogFragmentFactory;
import ca.rmen.android.networkmonitor.app.dialog.PreferenceDialog;
import ca.rmen.android.networkmonitor.app.prefs.FilterColumnActivity;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.app.prefs.SelectFieldsActivity;
import ca.rmen.android.networkmonitor.app.prefs.SortPreferences;

public class LogActivity extends AppCompatActivity implements DialogButtonListener, ChoiceDialogFragment.DialogItemListener {
    private static final String TAG = Constants.TAG + LogActivity.class.getSimpleName();
    private WebView mWebView;
    private Dialog mDialog;
    private Menu mMenu;
    private boolean mDBOpInProgress;
    private static final int REQUEST_CODE_CLEAR = 1;
    private static final int REQUEST_CODE_SELECT_FIELDS = 2;
    private static final int REQUEST_CODE_FILTER_COLUMN = 3;
    private static final int ID_ACTION_SHARE = 1;
    private static final int ID_ACTION_CLEAR = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mWebView = findViewById(R.id.web_view);
        assert mWebView != null;
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    //noinspection deprecation
                    mWebView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    mWebView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                loadHTMLFile();
            }
        });
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause");
        if (mDialog != null) mDialog.dismiss();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
        NetMonBus.getBus().unregister(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume");
        super.onResume();
        if (mDialog != null) mDialog.show();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
        NetMonBus.getBus().register(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.log, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Only show the menu item to clear filters if we have filters.
        menu.findItem(R.id.action_reset_filters).setVisible(NetMonPreferences.getInstance(this).hasColumnFilters());
        menu.findItem(R.id.action_clear).setEnabled(!mDBOpInProgress);
        MenuItem menuItem = menu.findItem(R.id.action_freeze_header);
        // Freezing the table header only seems to work on kitkat+.
        // Don't know why.  But we'll hide this feature on older versions.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            menuItem.setVisible(false);
        } else {
            boolean freezeHeader = NetMonPreferences.getInstance(this).getFreezeHtmlTableHeader();
            if (freezeHeader) menuItem.setTitle(R.string.action_unfreeze_header);
            else menuItem.setTitle(R.string.action_freeze_header);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_share:
                DialogFragmentFactory.showChoiceDialog(this, getString(R.string.export_choice_title), getResources().getStringArray(R.array.export_choices), -1,
                        ID_ACTION_SHARE);
                return true;
            case R.id.action_refresh:
                loadHTMLFile();
                return true;
            case R.id.action_clear:
                DialogFragmentFactory.showConfirmDialog(this, getString(R.string.action_clear), getString(R.string.confirm_logs_clear), ID_ACTION_CLEAR, null);
                return true;
            case R.id.action_select_fields:
                Intent intentSelectFields = new Intent(this, SelectFieldsActivity.class);
                startActivityForResult(intentSelectFields, REQUEST_CODE_SELECT_FIELDS);
                return true;
            case R.id.action_filter:
                mDialog = PreferenceDialog.showFilterRecordCountChoiceDialog(this, mPreferenceChoiceDialogListener);
                return true;
            case R.id.action_reset_filters:
                DialogFragmentFactory.showConfirmDialog(this, getString(R.string.clear_filters_confirm_dialog_title),
                        getString(R.string.clear_filters_confirm_dialog_message), R.id.action_reset_filters, null);
                return true;
            case R.id.action_freeze_header:
                boolean newFreezeHeaderSetting = !NetMonPreferences.getInstance(this).getFreezeHtmlTableHeader();
                NetMonPreferences.getInstance(this).setFreezeHtmlTableHeader(newFreezeHeaderSetting);
                loadHTMLFile();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Read the data from the DB, export it to an HTML file, and load the HTML file in the WebView.
     */
    @SuppressLint("SetJavaScriptEnabled") // only loading local files, it's ok.
    private void loadHTMLFile() {
        Log.v(TAG, "loadHTMLFile");
        final ProgressBar progressBar = findViewById(R.id.progress_bar);
        assert progressBar != null;
        progressBar.setVisibility(View.VISIBLE);
        startRefreshIconAnimation();
        final boolean freezeHeader = NetMonPreferences.getInstance(this).getFreezeHtmlTableHeader();
        final String fixedTableHeight;
        if (freezeHeader) {
            // I've come to the following calculation by trial and error.
            // I've noticed that when entering the web view, the density is equal to the scale/zoom, but I'm
            // not sure if it's the density or zoom which really matters in this calculation.
            // We subtract 100px from the scaled webview height to account for the table header.
            fixedTableHeight = ((mWebView.getHeight() / getResources().getDisplayMetrics().density) - 100) + "px";
        } else {
            fixedTableHeight = null;
        }

        AsyncTask.execute(() -> {
            Log.v(TAG, "loadHTMLFile:doInBackground");
            // Export the DB to the HTML file.
            HTMLExport htmlExport = new HTMLExport(LogActivity.this, false, fixedTableHeight);
            int recordCount = NetMonPreferences.getInstance(LogActivity.this).getFilterRecordCount();
            File result = htmlExport.export(recordCount, null);
            runOnUiThread(() -> {
                Log.v(TAG, "loadHTMLFile:onPostExecute, result=" + result);
                if (isFinishing()) {
                    Log.v(TAG, "finishing, ignoring loadHTMLFile result");
                    return;
                }
                WebView webView = mWebView;
                if (webView == null) {
                    Log.v(TAG, "Must be destroyed or destroying, we have no webview, ignoring loadHTMLFile result");
                    return;
                }
                if (result == null) {
                    Snackbar.make(webView, R.string.error_reading_log, Snackbar.LENGTH_LONG).show();
                    return;
                }
                // Load the exported HTML file into the WebView.
                // Save our current horizontal scroll position so we can keep our
                // horizontal position after reloading the page.
                final int oldScrollX = webView.getScrollX();
                webView.getSettings().setUseWideViewPort(true);
                webView.getSettings().setBuiltInZoomControls(true);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.loadUrl("file://" + result.getAbsolutePath());
                webView.setWebViewClient(new WebViewClient() {

                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        super.onPageStarted(view, url, favicon);
                        Log.v(TAG, "onPageStarted");
                        // Javascript hack to scroll back to our old X position.
                        // http://stackoverflow.com/questions/6855715/maintain-webview-content-scroll-position-on-orientation-change
                        if (oldScrollX > 0) {
                            String jsScrollX = "javascript:window:scrollTo(" + oldScrollX + " / window.devicePixelRatio,0);";
                            view.loadUrl(jsScrollX);
                        }
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        progressBar.setVisibility(View.GONE);
                        stopRefreshIconAnimation();
                    }

                    @Override
                    @TargetApi(Build.VERSION_CODES.N)
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        return loadUrl(request.getUrl().toString()) || super.shouldOverrideUrlLoading(view, request);
                    }

                    @SuppressWarnings("deprecation")
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        return loadUrl(url) || super.shouldOverrideUrlLoading(view, url);
                    }

                    private boolean loadUrl(String url) {
                        Log.v(TAG, "url: " + url);
                        // If the user clicked on one of the column names, let's update
                        // the sorting preference (column name, ascending or descending order).
                        if (url.startsWith(HTMLExport.URL_SORT)) {
                            NetMonPreferences prefs = NetMonPreferences.getInstance(LogActivity.this);
                            SortPreferences oldSortPreferences = prefs.getSortPreferences();
                            // The new column used for sorting will be the one the user tapped on.
                            String newSortColumnName = url.substring(HTMLExport.URL_SORT.length());
                            SortPreferences.SortOrder newSortOrder = oldSortPreferences.sortOrder;
                            // If the user clicked on the column which is already used for sorting,
                            // toggle the sort order between ascending and descending.
                            if (newSortColumnName.equals(oldSortPreferences.sortColumnName)) {
                                if (oldSortPreferences.sortOrder == SortPreferences.SortOrder.DESC) newSortOrder = SortPreferences.SortOrder.ASC;
                                else
                                    newSortOrder = SortPreferences.SortOrder.DESC;
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
                            return false;
                        }
                    }
                });

            });

        });
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        if (mWebView != null) {
            mWebView.getSettings().setDisplayZoomControls(false);
            mWebView.removeAllViews();
            ((ViewGroup) mWebView.getParent()).removeView(mWebView);
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

    @SuppressWarnings("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onDBOperationStarted(NetMonBus.DBOperationStarted event) {
        Log.d(TAG, "onDBOperationStarted() called with " + "event = [" + event + "]");
        mDBOpInProgress = true;
        supportInvalidateOptionsMenu();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onDBOperationEnded(NetMonBus.DBOperationEnded event) {
        Log.d(TAG, "onDBOperationEnded() called with " + "event = [" + event + "]");
        mDBOpInProgress = false;
        supportInvalidateOptionsMenu();
        if (event.isDataChanged) loadHTMLFile();
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
    private final OnSharedPreferenceChangeListener mSharedPreferenceChangeListener = (sharedPreferences, key) -> {
        if (key.equals(NetMonPreferences.PREF_SORT_COLUMN_NAME) || key.equals(NetMonPreferences.PREF_SORT_ORDER)) loadHTMLFile();
    };

    @Override
    public void onOkClicked(int actionId, Bundle extras) {
        // The user confirmed to clear the logs.  Let's do that and refresh the screen.
        if (actionId == R.id.action_reset_filters) {
            NetMonPreferences.getInstance(this).resetColumnFilters();
            loadHTMLFile();
        } else if (actionId == ID_ACTION_CLEAR) {
            Log.v(TAG, "Clicked ok to clear log");
            DBOpIntentService.startActionPurge(this, 0);
        }
    }

    @Override
    public void onCancelClicked(int actionId, Bundle extras) {}

    @Override
    public void onItemSelected(int actionId, int which) {
        // The user picked a file format to export.
        if (actionId == ID_ACTION_SHARE) {
            String[] exportChoices = getResources().getStringArray(R.array.export_choices);
            String selectedShareFormat = exportChoices[which];
            Share.share(this, selectedShareFormat);
        }
    }

    private void startRefreshIconAnimation() {
        Log.v(TAG, "startRefreshIconAnimation");
        if(mMenu == null) return; // This is null when we first enter the activity and the page loads.
        MenuItem menuItemRefresh = mMenu.findItem(R.id.action_refresh);
        if(menuItemRefresh == null) return;
        View refreshIcon = View.inflate(this, R.layout.refresh_icon, null);
        Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        rotation.setRepeatCount(Animation.INFINITE);
        refreshIcon.startAnimation(rotation);
        menuItemRefresh.setActionView(refreshIcon);
    }

    private void stopRefreshIconAnimation() {
        Log.v(TAG, "stopRefreshIconAnimation");
        if(mMenu == null) return;
        MenuItem menuItemRefresh = mMenu.findItem(R.id.action_refresh);
        if(menuItemRefresh == null) return;
        View refreshIcon = menuItemRefresh.getActionView();
        if (refreshIcon != null) {
            refreshIcon.clearAnimation();
            menuItemRefresh.setActionView(null);
        }
    }

}
