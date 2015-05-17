/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013-2015 Carmen Alvarez (c@rmen.ca)
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import android.content.Context;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.FormatterFactory.FormatterStyle;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.app.prefs.SortPreferences;
import ca.rmen.android.networkmonitor.app.prefs.SortPreferences.SortOrder;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Export the Network Monitor data to an HTML file. The HTML file includes CSS specified in the strings XML file.
 */
public class HTMLExport extends TableFileExport {
    private static final String TAG = Constants.TAG + TableFileExport.class.getSimpleName();
    private static final String SCHEME_NETMON = "netmon:";
    public static final String URL_SORT = SCHEME_NETMON + "//sort";
    public static final String URL_FILTER = SCHEME_NETMON + "//filter";
    private static final String HTML_FILE = "networkmonitor.html";
    private PrintWriter mPrintWriter;

    /**
     * @param external if true, the file will be exported to the sd card. Otherwise it will written to the application's internal storage.
     */
    public HTMLExport(Context context, boolean external) {
        super(context, new File(external ? context.getExternalFilesDir(null) : context.getFilesDir(), HTML_FILE), FormatterStyle.XML);
    }

    @Override
    void writeHeader(String[] columnLabels) {
        try {
            mPrintWriter = new PrintWriter(mFile, "utf-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            Log.e(TAG, "writeHeader Could not initialize print writer", e);
            return;
        }
        mPrintWriter.println("<html>");
        mPrintWriter.println("  <head>");
        mPrintWriter.println(mContext.getString(R.string.css));
        mPrintWriter.println("  </head><body>");
        mPrintWriter.println("<table><thead>");

        mPrintWriter.println("  <tr>");
        SortPreferences sortPreferences = NetMonPreferences.getInstance(mContext).getSortPreferences();
        mPrintWriter.println("    <th><table><tr></tr></table></th>");
        for (String columnLabel : columnLabels) {
            String dbColumnName = NetMonColumns.getColumnName(mContext, columnLabel);
            String labelClass = "";

            // Indicate if this is the sorting column: specify a particular css style
            // for the label, and show an up or down arrow depending on if we're
            // sorting ascending or descending.
            String sortIconCharacter = "";
            if (dbColumnName.equals(sortPreferences.sortColumnName)) {
                labelClass = "sort_column";
                if (sortPreferences.sortOrder == SortOrder.DESC) sortIconCharacter = mContext.getString(R.string.icon_sort_desc);
                else
                    sortIconCharacter = mContext.getString(R.string.icon_sort_asc);
            }

            // Indicate if this is a filtered column: specify a particular css style
            // for the label, and show the filter on or off icon.
            boolean isFilterable = NetMonColumns.isColumnFilterable(mContext, dbColumnName);
            String filterIconClass = "filter_icon";
            String filterIconCharacter = isFilterable ? mContext.getString(R.string.icon_filter_off) : "";
            if (isFilterable) {
                List<String> columnFilterValues = NetMonPreferences.getInstance(mContext).getColumnFilterValues(dbColumnName);
                if (columnFilterValues != null && columnFilterValues.size() > 0) {
                    labelClass += " filtered_column_label";
                    filterIconCharacter = mContext.getString(R.string.icon_filter_on);
                }
            }

            // One cell for the sort icon and column label.
            String sort = "<td class=\"" + labelClass + "\">" + sortIconCharacter + "<a href=\"" + URL_SORT + dbColumnName + "\">" + columnLabel + "</a></td>";

            // One cell for the filter icon.
            String filter = "<td class=\"" + filterIconClass + "\"><a href=\"" + URL_FILTER + dbColumnName + "\"a>" + filterIconCharacter + "</a></td>";

            // Write out the table cell for this column header.
            mPrintWriter.println("    <th><table><tr>" + sort + filter + "</tr></table></th>");
        }
        mPrintWriter.println("  </tr></thead><tbody>");
    }

    @Override
    void writeRow(int rowNumber, String[] cellValues) {
        if (mPrintWriter == null) return;
        // Alternating styles for odd and even rows.
        String trClass = "odd";
        if (rowNumber % 2 == 0) trClass = "even";
        mPrintWriter.println("  <tr class=\"" + trClass + "\">");

        mPrintWriter.println("    <td>" + (rowNumber + 1) + "</td>");
        for (String cellValue : cellValues) {
            String tdClass = "";
            // Highlight PASS in green and FAIL in red.
            if (Constants.CONNECTION_TEST_FAIL.equals(cellValue)) tdClass = "fail";
            else if (Constants.CONNECTION_TEST_PASS.equals(cellValue)) tdClass = "pass";
            else if (Constants.CONNECTION_TEST_SLOW.equals(cellValue)) tdClass = "slow";
            mPrintWriter.println("    <td class=\"" + tdClass + "\">" + cellValue + "</td>");
        }
        mPrintWriter.println("  </tr>");
        mPrintWriter.flush();
    }

    @Override
    void writeFooter() {
        if (mPrintWriter == null) return;
        mPrintWriter.println("</tbody></table>");
        mPrintWriter.println("</body></html>");
        mPrintWriter.flush();
        mPrintWriter.close();
    }
}
