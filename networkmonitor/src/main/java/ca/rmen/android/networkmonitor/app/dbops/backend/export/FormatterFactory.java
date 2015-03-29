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
package ca.rmen.android.networkmonitor.app.dbops.backend.export;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences.CellIdFormat;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Create the appropriate Formatter instance depending on the formatter style.
 */
public class FormatterFactory {
    private static final String TAG = Constants.TAG + FormatterFactory.class.getSimpleName();

    public enum FormatterStyle {
        DEFAULT, XML
    }

    /**
     * @return the {@link Formatter} which will format for the given {@link FormatterStyle}.
     */
    public static Formatter getFormatter(FormatterStyle formatterStyle, Context context) {
        switch (formatterStyle) {
            case XML:
                return new XMLFormatter(context);
            default:
                return new DefaultFormatter(context);
        }
    }

    /**
     * This formats values with a format which is common to all export types.
     */
    private static class DefaultFormatter implements Formatter {
        public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.US);
        private final CellIdFormat mCellIdFormat;

        public DefaultFormatter(Context context) {
            mCellIdFormat = NetMonPreferences.getInstance(context).getCellIdFormat();
            Log.v(TAG, "Constructor: cellIdFormat = " + mCellIdFormat);
        }

        /**
         * @return a non-null String representation of the value at the given column and current Cursor row.
         */
        @Override
        public String format(Cursor c, int columnIndex) {
            String result;
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
    }

    /**
     * This does everything the {@link DefaultFormatter} does, and also makes sure certain text fields have special characters escaped properly for XML.
     */
    private static class XMLFormatter extends DefaultFormatter {

        private final List<String> mTextColumnNames;

        public XMLFormatter(Context context) {
            super(context);
            String[] textColumns = context.getResources().getStringArray(R.array.text_columns);
            mTextColumnNames = Arrays.asList(textColumns);
        }

        /**
         * @return a non-null String representation of the value at the given column and current Cursor row, suitable for XML.
         */
        @Override
        public String format(Cursor c, int columnIndex) {
            String result = super.format(c, columnIndex);
            String columnName = c.getColumnName(columnIndex);
            // If this is a field which can contain free text, let's make sure special characters are escaped for XML.
            if (mTextColumnNames.contains(columnName)) {
                result = result.replaceAll("&", "&amp;");
                result = result.replaceAll("<", "&lt;");
                result = result.replaceAll(">", "&gt;");
            }
            return result;
        }

    }
}
