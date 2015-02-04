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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.text.TextUtils;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
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
        Cursor c = context.getContentResolver().query(NetMonColumns.CONTENT_URI, null, null, null, NetMonColumns.TIMESTAMP);
        Map<Integer, String> previousRow = null;
        List<Integer> rowIdsToDelete = new ArrayList<Integer>();
        int idLastRow = 0;
        int posLastNewRow = 0;
        if (c != null) {
            try {
                int columnCount = c.getColumnCount();
                // We will not include the _id and _timestamp fields when comparing rows.
                int timestampIndex = c.getColumnIndex(NetMonColumns.TIMESTAMP);
                int idIndex = c.getColumnIndex(BaseColumns._ID);
                while (c.moveToNext()) {
                    int position = c.getPosition();
                    int id = c.getInt(idIndex);
                    Map<Integer, String> currentRow = readRow(c, columnCount, timestampIndex, idIndex);
                    if (previousRow != null) {
                        boolean rowsAreEqual = previousRow.equals(currentRow);
                        if (rowsAreEqual) {
                            // If we've seen at least 3 consecutive identical rows,
                            // delete the previous row.
                            // Ex: if rows 21, 22, 23, and 24 are identical, we'll add
                            // the ids of rows 22 and 23 to the list.
                            if (position - posLastNewRow >= 2) {
                                rowIdsToDelete.add(idLastRow);
                            }
                        } else {
                            posLastNewRow = position;
                        }
                    }
                    idLastRow = id;
                    previousRow = currentRow;
                }
            } finally {
                c.close();
            }
        }
        Log.v(TAG, "compress DB: ids to delete: " + rowIdsToDelete);
        String inClause = TextUtils.join(",", rowIdsToDelete);
        String whereClause = BaseColumns._ID + " in (" + inClause + ")";

        return context.getContentResolver().delete(NetMonColumns.CONTENT_URI, whereClause, null);
    }

    /**
     * @return a map of the row's values: the key is the column index, and the value the string representation of a cell.
     */
    private static Map<Integer, String> readRow(Cursor c, int columnCount, int timestampIndex, int idIndex) {
        Map<Integer, String> result = new HashMap<Integer, String>();
        for (int i = 0; i < columnCount; i++) {
            if (i == timestampIndex || i == idIndex) continue;
            result.put(i, c.getString(i));
        }
        return result;
    }
}
