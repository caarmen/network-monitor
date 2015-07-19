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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import java.io.File;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.app.dialog.DialogFragmentFactory;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * This activity handles the action {@link Intent#ACTION_GET_CONTENT}.  It displays a file chooser
 * dialog.  If the user selects a file, this activity sets the selected file as the result Intent data.
 */
public class FileChooserActivity extends FragmentActivity implements FileChooserDialogFragment.FileChooserDialogListener {
    private static final String TAG = Constants.TAG + FileChooserActivity.class.getSimpleName();
    private static final int ACTION_CHOOSE_FILE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate: bundle=" + savedInstanceState);
        if(savedInstanceState == null) {
            File initialFolder = NetMonPreferences.getInstance(this).getImportFolder();
            DialogFragmentFactory.showFileChooserDialog(this, initialFolder, false, ACTION_CHOOSE_FILE);
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

}
