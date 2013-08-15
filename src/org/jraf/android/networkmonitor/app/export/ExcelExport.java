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

import jxl.CellView;
import jxl.JXLException;
import jxl.Workbook;
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
import android.util.Log;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;

/**
 * Export the Network Monitor data to an Excel file.
 */
public class ExcelExport extends FileExport {
    private static final String TAG = Constants.TAG + ExcelExport.class.getSimpleName();

    private static final String EXCEL_FILE = "networkmonitor.xls";

    private WritableWorkbook mWorkbook;
    private WritableSheet mSheet;
    private WritableCellFormat mBoldFormat;
    private WritableCellFormat mRedFormat;
    private WritableCellFormat mGreenFormat;
    private int mColumnWidth;
    private int mColumnCount;

    public ExcelExport(Context context) throws FileNotFoundException {
        super(context, new File(context.getExternalFilesDir(null), EXCEL_FILE));
    }

    @Override
    void writeHeader(String[] columnNames) throws IOException {
        // Create the workbook, sheet, custom cell formats, and freeze
        // row/column.
        mWorkbook = Workbook.createWorkbook(mFile);
        mSheet = mWorkbook.createSheet(mContext.getString(R.string.app_name), 0);
        mSheet.insertRow(0);
        mSheet.getSettings().setHorizontalFreeze(2);
        mSheet.getSettings().setVerticalFreeze(1);
        createCellFormats();
        for (int i = 0; i < columnNames.length; i++) {
            mSheet.insertColumn(i);
            int columnWidth = getLongestWordLength(columnNames[i]);
            if (columnWidth > mColumnWidth) mColumnWidth = columnWidth;
            insertCell(columnNames[i], 0, i, mBoldFormat);
        }
        mColumnCount = columnNames.length;
    }

    private int getLongestWordLength(String s) {
        String[] words = s.split(" ");
        int result = 0;
        for (String word : words) {
            if (word.length() > result) result = word.length();
        }
        return result;
    }

    @Override
    void writeRow(int rowNumber, String[] cellValues) throws IOException {
        mSheet.insertRow(rowNumber);
        for (int i = 0; i < cellValues.length; i++) {
            CellFormat cellFormat = null;
            if (Constants.CONNECTION_TEST_PASS.equals(cellValues[i])) cellFormat = mGreenFormat;
            else if (Constants.CONNECTION_TEST_FAIL.equals(cellValues[i])) cellFormat = mRedFormat;
            insertCell(cellValues[i], rowNumber, i, cellFormat);
        }
    }

    @Override
    void writeFooter() throws IOException {
        Log.v(TAG, "writeFooter: column width = " + mColumnWidth + " characters, " + mColumnCount + " columns");
        try {
            // Auto size the first column so the timestamp appears completely.
            CellView columnView = mSheet.getColumnView(0);
            columnView.setAutosize(true);
            mSheet.setColumnView(0, columnView);
            // Set the column width of all the other columns such that no word in the column headings will be truncated.
            for (int i = 1; i < mColumnCount; i++) {
                columnView = mSheet.getColumnView(i);
                // The column size is "the width of the column in characters multiplied by 256".  We add one character to the longest column heading word we have, to add additional horizontal padding.
                columnView.setSize((mColumnWidth + 1) * 256);
                mSheet.setColumnView(i, columnView);
            }
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

    private void insertCell(String text, int row, int column, CellFormat format) {
        Label label = format == null ? new Label(column, row, text) : new Label(column, row, text, format);
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
            // Create the bold format
            final WritableFont boldFont = new WritableFont(cellFormat.getFont());
            mBoldFormat = new WritableCellFormat(cellFormat);
            boldFont.setBoldStyle(WritableFont.BOLD);
            mBoldFormat.setFont(boldFont);
            mBoldFormat.setWrap(true);
            mBoldFormat.setAlignment(Alignment.CENTRE);

            // Create the red format
            mRedFormat = new WritableCellFormat(cellFormat);
            final WritableFont redFont = new WritableFont(cellFormat.getFont());
            redFont.setColour(Colour.RED);
            mRedFormat.setFont(redFont);

            // Create the green format
            mGreenFormat = new WritableCellFormat(cellFormat);
            final WritableFont greenFont = new WritableFont(cellFormat.getFont());
            greenFont.setColour(Colour.GREEN);
            mGreenFormat.setFont(greenFont);
        } catch (WriteException e) {
            Log.e(TAG, "createCellFormats Could not create cell formats", e);
        }
    }
}
