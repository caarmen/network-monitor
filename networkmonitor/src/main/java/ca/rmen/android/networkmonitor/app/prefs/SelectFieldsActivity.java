/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2020 Carmen Alvarez (c@rmen.ca)
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dialog.ConfirmDialogFragment;
import ca.rmen.android.networkmonitor.app.dialog.DialogFragmentFactory;
import ca.rmen.android.networkmonitor.databinding.SelectFieldsBinding;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.PermissionUtil;
import ca.rmen.android.networkmonitor.util.TextUtil;

public class SelectFieldsActivity extends AppCompatActivity
        implements ConfirmDialogFragment.DialogButtonListener {
    private static final String TAG = Constants.TAG + SelectFieldsActivity.class.getSimpleName();
    private static final int ACTION_REQUEST_PHONE_STATE_PERMISSION = 1;
    private static final int ACTION_REQUEST_USAGE_PERMISSION = 2;
    private static final int PERMISSION_PHONE_STATE_REQUEST_CODE = 9401;
    private SelectedFieldsAdapter mSelectFieldsAdapter;
    private SelectFieldsBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.select_fields);
        mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        mSelectFieldsAdapter = new SelectedFieldsAdapter(this);
        mBinding.recyclerView.setAdapter(mSelectFieldsAdapter);
        mSelectFieldsAdapter.registerAdapterDataObserver(mListener);
    }

    @Override
    protected void onDestroy() {
        mSelectFieldsAdapter.unregisterAdapterDataObserver(mListener);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.select_fields, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_select_all:
                mSelectFieldsAdapter.selectAll();
                mBinding.okCancelBar.ok.setEnabled(true);
                break;
            case R.id.action_select_none:
                mSelectFieldsAdapter.selectNone();
                mBinding.okCancelBar.ok.setEnabled(false);
                break;
            case R.id.action_select_profile_wifi:
                mSelectFieldsAdapter.selectColumns(getResources().getStringArray(R.array.db_columns_profile_wifi));
                mBinding.okCancelBar.ok.setEnabled(true);
                break;
            case R.id.action_select_profile_mobile_gsm:
                mSelectFieldsAdapter.selectColumns(getResources().getStringArray(R.array.db_columns_profile_mobile_gsm));
                mBinding.okCancelBar.ok.setEnabled(true);
                break;
            case R.id.action_select_profile_mobile_cdma:
                mSelectFieldsAdapter.selectColumns(getResources().getStringArray(R.array.db_columns_profile_mobile_cdma));
                mBinding.okCancelBar.ok.setEnabled(true);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @SuppressWarnings("UnusedParameters")
    public void onCancel(View v) {
        Log.v(TAG, "onCancel");
        finish();
    }

    @SuppressWarnings("UnusedParameters")
    public void onOk(View v) {
        Log.v(TAG, "onOk");
        final List<String> selectedColumns = mSelectFieldsAdapter.getSelectedColumns();
        AsyncTask.execute(() -> {
            NetMonPreferences.getInstance(this).setSelectedColumns(selectedColumns);
            runOnUiThread(() -> {
                setResult(Activity.RESULT_OK);
                finish();
            });
        });
    }

    private final RecyclerView.AdapterDataObserver mListener = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            Log.v(TAG, "onChanged");
            List<String> selectedColumns = mSelectFieldsAdapter.getSelectedColumns();
            mBinding.okCancelBar.ok.setEnabled(!selectedColumns.isEmpty());

            // We require permissions to collect data for some columns.
            // First we need the READ_PHONE_STATE permission, which is managed like typical
            // M dangerous permissions.
            // Then we need the PACKAGE_USAGE_STATS permission, which we can only obtain by asking
            // the user to grant us access in a specific system settings screen (Security -> Data Usage)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (selectedColumns.contains(NetMonColumns.MOST_CONSUMING_APP_NAME)
                        || selectedColumns.contains(NetMonColumns.MOST_CONSUMING_APP_BYTES)) {
                    requestPhoneStatePermission();
                }
            }
        }
    };


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

    @TargetApi(Build.VERSION_CODES.M)
    void requestUsagePermission() {
        Log.v(TAG, "Read phone state permission granted");
        if (PermissionUtil.hasUsageStatsPermission(this)) {
            Log.v(TAG, "Already have the usage stats permission");
        } else {
            // Need to post it.  Otherwise we end up with one of those exceptions about doing stuff
            // after onSaveInstanceState was called.
            new Handler().post(() -> DialogFragmentFactory.showConfirmDialog(
                    SelectFieldsActivity.this,
                    getString(R.string.permission_data_usage_permission_title),
                    TextUtil.fromHtml(getString(R.string.permission_data_usage_message)),
                    ACTION_REQUEST_USAGE_PERMISSION,
                    null));
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    void onPermissionsDenied() {
        Snackbar.make(getWindow().getDecorView().getRootView(), R.string.permission_data_usage_denied, Snackbar.LENGTH_LONG).show();
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PERMISSION_PHONE_STATE_REQUEST_CODE == requestCode) {
            if (PermissionUtil.areAllGranted(grantResults)) {
                requestUsagePermission();
            } else {
                onPermissionsDenied();
            }
        }
    }

    @Override
    public void onOkClicked(int actionId, Bundle extras) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (actionId == ACTION_REQUEST_PHONE_STATE_PERMISSION) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_PHONE_STATE_REQUEST_CODE);
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
