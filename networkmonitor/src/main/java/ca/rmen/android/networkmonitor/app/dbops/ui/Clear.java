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

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.backend.clean.DBPurge;
import ca.rmen.android.networkmonitor.util.Log;

/**
 *
 */
public class Clear {

    private static final String TAG = Constants.TAG + Clear.class.getSimpleName();

    /**
     * Deletes rows from the database, keeping only the given number of rows.
     *
     * @param activity a progress dialog will be displayed in this activity
     * @param numRowsToKeep delete all but the most recent numRowToKeep rows. 0 to delete all rows.
     */
    public static void clear(final FragmentActivity activity, int numRowsToKeep) {
        Log.v(TAG, "clear, keeping " + numRowsToKeep + " records");
        DBPurge dbPurge = new DBPurge(activity, numRowsToKeep);
        Bundle bundle = new Bundle(1);
        bundle.putInt(DBOpAsyncTask.EXTRA_DIALOG_STYLE, ProgressDialog.STYLE_SPINNER);
        new DBOpAsyncTask<Integer>(activity, dbPurge, bundle) {

            @Override
            protected void onPostExecute(Integer result) {
                // Once the DB is deleted, reload the WebView.
                if (result > 0) Toast.makeText(activity, activity.getString(R.string.compress_successful, result), Toast.LENGTH_LONG).show();
                activity.setResult(Activity.RESULT_OK);
                super.onPostExecute(result);
            }
        }.execute();

    }

}
