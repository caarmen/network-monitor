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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import java.io.File;

import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.util.IoUtil;
import ca.rmen.android.networkmonitor.util.Log;

public class SaveToStorageActivity extends FragmentActivity {
    private static final String TAG = SaveToStorageActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate: bundle="+savedInstanceState);

        Parcelable extra = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        if (extra == null || !(extra instanceof Uri)) {
            Toast.makeText(this, getString(R.string.export_save_to_external_storage_fail), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Uri sourceFileUri = (Uri) extra;
        if (!"file".equals(sourceFileUri.getScheme())) {
            Toast.makeText(this, getString(R.string.export_save_to_external_storage_fail), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        saveFile(new File(sourceFileUri.getPath()));
    }

    private void saveFile(File file) {
        new AsyncTask<File, Void, File>() {

            @Override
            protected File doInBackground(File... files) {
                if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
                    return null;
                File folder = Environment.getExternalStorageDirectory();
                File src = files[0];
                File dest = new File(folder, src.getName());
                if(IoUtil.copy(src, dest)) return dest;
                return null;
            }

            @Override
            protected void onPostExecute(File copiedFile) {
                if(copiedFile != null)
                    Toast.makeText(getApplicationContext(), getString(R.string.export_save_to_external_storage_success, copiedFile.getAbsolutePath()), Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(getApplicationContext(), R.string.export_save_to_external_storage_fail, Toast.LENGTH_LONG).show();
                finish();
            }
        }.execute(file);
    }
}
