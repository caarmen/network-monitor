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
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;

import ca.rmen.android.networkmonitor.util.Log;

/**
 * Dummy invisible activity which just launches the {@link SaveToStorageService} service
 * and exits immediately.
 */
public class SaveToStorageActivity extends FragmentActivity {
    private static final String TAG = SaveToStorageActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate: bundle=" + savedInstanceState);
        Intent intent = new Intent(this, SaveToStorageService.class);

        Parcelable extra = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        intent.putExtra(Intent.EXTRA_STREAM, extra);
        startService(intent);
        finish();
    }

}
