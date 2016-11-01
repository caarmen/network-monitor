/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2015 Carmen Alvarez (c@rmen.ca)
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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dialog.ConfirmDialogFragment;
import ca.rmen.android.networkmonitor.app.dialog.DialogFragmentFactory;
import ca.rmen.android.networkmonitor.app.prefs.SelectFieldsFragment.SelectFieldsFragmentListener;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.Log;
import ca.rmen.android.networkmonitor.util.PermissionUtil;
import ca.rmen.android.networkmonitor.util.TextUtil;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class SelectFieldsActivity extends AppCompatActivity
        implements SelectFieldsFragmentListener,
        ConfirmDialogFragment.DialogButtonListener {
    private static final String TAG = Constants.TAG + SelectFieldsActivity.class.getSimpleName();
    private static final int ACTION_REQUEST_PHONE_STATE_PERMISSION = 1;
    private static final int ACTION_REQUEST_USAGE_PERMISSION = 2;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_fields);
        ListFragment lvf = (ListFragment) getSupportFragmentManager().findFragmentById(R.id.listFragment);
        mListView = lvf.getListView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.select_fields, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        View okButton = findViewById(R.id.ok);
        assert okButton != null;
        switch (item.getItemId()) {
            case R.id.action_select_all:
                for (int i = 0; i < mListView.getCount(); i++)
                    mListView.setItemChecked(i, true);
                okButton.setEnabled(true);
                break;
            case R.id.action_select_none:
                for (int i = 0; i < mListView.getCount(); i++)
                    mListView.setItemChecked(i, false);
                okButton.setEnabled(false);
                break;
            case R.id.action_select_profile_wifi:
                selectColumns(getResources().getStringArray(R.array.db_columns_profile_wifi));
                okButton.setEnabled(true);
                break;
            case R.id.action_select_profile_mobile_gsm:
                selectColumns(getResources().getStringArray(R.array.db_columns_profile_mobile_gsm));
                okButton.setEnabled(true);
                break;
            case R.id.action_select_profile_mobile_cdma:
                selectColumns(getResources().getStringArray(R.array.db_columns_profile_mobile_cdma));
                okButton.setEnabled(true);
                break;
            case R.id.action_select_profile_location:
                selectColumns(getResources().getStringArray(R.array.db_columns_profile_location));
                okButton.setEnabled(true);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void selectColumns(String[] columnNames) {
        for (int i = 0; i < mListView.getCount(); i++)
            mListView.setItemChecked(i, false);
        @SuppressWarnings("unchecked")
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) mListView.getAdapter();
        for (String columnName : columnNames) {
            String columnLabel = NetMonColumns.getColumnLabel(this, columnName);
            int position = adapter.getPosition(columnLabel);
            mListView.setItemChecked(position, true);
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void onCancel(View v) {
        Log.v(TAG, "onCancel");
        finish();
    }

    @SuppressWarnings("UnusedParameters")
    public void onOk(View v) {
        Log.v(TAG, "onOk");
        SparseBooleanArray checkedPositions = mListView.getCheckedItemPositions();
        String[] dbColumns = NetMonColumns.getColumnNames(this);
        final List<String> selectedColumns = new ArrayList<>(dbColumns.length);
        for (int i = 0; i < dbColumns.length; i++) {
            if (checkedPositions.get(i)) selectedColumns.add(dbColumns[i]);
        }
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                NetMonPreferences.getInstance(SelectFieldsActivity.this).setSelectedColumns(selectedColumns);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                setResult(Activity.RESULT_OK);
                finish();
            }
        }.execute();
    }

    @Override
    public void onListItemClick(ListView l, int position) {
        Log.v(TAG, "onListItemClick: clicked on position " + position);
        View okButton = findViewById(R.id.ok);
        assert okButton != null;
        okButton.setEnabled(false);
        SparseBooleanArray checkedItemPositions = l.getCheckedItemPositions();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            if (checkedItemPositions.get(checkedItemPositions.keyAt(i))) {
                okButton.setEnabled(true);
                break;
            }
        }

        // We require permissions to collect data for some columns.
        // First we need the READ_PHONE_STATE permission, which is managed like typical
        // M dangerous permissions.
        // Then we need the PACKAGE_USAGE_STATS permission, which we can only obtain by asking
        // the user to grant us access in a specific system settings screen (Security -> Data Usage)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkedItemPositions.get(position)) {
                SelectFieldsFragment.SelectedField selectedField
                        = (SelectFieldsFragment.SelectedField) l.getAdapter().getItem(position);
                if (selectedField.dbName.equals(NetMonColumns.MOST_CONSUMING_APP_NAME)
                        || selectedField.dbName.equals(NetMonColumns.MOST_CONSUMING_APP_BYTES)) {
                    requestPhoneStatePermission();
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestPhoneStatePermission() {
        if (!PermissionUtil.hasReadPhoneStatePermission(this)) {
            DialogFragmentFactory.showConfirmDialog(
                    this,
                    getString(R.string.permission_data_usage_permission_title),
                    TextUtil.fromHtml(getString(R.string.permission_read_phone_state_message)),
                    ACTION_REQUEST_PHONE_STATE_PERMISSION,
                    null);
        } else if (!PermissionUtil.hasUsageStatsPermission(this)) {
            requestUsagePermission();
        }
    }

    @NeedsPermission({Manifest.permission.READ_PHONE_STATE})
    @TargetApi(Build.VERSION_CODES.M)
    void requestUsagePermission() {
        Log.v(TAG, "Read phone state permission granted");
        if (PermissionUtil.hasUsageStatsPermission(this)) {
            Log.v(TAG, "Already have the usage stats permission");
        } else {
            // Need to post it.  Otherwise we end up with one of those exceptions about doing stuff
            // after onSaveInstanceState was called.
            new Handler().post(new Runnable(){
                @Override
                public void run() {
                    DialogFragmentFactory.showConfirmDialog(
                            SelectFieldsActivity.this,
                            getString(R.string.permission_data_usage_permission_title),
                            TextUtil.fromHtml(getString(R.string.permission_data_usage_message)),
                            ACTION_REQUEST_USAGE_PERMISSION,
                            null);
                }
            });
        }
    }

    @OnPermissionDenied({Manifest.permission.READ_PHONE_STATE})
    @TargetApi(Build.VERSION_CODES.M)
    void onPermissionsDenied() {
        Snackbar.make(getWindow().getDecorView().getRootView(), R.string.permission_data_usage_denied, Snackbar.LENGTH_LONG).show();
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        SelectFieldsActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    public void onOkClicked(int actionId, Bundle extras) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (actionId == ACTION_REQUEST_PHONE_STATE_PERMISSION) {
                SelectFieldsActivityPermissionsDispatcher.requestUsagePermissionWithCheck(this);
            } else if (actionId == ACTION_REQUEST_USAGE_PERMISSION) {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            }
        }
    }

    @Override
    public void onCancelClicked(int actionId, Bundle extras) {
        Snackbar.make(getWindow().getDecorView().getRootView(), R.string.permission_data_usage_denied, Snackbar.LENGTH_LONG).show();
    }
}
