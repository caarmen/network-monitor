/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013-2020 Carmen Alvarez (c@rmen.ca)
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

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;

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
        final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.US);
        private final Context mContext;

        DefaultFormatter(Context context) {
            mContext = context;
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
                result = mDateFormat.format(date);
            }
            // bytes
            else if (NetMonColumns.MOST_CONSUMING_APP_BYTES.equals(columnName)) {
                long bytes = c.getLong(columnIndex);
                if (bytes > 0) {
                    result = android.text.format.Formatter.formatShortFileSize(mContext, bytes);
                } else {
                    result = "";
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

        XMLFormatter(Context context) {
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
