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
package ca.rmen.android.networkmonitor.app.dbops.backend.export;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.FormatterFactory.FormatterStyle;
import ca.rmen.android.networkmonitor.app.dbops.ui.Share;
import ca.rmen.android.networkmonitor.app.prefs.FilterPreferences;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.app.prefs.SortPreferences;
import ca.rmen.android.networkmonitor.app.prefs.SortPreferences.SortOrder;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import android.util.Log;

/**
 * Export the Network Monitor data to an HTML file. The HTML file includes CSS specified in the strings XML file.
 */
public class HTMLExport extends TableFileExport {
    private static final String TAG = Constants.TAG + TableFileExport.class.getSimpleName();
    private static final String SCHEME_NETMON = "netmon:";
    private static final String TABLE_HEIGHT_FILE_EXPORT = "100vh";
    public static final String URL_SORT = SCHEME_NETMON + "//sort";
    public static final String URL_FILTER = SCHEME_NETMON + "//filter";
    private static final String HTML_FILE = "networkmonitor.html";
    private PrintWriter mPrintWriter;
    private final String mFixedTableHeight;

    /**
     */
    public HTMLExport(Context context) {
        this(context, true, TABLE_HEIGHT_FILE_EXPORT);
    }

    /**
     * @param external if true, the file will be exported to the sd card. Otherwise it will written to the application's internal storage.
     * @param fixedTableHeight CSS height specification for the body of the table (below the column headers). Ex: "100vh" or "1080px". If provided, the header of the table will remain in a fixed position, the body of the table will have the given fixed height, and the contents of the table body will be scrollable.
     */
    public HTMLExport(Context context, boolean external, String fixedTableHeight) {
        super(context, new File(external ? Share.getExportFolder(context) : context.getFilesDir(), HTML_FILE), FormatterStyle.XML);
        mFixedTableHeight = fixedTableHeight;
    }

    @Override
    void writeHeader(String[] columnLabels) {
        try {
            mPrintWriter = new PrintWriter(mFile, "utf-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            Log.e(TAG, "writeHeader Could not initialize print writer", e);
            return;
        }
        mPrintWriter.println("<!DOCTYPE html>");
        mPrintWriter.println("<html>");
        mPrintWriter.println("  <head>");
        mPrintWriter.println("    <title>" + mContext.getString(R.string.app_name) + "</title>");
        String columnCss = mContext.getString(R.string.css_template, getColumnCss());

        mPrintWriter.println(mContext.getString(R.string.css));
        mPrintWriter.println(mContext.getString(R.string.css_api_level_custom));
        mPrintWriter.println(mContext.getString(R.string.css_themed));
        mPrintWriter.println(columnCss);
        if (mFixedTableHeight != null) {
            String fixedHeaderCss = mContext.getString(R.string.fixed_header_css, mFixedTableHeight);
            mPrintWriter.println(fixedHeaderCss);
        }
        mPrintWriter.println("  </head><body>");
        mPrintWriter.println("<table class='main-table'>");
        mPrintWriter.println(getColgroup(columnLabels.length));
        mPrintWriter.println("  <thead><tr>");
        SortPreferences sortPreferences = NetMonPreferences.getInstance(mContext).getSortPreferences();
        mPrintWriter.println("    <th></th>");
        for (String columnLabel : columnLabels) {
            String dbColumnName = NetMonColumns.getColumnName(mContext, columnLabel);
            if (dbColumnName != null) {
                String labelClass = "column_heading";

                // Indicate if this is the sorting column: specify a particular css style
                // for the label, and show an up or down arrow depending on if we're
                // sorting ascending or descending.
                String sortIconCharacter = "";
                if (dbColumnName.equals(sortPreferences.sortColumnName)) {
                    labelClass += " sort_column";
                    if (sortPreferences.sortOrder == SortOrder.DESC)
                        sortIconCharacter = mContext.getString(R.string.icon_sort_desc);
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
                String sort = sortIconCharacter + "<a class=\"" + labelClass + "\" href=\"" + URL_SORT + dbColumnName + "\">" + columnLabel + "</a>";

                // One cell for the filter icon.
                String filter = "<a class=\"" + filterIconClass + "\" href=\"" + URL_FILTER + dbColumnName + "\">" + filterIconCharacter + "</a>";

                // Write out the table cell for this column header.
                mPrintWriter.println("    <th>" + sort + filter + "</th>");
            } else {
                mPrintWriter.println("    <th></th>");
            }
        }
        mPrintWriter.println("  </tr></thead><tbody>");
        mPrintWriter.println("  <tr>");
        // container-container?  :( Wish I could do this more cleanly/simply.
        mPrintWriter.println("    <td class='table-data-container-container' colspan='" + (columnLabels.length + 1) + "'>");
        mPrintWriter.println("      <div class='table-data-container'>");
        mPrintWriter.println("        <table class='table-data'>");
        mPrintWriter.println(getColgroup(columnLabels.length));
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
        mPrintWriter.println("</tbody></table></div></td></tr>");
        mPrintWriter.println("</tbody></table>");
        mPrintWriter.println("</body></html>");
        mPrintWriter.flush();
        mPrintWriter.close();
    }


    /**
     * @return the longest word in the given string.
     */
    private static String findLongestWord(String s) {
        String[] tokens = s.split(" ");
        String longestWord = null;
        for (String token : tokens) {
            if(longestWord == null || token.length() > longestWord.length()) longestWord = token;
        }
        return longestWord;
    }

    /**
     * @return the HTML &lt;colgroup&gt; tag including all the &lt;col&gt; child tags, used to size each column.
     */
    private String getColgroup(int numColumns) {
        StringBuilder stringBuilder = new StringBuilder("  <colgroup>");
        for (int i = 0; i <= numColumns; i++) {
            stringBuilder.append("    <col class='col").append(i).append("'/>\n");
        }
        stringBuilder.append("  </colgroup>");
        return stringBuilder.toString();

    }

    /**
     * @return the CSS code for the styles of each column.  The styles only specify the column width.
     */
    private String getColumnCss() {
        int[] columnWidths = getBestColumnWidths();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(".col0{width:50px}\n");
        for (int i = 0; i < columnWidths.length; i++) {
            stringBuilder.append(".col").append((i+1)).append("{width:").append((columnWidths[i] + 3)).append("em;}").append('\n');
        }
        return stringBuilder.toString();
    }

    /**
     * Determines the best width for each column based on the column titles and the column data.
     * @return an array of widths, in em, for each column in the table.
     */
    private int[] getBestColumnWidths() {
        String[] usedColumnNames = (String[]) NetMonPreferences.getInstance(mContext).getSelectedColumns().toArray();
        Uri uri = NetMonColumns.CONTENT_URI;
        FilterPreferences.Selection selection = FilterPreferences.getSelectionClause(mContext);
        String[] projection = new String[usedColumnNames.length];
        for (int i =0; i < usedColumnNames.length; i++) {
            projection[i] = "MAX(length(" + usedColumnNames[i] + "))";
        }
        int[] columnWidths = new int[usedColumnNames.length];
        Cursor c = mContext.getContentResolver().query(uri, projection, selection.selectionString, selection.selectionArgs,
                null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    for (int i=0; i < c.getColumnCount(); i++) {
                        String columnLabel = NetMonColumns.getColumnLabel(mContext, usedColumnNames[i]);
                        String longestWordInLabel = findLongestWord(columnLabel);
                        int lengthLongestWordInLabel = longestWordInLabel.length();
                        int lengthLongestValue = c.getInt(i);
                        // The best column width is the largest of:
                        // a) the largest data value
                        // b) the largest word in the column title
                        columnWidths[i] = Math.max(lengthLongestWordInLabel, lengthLongestValue);
                    }
                }
            } finally {
                c.close();
            }
        }
        return columnWidths;
    }
}
