/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 * 
 * Copyright (C) 2015-2019 Carmen Alvarez (c@rmen.ca)
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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;
import ca.rmen.android.networkmonitor.Constants;

/**
 * Invisible activity which expects a file as a Uri in the {@link Intent#EXTRA_STREAM} extra.
 * This activity shows a folder chooser dialog to the user. Once the folder is
 * selected, this launches the {@link SaveToStorageService} service to save the file in
 * the selected folder, and exits without waiting for the service to complete.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class SaveToStorageActivity extends FragmentActivity {
    private static final String TAG = Constants.TAG + SaveToStorageActivity.class.getSimpleName();
    private static final int ACTION_SAVE_TO_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate: bundle=" + savedInstanceState);
        Parcelable extra = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        if (!(extra instanceof Uri)) {
            SaveToStorage.displayErrorToast(this);
            return;
        }
        Uri sourceFileUri = (Uri) extra;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(getIntent().getType());
        final String fileName = sourceFileUri.getLastPathSegment();
        if (!TextUtils.isEmpty(fileName)) intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, ACTION_SAVE_TO_STORAGE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "onActivityResult: resultCode = " + resultCode + ", data=" + data);
        if (requestCode == ACTION_SAVE_TO_STORAGE) {
            if (resultCode == Activity.RESULT_OK) {
                Intent intent = new Intent(this, SaveToStorageService.class);
                Uri sourceUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
                intent.putExtra(SaveToStorageService.EXTRA_SOURCE_URI, sourceUri);
                intent.putExtra(SaveToStorageService.EXTRA_DESTINATION_URI, data.getData());
                SaveToStorageService.enqueueWork(this, intent);
            }
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
