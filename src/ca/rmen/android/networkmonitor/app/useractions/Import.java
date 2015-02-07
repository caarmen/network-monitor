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
package ca.rmen.android.networkmonitor.app.useractions;

import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.db.DBImport;
import ca.rmen.android.networkmonitor.app.main.NetMonAsyncTask;
import ca.rmen.android.networkmonitor.util.Log;

/**
 *
 */
public class Import {
    private static final String TAG = Constants.TAG + Import.class.getSimpleName();

    /**
     * Import a database file.
     *
     * @param activity A progress dialog will appear on the activity during the import
     * @param uri the location of the db to import
     */
    public static void importDb(final FragmentActivity activity, final Uri uri) {
        Log.v(TAG, "importDb: uri = " + uri);
        DBImport dbImport = new DBImport(activity, uri);
        NetMonAsyncTask<Boolean> task = new NetMonAsyncTask<Boolean>(activity, dbImport, null) {

            @Override
            protected void onPostExecute(Boolean result) {
                String toastText = result ? activity.getString(R.string.import_successful, uri.getPath()) : activity.getString(R.string.import_failed,
                        uri.getPath());
                Toast.makeText(activity, toastText, Toast.LENGTH_SHORT).show();
                super.onPostExecute(result);
            }
        };
        task.execute();

    }
}
