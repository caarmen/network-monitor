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
package ca.rmen.android.networkmonitor.app.savetostorage;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;

import java.io.File;

import ca.rmen.android.networkmonitor.app.dialog.DialogFragmentFactory;
import ca.rmen.android.networkmonitor.app.dialog.filechooser.FileChooserDialogFragment;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Dummy invisible activity which just launches the {@link SaveToStorageService} service
 * and exits immediately.
 */
public class SaveToStorageActivity extends FragmentActivity implements FileChooserDialogFragment.FileChooserDialogListener {
    private static final String TAG = SaveToStorageActivity.class.getSimpleName();
    private static final int ACTION_SAVE_TO_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate: bundle=" + savedInstanceState);
        if(savedInstanceState == null) {
            Parcelable extra = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            if (extra == null || !(extra instanceof Uri)) {
                SaveToStorage.displayErrorToast(this);
                return;
            }

            Uri sourceFileUri = (Uri) extra;
            if (!"file".equals(sourceFileUri.getScheme())) {
                SaveToStorage.displayErrorToast(this);
                return;
            }

            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                SaveToStorage.displayErrorToast(this);
                return;
            }
            File initialFolder = NetMonPreferences.getInstance(this).getExportFolder();
            DialogFragmentFactory.showFileChooserDialog(this, initialFolder, true, ACTION_SAVE_TO_STORAGE);
        }

    }

    @Override
    public void onFileSelected(int actionId, File file) {
        Log.v(TAG, "onFileSelected: file = " + file);
        NetMonPreferences.getInstance(this).setExportFolder(file);
        Uri uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        File sourceFile = new File(uri.getPath());

        // If the user picked a file, we'll save to that file.
        // If the user picked a folder, we'll save in that folder, with the original file name.
        final File destFile;
        if(file.isDirectory()) destFile = new File(file, sourceFile.getName());
        else destFile = file;

        Intent intent = new Intent(this, SaveToStorageService.class);
        intent.putExtra(SaveToStorageService.EXTRA_SOURCE_FILE, sourceFile);
        intent.putExtra(SaveToStorageService.EXTRA_DESTINATION_FILE, destFile);
        startService(intent);
        finish();
    }

    @Override
    public void onDismiss(int actionId) {
        Log.v(TAG, "onDismiss");
        finish();
    }

}
