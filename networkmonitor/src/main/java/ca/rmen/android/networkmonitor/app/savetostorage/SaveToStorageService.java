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
 * Copyright (C) 2015-2017 Carmen Alvarez (c@rmen.ca)
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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.util.IoUtil;
import android.util.Log;

/**
 * Reads the file in the {@link #EXTRA_SOURCE_URI} Uri extra, and saves
 * a copy of that file to the {@link #EXTRA_DESTINATION_URI} Uri extra.
 */
public class SaveToStorageService extends JobIntentService {
    private static final String TAG = Constants.TAG + SaveToStorageService.class.getSimpleName();
    public static final String EXTRA_SOURCE_URI = "source_uri";
    public static final String EXTRA_DESTINATION_URI = "destination_uri";

    private static final int JOB_ID = 8788;

    static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, SaveToStorageService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.v(TAG, "onHandleIntent: intent = " + intent);

        Uri src = intent.getParcelableExtra(EXTRA_SOURCE_URI);
        Uri dest = intent.getParcelableExtra(EXTRA_DESTINATION_URI);
        if (IoUtil.copy(this, src, dest))
            SaveToStorage.displaySuccessToast(getApplicationContext(), dest);
        else
            SaveToStorage.displayErrorToast(getApplicationContext());
    }

}
