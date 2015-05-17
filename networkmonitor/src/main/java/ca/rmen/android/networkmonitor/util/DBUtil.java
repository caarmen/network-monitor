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
package ca.rmen.android.networkmonitor.util;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;

import ca.rmen.android.networkmonitor.provider.NetMonColumns;

public final class DBUtil {

    private DBUtil() {
        // prevent instantiation of a utility class
    }

    /**
     * @return the most recent value we logged for the given columnName, which matches the given selection.  May return null.
     */
    public static String readLastLoggedValue(Context context, String columnName, String selection) {
        String[] projection = new String[]{columnName};
        String orderBy = BaseColumns._ID + " DESC";
        Cursor cursor = context.getContentResolver().query(NetMonColumns.CONTENT_URI, projection, selection, null, orderBy);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

}
