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
package org.jraf.android.networkmonitor.app.export.kml;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.app.export.FileExport;
import org.jraf.android.networkmonitor.provider.NetMonColumns;

/**
 * Export the Network Monitor data to a KML file. The KML file placemark icon label and color depend on the field the user chose to export.
 */
public class KMLExport extends FileExport {
    private static final String TAG = KMLExport.class.getSimpleName();
    private static final String KML_FILE = "networkmonitor.kml";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.US);

    // The field which determines the name/label of the KML placemarks we will export.i
    private final String mPlacemarkNameColumn;

    /**
     */
    public KMLExport(Context context, FileExport.ExportProgressListener listener, String placemarkNameColumn) throws FileNotFoundException {
        super(context, new File(context.getExternalFilesDir(null), KML_FILE), listener);
        mPlacemarkNameColumn = placemarkNameColumn;
    }

    /**
     * @return the file if it was correctly exported, null otherwise.
     */
    @Override
    public File export() {
        Log.v(TAG, "export");
        final String[] columnsToExport = mContext.getResources().getStringArray(R.array.db_columns);
        Map<String, String> columnNamesMapping = new HashMap<String, String>(columnsToExport.length);
        Cursor c = mContext.getContentResolver().query(NetMonColumns.CONTENT_URI, columnsToExport, null, null, NetMonColumns.TIMESTAMP);
        if (c != null) {
            try {
                Log.v(TAG, "Find user-friendly labels for columns " + Arrays.toString(columnsToExport));
                for (String element : columnsToExport) {
                    int columnLabelId = mContext.getResources().getIdentifier(element, "string", R.class.getPackage().getName());
                    if (columnLabelId > 0) {
                        String columnLabel = mContext.getString(columnLabelId);
                        Log.v(TAG, element + "->" + columnLabel);
                        columnNamesMapping.put(element, columnLabel);
                    }
                }
                Log.v(TAG, "Column names: " + Arrays.toString(columnsToExport));

                KMLStyle kmlStyle = KMLStyle.getKMLStyle(mContext, mPlacemarkNameColumn);
                KMLWriter kmlWriter = new KMLWriter(mFile, kmlStyle, mContext.getString(R.string.unknown), columnNamesMapping);
                // Write the table rows to the file.
                if (c.moveToFirst()) {
                    int rowCount = c.getCount();
                    int latitudeIndex = c.getColumnIndex(NetMonColumns.DEVICE_LATITUDE);
                    int longitudeIndex = c.getColumnIndex(NetMonColumns.DEVICE_LONGITUDE);
                    int timestampIndex = c.getColumnIndex(NetMonColumns.TIMESTAMP);
                    int dataIndex = c.getColumnIndex(mPlacemarkNameColumn);
                    // Start writing to the file.
                    kmlWriter.writeHeader();
                    while (c.moveToNext()) {
                        Map<String, String> cellValues = new LinkedHashMap<String, String>(c.getColumnCount());
                        long timestamp = c.getLong(timestampIndex);
                        Date date = new Date(timestamp);
                        String timestampString = DATE_FORMAT.format(date);
                        for (int i = 0; i < c.getColumnCount(); i++) {
                            String cellValue;
                            if (NetMonColumns.TIMESTAMP.equals(c.getColumnName(i))) cellValue = timestampString;
                            else
                                cellValue = c.getString(i);
                            if (cellValue == null) cellValue = "";
                            cellValues.put(c.getColumnName(i), cellValue);
                        }
                        kmlWriter.writePlacemark(c.getString(dataIndex), cellValues, c.getString(latitudeIndex), c.getString(longitudeIndex), timestamp);
                        // Notify the listener of our progress (progress is 1-based)
                        if (mListener != null) mListener.onExportProgress(c.getPosition() + 1, rowCount);
                    }
                    // Write the footer and clean up the file.
                    kmlWriter.writeFooter();
                    kmlWriter.close();
                }

                return mFile;
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Could not export to file " + mFile + ": " + e.getMessage(), e);
            } finally {
                c.close();
            }
        }
        return null;
    }
}
