/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013-2017 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.dbops.backend.export.kml;

import android.content.Context;
import android.database.Cursor;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.ProgressListener;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.FileExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.Formatter;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.FormatterFactory;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.FormatterFactory.FormatterStyle;
import ca.rmen.android.networkmonitor.app.dbops.ui.Share;
import ca.rmen.android.networkmonitor.app.prefs.FilterPreferences;
import ca.rmen.android.networkmonitor.app.prefs.FilterPreferences.Selection;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import android.util.Log;

/**
 * Export the Network Monitor data to a KML file. The KML file placemark icon label and color depend on the field the user chose to export.
 */
public class KMLExport extends FileExport {
    private static final String TAG = Constants.TAG + KMLExport.class.getSimpleName();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.US);
    private static final String KML_FILE_PREFIX = "networkmonitor-";

    // The field which determines the name/label of the KML placemarks we will export.i
    private final String mPlacemarkNameColumn;

    /**
     * @param placemarkNameColumn the column whose value will be exported to the KML placemark names.
     */
    public KMLExport(Context context, String placemarkNameColumn) {
        super(context, Share.getExportFile(context, KML_FILE_PREFIX + placemarkNameColumn + ".kml"));
        mPlacemarkNameColumn = placemarkNameColumn;
    }

    @Override
    public void execute(ProgressListener listener) {
        Log.v(TAG, "export");
        Formatter formatter = FormatterFactory.getFormatter(FormatterStyle.XML, mContext);
        List<String> selectedColumns = new ArrayList<>(NetMonPreferences.getInstance(mContext).getSelectedColumns());
        if (!selectedColumns.contains(NetMonColumns.DEVICE_LATITUDE)) selectedColumns.add(NetMonColumns.DEVICE_LATITUDE);
        if (!selectedColumns.contains(NetMonColumns.DEVICE_LONGITUDE)) selectedColumns.add(NetMonColumns.DEVICE_LONGITUDE);
        if (!selectedColumns.contains(mPlacemarkNameColumn)) selectedColumns.add(mPlacemarkNameColumn);
        final String[] columnsToExport = new String[selectedColumns.size()];
        selectedColumns.toArray(columnsToExport);
        Map<String, String> columnNamesMapping = new HashMap<>(columnsToExport.length);
        // Filter the results based on the user's preferences.
        Selection selection = FilterPreferences.getSelectionClause(mContext);
        Cursor c = mContext.getContentResolver().query(NetMonColumns.CONTENT_URI, columnsToExport, selection.selectionString, selection.selectionArgs,
                NetMonColumns.TIMESTAMP);
        if (c != null) {
            try {
                Log.v(TAG, "Find user-friendly labels for columns " + Arrays.toString(columnsToExport));
                for (String element : columnsToExport) {
                    String columnLabel = NetMonColumns.getColumnLabel(mContext, element);
                    Log.v(TAG, element + "->" + columnLabel);
                    columnNamesMapping.put(element, columnLabel);
                }
                Log.v(TAG, "Column names: " + Arrays.toString(columnsToExport));

                KMLStyle kmlStyle = KMLStyleFactory.getKMLStyle(mPlacemarkNameColumn);
                int placemarkNameColumnId = c.getColumnIndex(mPlacemarkNameColumn);
                String now = DATE_FORMAT.format(new Date());
                String title = mContext.getString(R.string.app_name) + ": " + columnNamesMapping.get(mPlacemarkNameColumn) + " (" + now + ")";
                KMLWriter kmlWriter = new KMLWriter(mFile, title, kmlStyle, mContext.getString(R.string.export_value_unknown), columnNamesMapping);

                // Write the KML placemarks to the file.
                int rowCount = c.getCount();
                int latitudeIndex = c.getColumnIndex(NetMonColumns.DEVICE_LATITUDE);
                int longitudeIndex = c.getColumnIndex(NetMonColumns.DEVICE_LONGITUDE);
                int timestampIndex = c.getColumnIndex(NetMonColumns.TIMESTAMP);

                // Start writing to the file.
                kmlWriter.writeHeader();

                // Write one KML placemark for each row in the DB.
                while (c.moveToNext() && !isCanceled()) {
                    Map<String, String> cellValues = new LinkedHashMap<>(c.getColumnCount());
                    long timestamp = -1;
                    if (timestampIndex >= 0) {
                        timestamp = c.getLong(timestampIndex);
                    }
                    for (int i = 0; i < c.getColumnCount(); i++) {
                        String cellValue = formatter.format(c, i);
                        cellValues.put(c.getColumnName(i), cellValue);
                    }
                    String placemarkName = formatter.format(c, placemarkNameColumnId);
                    kmlWriter.writePlacemark(placemarkName, cellValues, c.getString(latitudeIndex), c.getString(longitudeIndex), timestamp);

                    // Notify the listener of our progress (progress is 1-based)
                    if (listener != null) listener.onProgress(c.getPosition() + 1, rowCount);
                }
                // Write the footer and clean up the file.
                kmlWriter.writeFooter();
                kmlWriter.close();
                if (listener != null) {
                    if (isCanceled()) {
                        listener.onComplete(mContext.getString(R.string.export_notif_canceled_content));
                    } else {
                        listener.onComplete(mContext.getString(R.string.export_save_to_external_storage_success, mFile.getAbsolutePath()));
                    }
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Could not export to file " + mFile + ": " + e.getMessage(), e);
            } finally {
                c.close();
            }
        }
        if (listener != null) listener.onError(mContext.getString(R.string.export_notif_error_content));
    }

}
