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
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.rmen.android.networkmonitor.app.dialog.filechooser;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;

import java.io.File;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dialog.DialogFragmentFactory;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.util.Log;
import ca.rmen.android.networkmonitor.util.PermissionUtil;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

/**
 * This invisible activity handles the action {@link Intent#ACTION_GET_CONTENT}.  It displays a file chooser
 * dialog.  If the user selects a file, this activity sets the selected file as the result Intent data.
 */
@RuntimePermissions
public class FileChooserActivity extends FragmentActivity implements FileChooserDialogFragment.FileChooserDialogListener {
    private static final String TAG = Constants.TAG + FileChooserActivity.class.getSimpleName();
    private static final int ACTION_CHOOSE_FILE = 1;

    private boolean mPermissionRequestingDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate: bundle=" + savedInstanceState);
        if (savedInstanceState == null) {
            mPermissionRequestingDone = false;
            FileChooserActivityPermissionsDispatcher.requestPermissionWithCheck(this);
        } else {
            mPermissionRequestingDone = true;
        }
    }

    @Override
    public void onFileSelected(int actionId, File file) {
        Log.v(TAG, "onFileSelected: file = " + file);
        NetMonPreferences.getInstance(this).setImportFolder(file);
        Intent data = new Intent();
        data.setData(Uri.fromFile(file));
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onDismiss(int actionId) {
        Log.v(TAG, "onDismiss");
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume");
        super.onResume();
        if (mPermissionRequestingDone) {
            showFileChooserDialog();
        }
    }

    private void showFileChooserDialog() {
        Log.v(TAG, "showFileChooserDialog");
        File initialFolder = getExternalFilesDir(null);
        if (PermissionUtil.hasExternalStoragePermission(this)) {
            initialFolder = NetMonPreferences.getInstance(this).getImportFolder();
        }
        DialogFragmentFactory.showFileChooserDialog(this, initialFolder, false, ACTION_CHOOSE_FILE);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    @OnPermissionDenied({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void requestPermission() {
        Log.v(TAG, "Permissions granted");
        mPermissionRequestingDone = true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @OnShowRationale({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void showRationaleForPermissions(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.permission_external_storage_rationale)
                .setPositiveButton(R.string.permission_button_allow, (dialogInterface, i) -> request.proceed())
                .setNegativeButton(R.string.permission_button_deny, (dialogInterface, i) -> {
                    request.cancel();
                    showFileChooserDialog();
                }).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        FileChooserActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }
}
