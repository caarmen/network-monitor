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

import java.io.IOException;
import java.util.Arrays;

import ca.rmen.android.networkmonitor.app.dbops.ui.Share;
import jxl.CellView;
import jxl.JXLException;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.format.Alignment;
import jxl.format.CellFormat;
import jxl.format.Colour;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import android.content.Context;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.FormatterFactory.FormatterStyle;
import android.util.Log;

/**
 * Export the Network Monitor data to an Excel file.
 */
public class ExcelExport extends TableFileExport {
    private static final String TAG = Constants.TAG + ExcelExport.class.getSimpleName();

    private static final String EXCEL_FILE = "networkmonitor.xls";

    private WritableWorkbook mWorkbook;
    private WritableSheet mSheet;
    private WritableCellFormat mDefaultFormat;
    private WritableCellFormat mBoldFormat;
    private WritableCellFormat mRedFormat;
    private WritableCellFormat mGreenFormat;
    private WritableCellFormat mAmberFormat;
    private int mRowCount;
    private int mColumnCount;

    public ExcelExport(Context context) {
        super(context, Share.getExportFile(context, EXCEL_FILE), FormatterStyle.DEFAULT);
    }

    @Override
    void writeHeader(String[] columnNames) throws IOException {
        Log.v(TAG, "writeHeader: " + Arrays.toString(columnNames));
        // Create the workbook, sheet, custom cell formats, and freeze
        // row/column.
        WorkbookSettings workbookSettings = new WorkbookSettings();
        workbookSettings.setUseTemporaryFileDuringWrite(true);
        mWorkbook = Workbook.createWorkbook(mFile, workbookSettings);
        mSheet = mWorkbook.createSheet(mContext.getString(R.string.app_name), 0);
        mSheet.insertRow(0);
        mSheet.getSettings().setHorizontalFreeze(3);
        mSheet.getSettings().setVerticalFreeze(1);
        createCellFormats();
        for (int i = 0; i < columnNames.length; i++) {
            mSheet.insertColumn(i);
            insertCell(columnNames[i], 0, i, mBoldFormat);
        }
        mColumnCount = columnNames.length;
        mRowCount = 0;
    }


    @Override
    void writeRow(int rowNumber, String[] cellValues) {
        mSheet.insertRow(rowNumber + 1);
        for (int i = 0; i < cellValues.length; i++) {
            CellFormat cellFormat = null;
            if (Constants.CONNECTION_TEST_PASS.equals(cellValues[i])) cellFormat = mGreenFormat;
            else if (Constants.CONNECTION_TEST_FAIL.equals(cellValues[i])) cellFormat = mRedFormat;
            else if (Constants.CONNECTION_TEST_SLOW.equals(cellValues[i])) cellFormat = mAmberFormat;
            insertCell(cellValues[i], rowNumber + 1, i, cellFormat);
        }
        mRowCount++;
    }

    @Override
    void writeFooter() throws IOException {
        Log.v(TAG, "writeFooter");
        try {
            for (int i = 0; i < mColumnCount; i++)
                resizeColumn(i);
            // Set the heading row height to 4 lines tall.  Using autoSize doesn't seem to work (the resulting file has only one row of characters in the header row).
            // Not sure how to dynamically calculate the optimal height of the header row, so we just assume the largest column heading will be four lines tall.
            CellView headerRowView = mSheet.getRowView(0);
            headerRowView.setSize(headerRowView.getSize() * 4);
            mSheet.setRowView(0, headerRowView);
            mWorkbook.write();
            mWorkbook.close();
        } catch (JXLException e) {
            Log.e(TAG, "writeHeader Could not close file", e);
        }
    }

    /**
     * Calculates the optimal width of the column and sets the column width to that value. The width will be large enough to fit the contents of all the cells
     * after the header, and large enough to fit the largest word in the header.
     */
    private void resizeColumn(int col) {
        String columnName = mSheet.getCell(col, 0).getContents();
        Log.v(TAG, "resizeColumn " + col + ": " + columnName);

        // Make sure the column width is large enough to fit this column name (plus a space for extra padding).
        int columnWidth = getLongestWordLength(columnName);
        // Make sure the column width is large enough to fit the widest data cell.
        // (Normally I would use setAutosize() but once you autosize a column in jxl, you can't
        // disable the autosize, so I have to calculate this myself...)
        for (int i = 1; i <= mRowCount; i++) {
            String cellValue = mSheet.getCell(col, i).getContents();
            int cellWidth = cellValue.length();
            if (cellWidth > columnWidth) columnWidth = cellWidth;
        }
        Log.v(TAG, "columnWidth: " + columnWidth);

        // The width of the column is the number of characters multiplied by 256.
        // From the Excel documentation, the width of a column is an:

        // "integer that specifies the column width in units of 1/256th of a character width.
        // Character width is defined as the maximum digit width of the numbers 0, 1, 2, ... 9
        // as rendered in the Normal style's font."

        // Some of our letters may be wider than the digits 0-9. So we may need to overestimate
        // the width we need by a bit. Adding a padding of 4 characters seems to be ok
        // for this app.
        CellView columnView = mSheet.getColumnView(col);
        columnView.setSize((columnWidth + 4) * 256);
        mSheet.setColumnView(col, columnView);
    }

    /**
     * @return the number of characters of the longest word in the given string.
     */
    private int getLongestWordLength(String s) {
        String[] words = s.split(" ");
        int result = 0;
        for (String word : words)
            if (word.length() > result) result = word.length();
        return result;
    }

    private void insertCell(String text, int row, int column, CellFormat format) {
        if (format == null) format = mDefaultFormat;
        Label label = new Label(column, row, text, format);
        try {
            mSheet.addCell(label);
        } catch (JXLException e) {
            Log.e(TAG, "writeHeader Could not insert cell " + text + " at row=" + row + ", col=" + column, e);
        }
    }

    /**
     * In order to set text to bold, red, or green, we need to create cell
     * formats for each style.
     */
    private void createCellFormats() {

        // Insert a dummy empty cell, so we can obtain its cell. This allows to
        // start with a default cell format.
        Label cell = new Label(0, 0, " ");
        CellFormat cellFormat = cell.getCellFormat();

        try {
            // Center all cells.
            mDefaultFormat = new WritableCellFormat(cellFormat);
            mDefaultFormat.setAlignment(Alignment.CENTRE);

            // Create the bold format
            final WritableFont boldFont = new WritableFont(cellFormat.getFont());
            mBoldFormat = new WritableCellFormat(mDefaultFormat);
            boldFont.setBoldStyle(WritableFont.BOLD);
            mBoldFormat.setFont(boldFont);
            mBoldFormat.setWrap(true);
            mBoldFormat.setAlignment(Alignment.CENTRE);

            // Create the red format
            mRedFormat = new WritableCellFormat(mDefaultFormat);
            final WritableFont redFont = new WritableFont(cellFormat.getFont());
            redFont.setColour(Colour.RED);
            mRedFormat.setFont(redFont);

            // Create the green format
            mGreenFormat = new WritableCellFormat(mDefaultFormat);
            final WritableFont greenFont = new WritableFont(cellFormat.getFont());
            greenFont.setColour(Colour.GREEN);
            mGreenFormat.setFont(greenFont);

            // Create the amber format
            mAmberFormat = new WritableCellFormat(mDefaultFormat);
            final WritableFont amberFont = new WritableFont(cellFormat.getFont());
            amberFont.setColour(Colour.LIGHT_ORANGE);
            mAmberFormat.setFont(amberFont);
        } catch (WriteException e) {
            Log.e(TAG, "createCellFormats Could not create cell formats", e);
        }
    }
}
