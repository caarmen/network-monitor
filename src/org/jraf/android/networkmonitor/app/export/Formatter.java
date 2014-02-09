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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.app.prefs.NetMonPreferences;
import org.jraf.android.networkmonitor.app.prefs.NetMonPreferences.CellIdFormat;
import org.jraf.android.networkmonitor.provider.NetMonColumns;
import org.jraf.android.networkmonitor.util.Log;

/**
 * Formats recorded values for exporting
 */
public class Formatter {
    private static final String TAG = Constants.TAG + Formatter.class.getSimpleName();

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.US);
    private final CellIdFormat mCellIdFormat;

    public Formatter(Context context) {
        mCellIdFormat = NetMonPreferences.getInstance(context).getCellIdFormat();
        Log.v(TAG, "Constructor: cellIdFormat = " + mCellIdFormat);
    }

    /**
     * @return a non-null String representation of the value at the given column and current Cursor row.
     */
    public String format(Cursor c, int columnIndex) {
        String result = null;
        String columnName = c.getColumnName(columnIndex);
        // Format timestamps
        if (NetMonColumns.TIMESTAMP.equals(columnName)) {
            long timestamp = c.getLong(columnIndex);
            Date date = new Date(timestamp);
            result = DATE_FORMAT.format(date);
        }
        // Format cell ids
        else if (NetMonColumns.CDMA_CELL_BASE_STATION_ID.equals(columnName) || NetMonColumns.CDMA_CELL_NETWORK_ID.equals(columnName)
                || NetMonColumns.CDMA_CELL_SYSTEM_ID.equals(columnName) || NetMonColumns.GSM_FULL_CELL_ID.equals(columnName)
                || NetMonColumns.GSM_SHORT_CELL_ID.equals(columnName) || NetMonColumns.GSM_CELL_LAC.equals(columnName)) {
            result = c.getString(columnIndex);
            if (mCellIdFormat != CellIdFormat.DECIMAL) {
                if (!TextUtils.isEmpty(result)) {
                    try {
                        long cellId = Long.parseLong(result);
                        String cellIdHex = Long.toHexString(cellId);
                        if (mCellIdFormat == CellIdFormat.HEX) result = cellIdHex;
                        else
                            result = cellId + " (" + cellIdHex + ")";
                    } catch (NumberFormatException e) {
                        // Can't read the cell id as a hex number: just display the raw value.
                    }
                }
            }
        }
        // Anything else: just return the raw value as a string
        else {
            result = c.getString(columnIndex);
        }
        // Make sure we don't return null.
        if (result == null) result = "";
        return result;
    }

    /**
     * @return a non-null String representation of the value at the given column and current Cursor row, suitable for XML.
     */
    public String formatXML(Cursor c, int columnIndex) {
        String result = format(c, columnIndex);
        result = result.replaceAll("&", "&amp;");
        result = result.replaceAll("<", "&lt;");
        result = result.replaceAll(">", "&gt;");
        return result;
    }
}
