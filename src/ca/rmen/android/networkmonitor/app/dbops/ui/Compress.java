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
package ca.rmen.android.networkmonitor.app.dbops.ui;

import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.backend.clean.DBCompress;
import ca.rmen.android.networkmonitor.util.Log;

public class Compress {
    private static final String TAG = Constants.TAG + Compress.class.getSimpleName();


    /**
     * Compress the database. The progress will be shown in a progress dialog on the given activity.
     */
    public static void compress(final FragmentActivity activity) {
        Log.v(TAG, "compress");
        DBCompress dbCompress = new DBCompress(activity);
        DBOpAsyncTask<Integer> task = new DBOpAsyncTask<Integer>(activity, dbCompress, null) {

            @Override
            protected void onPostExecute(Integer result) {
                String toastText = result >= 0 ? activity.getString(R.string.compress_successful, result) : activity.getString(R.string.compress_failed);
                Toast.makeText(activity, toastText, Toast.LENGTH_SHORT).show();
                super.onPostExecute(result);
            }
        };
        task.execute();

    }

}
