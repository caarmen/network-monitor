/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013 Carmen Alvarez (c@rmen.ca)
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
package org.jraf.android.networkmonitor.app.export;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import android.content.Context;
import android.database.Cursor;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.app.export.FormatterFactory.FormatterStyle;
import org.jraf.android.networkmonitor.app.prefs.NetMonPreferences;
import org.jraf.android.networkmonitor.provider.NetMonColumns;
import org.jraf.android.networkmonitor.util.Log;

/**
 * Export the Network Monitor data from the DB to a file in a table format.
 */
abstract class TableFileExport extends FileExport {
    private static final String TAG = Constants.TAG + TableFileExport.class.getSimpleName();
    private final FormatterStyle mFormatterStyle;

    TableFileExport(Context context, File file, FormatterStyle formatterStyle, FileExport.ExportProgressListener listener) throws FileNotFoundException {
        super(context, file, listener);
        mFormatterStyle = formatterStyle;
    }

    /**
     * Do any preparation for the export, including writing the header with the
     * column names.
     */
    abstract void writeHeader(String[] columnNames) throws IOException;

    /**
     * Write a single row to the file.
     */
    abstract void writeRow(int rowNumber, String[] cellValues) throws IOException;

    /**
     * Write the footer (if any) and do any cleanup after the export.
     */
    abstract void writeFooter() throws IOException;


    /**
     * @return the file if it was correctly exported, null otherwise.
     */
    @Override
    public File export() {
        Log.v(TAG, "export");
        return export(0);
    }

    /**
     * @param recordCount export at most this number of records. If recordCount is 0 or less, all records will be exported.
     * @return the file if it was correctly exported, null otherwise.
     */
    public File export(int recordCount) {
        Log.v(TAG, "export " + (recordCount <= 0 ? "all" : recordCount) + " records");
        String[] usedColumnNames = (String[]) NetMonPreferences.getInstance(mContext).getSelectedColumns().toArray();
        Formatter formatter = FormatterFactory.getFormatter(mFormatterStyle, mContext);
        Cursor c = mContext.getContentResolver().query(NetMonColumns.CONTENT_URI, usedColumnNames, null, null, NetMonColumns.TIMESTAMP + " DESC");
        if (c != null) {
            try {
                for (int i = 0; i < usedColumnNames.length; i++)
                    usedColumnNames[i] = NetMonColumns.getColumnLabel(mContext, usedColumnNames[i]);
                Log.v(TAG, "Column names: " + Arrays.toString(usedColumnNames));

                // Start writing to the file.
                writeHeader(usedColumnNames);

                // Write the table rows to the file.
                int rowsAvailable = c.getCount();
                // Check if we're supposed to limit the number of rows exported.
                int rowsToExport = recordCount > 0 ? Math.min(recordCount, rowsAvailable) : rowsAvailable;
                while (c.moveToNext() && c.getPosition() < rowsToExport) {
                    String[] cellValues = new String[c.getColumnCount()];
                    for (int i = 0; i < c.getColumnCount(); i++)
                        cellValues[i] = formatter.format(c, i);
                    writeRow(c.getPosition(), cellValues);
                    // Notify the listener of our progress (progress is 1-based)
                    if (mListener != null) mListener.onExportProgress(c.getPosition() + 1, rowsToExport);
                }

                // Write the footer and clean up the file.
                writeFooter();
                return mFile;
            } catch (IOException e) {
                Log.e(TAG, "export Could not export file " + mFile + ": " + e.getMessage(), e);
            } finally {
                c.close();
            }
        }
        return null;
    }

}
