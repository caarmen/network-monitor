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

import android.app.IntentService;
import android.content.Intent;

import java.io.File;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.util.IoUtil;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Reads the file in the {@link #EXTRA_SOURCE_FILE} File extra, and saves
 * a copy of that file to the {@link #EXTRA_DESTINATION_FILE} File extra. The destination may
 * be a file or a folder.
 */
public class SaveToStorageService extends IntentService {
    private static final String TAG = Constants.TAG + SaveToStorageService.class.getSimpleName();
    public static final String EXTRA_SOURCE_FILE = "source_file";
    public static final String EXTRA_DESTINATION_FILE = "destination_file";

    public SaveToStorageService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "onHandleIntent: intent = " + intent);

        File src = (File) intent.getSerializableExtra(EXTRA_SOURCE_FILE);
        File dest = (File) intent.getSerializableExtra(EXTRA_DESTINATION_FILE);
        if (IoUtil.copy(src, dest))
            SaveToStorage.displaySuccessToast(getApplicationContext(), dest);
        else
            SaveToStorage.displayErrorToast(getApplicationContext());
    }

}
