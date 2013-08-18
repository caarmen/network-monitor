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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.provider.NetMonColumns;

/**
 * Export the Network Monitor data from the DB to a file in a table format.
 */
public abstract class TableFileExport extends FileExport {
    private static final String TAG = Constants.TAG + TableFileExport.class.getSimpleName();

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.US);


    TableFileExport(Context context, File file, FileExport.ExportProgressListener listener) throws FileNotFoundException {
        super(context, file, listener);
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
        Cursor c = mContext.getContentResolver().query(NetMonColumns.CONTENT_URI, null, null, null, NetMonColumns.TIMESTAMP);
        if (c != null) {
            try {
                // We ignore the first column (_id).
                String[] columnNames = c.getColumnNames();
                String[] usedColumnNames = new String[c.getColumnCount() - 1];
                System.arraycopy(columnNames, 1, usedColumnNames, 0, c.getColumnCount() - 1);
                Log.v(TAG, "Find user-friendly labels for columns " + Arrays.toString(usedColumnNames));
                for (int i = 0; i < usedColumnNames.length; i++) {
                    int columnLabelId = mContext.getResources().getIdentifier(usedColumnNames[i], "string", R.class.getPackage().getName());
                    if (columnLabelId > 0) {
                        String columnLabel = mContext.getString(columnLabelId);
                        Log.v(TAG, usedColumnNames[i] + "->" + columnLabel);
                        usedColumnNames[i] = columnLabel;
                    }
                }
                Log.v(TAG, "Column names: " + Arrays.toString(usedColumnNames));


                // Start writing to the file.
                writeHeader(usedColumnNames);

                // Write the table rows to the file.
                if (c.moveToFirst()) {
                    int rowCount = c.getCount();
                    while (c.moveToNext()) {
                        String[] cellValues = new String[c.getColumnCount() - 1];
                        for (int i = 1; i < c.getColumnCount(); i++) {
                            String cellValue;
                            if (NetMonColumns.TIMESTAMP.equals(c.getColumnName(i))) {
                                long timestamp = c.getLong(i);
                                Date date = new Date(timestamp);
                                cellValue = DATE_FORMAT.format(date);
                            } else {
                                cellValue = c.getString(i);
                            }
                            if (cellValue == null) cellValue = "";
                            cellValues[i - 1] = cellValue;
                        }
                        writeRow(c.getPosition(), cellValues);
                        if (mListener != null) mListener.onExportProgress(c.getPosition(), rowCount);
                    }
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
