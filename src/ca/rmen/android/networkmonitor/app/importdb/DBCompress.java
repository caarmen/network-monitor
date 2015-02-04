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
package ca.rmen.android.networkmonitor.app.importdb;

import java.io.IOException;

import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Reduces groups of 3 or more consecutive rows with identical data (except the timestamp) into a single row.
 */
public class DBCompress {
    private static final String TAG = Constants.TAG + "/" + DBCompress.class.getSimpleName();

    /**
     * @return the number of rows deleted from the database
     */
    public static int compressDB(Context context) throws RemoteException, OperationApplicationException, IOException {
        Log.v(TAG, "compress DB");
        return 0;
    }
}
