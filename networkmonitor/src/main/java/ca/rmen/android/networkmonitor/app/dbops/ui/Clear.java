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

import android.content.Context;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.app.dbops.backend.DBOpIntentService;
import ca.rmen.android.networkmonitor.util.Log;

/**
 *
 */
public class Clear {

    private static final String TAG = Constants.TAG + Clear.class.getSimpleName();

    /**
     * Deletes rows from the database, keeping only the given number of rows.
     *
     * @param numRowsToKeep delete all but the most recent numRowToKeep rows. 0 to delete all rows.
     */
    public static void clear(final Context context, int numRowsToKeep) {
        Log.v(TAG, "clear, keeping " + numRowsToKeep + " records");
        DBOpIntentService.startActionPurge(context, numRowsToKeep);
    }

}
