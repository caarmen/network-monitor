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
package ca.rmen.android.networkmonitor.app.dbops.backend.clean;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.ProgressListener;
import ca.rmen.android.networkmonitor.app.dbops.backend.DBOperation;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import android.util.Log;

/**
 * Reduces groups of 3 or more consecutive rows with identical data (except the timestamp) into a single row.
 */
public class DBCompress implements DBOperation {
    private static final String TAG = Constants.TAG + DBCompress.class.getSimpleName();

    private final Context mContext;
    private final AtomicBoolean mIsCanceled = new AtomicBoolean(false);

    public DBCompress(Context context) {
        mContext = context;
    }

    @Override
    public void execute(ProgressListener listener) {
        Log.v(TAG, "compress DB");
        Cursor c = mContext.getContentResolver().query(NetMonColumns.CONTENT_URI, null, null, null, BaseColumns._ID);
        SparseArray<String> previousRow = null;
        List<Integer> rowIdsToDelete = new ArrayList<>();
        int idLastRow = 0;
        int posLastNewRow = 0;
        if (c != null) {
            try {
                int rowCount = c.getCount();
                int columnCount = c.getColumnCount();
                // We will not include the _id and _timestamp fields when comparing rows.
                int timestampIndex = c.getColumnIndex(NetMonColumns.TIMESTAMP);
                int idIndex = c.getColumnIndex(BaseColumns._ID);
                while (c.moveToNext() && !mIsCanceled.get()) {
                    int position = c.getPosition();
                    int id = c.getInt(idIndex);
                    SparseArray<String> currentRow = readRow(c, columnCount, timestampIndex, idIndex);
                    if (previousRow != null) {
                        boolean rowsAreEqual = areEqual(previousRow, currentRow);
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
                    if (listener != null) listener.onProgress(position, rowCount);
                    idLastRow = id;
                    previousRow = currentRow;
                }
            } finally {
                c.close();
            }
        }
        int numRowsDeleted = 0;
        int numRowsToDelete = rowIdsToDelete.size();
        Log.v(TAG, "compress DB: " + numRowsToDelete + " rows to delete");
        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < numRowsToDelete; i++) {
            inClause.append(rowIdsToDelete.get(i));
            if (i % 100 == 0 || i == numRowsToDelete - 1) {
                String whereClause = BaseColumns._ID + " in (" + inClause + ")";
                numRowsDeleted += mContext.getContentResolver().delete(NetMonColumns.CONTENT_URI, whereClause, null);
                Log.v(TAG, "compress DB: deleted " + numRowsDeleted + " rows");
                inClause = new StringBuilder();

            } else {
                inClause.append(",");
            }
        }
        if (listener != null) {
            if (numRowsToDelete >= 0) {
                if(mIsCanceled.get())
                    listener.onError(mContext.getString(R.string.compress_notif_canceled_content));
                else
                    listener.onComplete(mContext.getResources().getQuantityString(R.plurals.compress_notif_complete_content, numRowsDeleted, numRowsDeleted));
            }
            else {
                listener.onError(mContext.getString(R.string.compress_notif_error_content));
            }
        }
    }

    private static boolean areEqual(SparseArray<String> o1, SparseArray<String> o2) {
        if (o1.size() != o2.size()) return false;
        for (int i = 0; i < o1.size(); i++) {
            if (o1.keyAt(i) != (o2.keyAt(i))) return false;
            if (!TextUtils.equals(o1.get(i), o2.get(i))) return false;
        }
        return true;
    }

    @Override
    public void cancel() {
        mIsCanceled.set(true);
    }

    /**
     * @return a map of the row's values: the key is the column index, and the value the string representation of a cell.
     */
    private static SparseArray<String> readRow(Cursor c, int columnCount, int timestampIndex, int idIndex) {
        SparseArray<String> result = new SparseArray<>();
        for (int i = 0; i < columnCount; i++) {
            if (i == timestampIndex || i == idIndex) continue;
            result.put(i, c.getString(i));
        }
        return result;
    }
}
