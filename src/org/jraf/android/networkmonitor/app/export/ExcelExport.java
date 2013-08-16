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
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.util.Log;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;

/**
 * Export the Network Monitor data to an Excel file.
 */
public class ExcelExport extends FileExport {
    private static final String TAG = Constants.TAG + ExcelExport.class.getSimpleName();

    private static final String EXCEL_FILE = "networkmonitor.xls";

    private Workbook mWorkbook;
    private Sheet mSheet;
    private CellStyle mDefaultFormat;
    private CellStyle mBoldFormat;
    private CellStyle mRedFormat;
    private CellStyle mGreenFormat;
    private int mRowCount;
    private int mColumnCount;

    public ExcelExport(Context context) throws FileNotFoundException {
        super(context, new File(context.getExternalFilesDir(null), EXCEL_FILE));
    }

    @Override
    void writeHeader(String[] columnNames) throws IOException {
        // Create the workbook, sheet, custom cell formats, and freeze
        // row/column.
        mWorkbook = new HSSFWorkbook();
        mSheet = mWorkbook.createSheet(mContext.getString(R.string.app_name));
        mSheet.createRow(0);
        mSheet.createFreezePane(2, 1);
        createCellFormats();
        for (int i = 0; i < columnNames.length; i++) {
            insertCell(columnNames[i], 0, i, mBoldFormat);
        }
        mColumnCount = columnNames.length;
        mRowCount = 0;
    }


    @Override
    void writeRow(int rowNumber, String[] cellValues) throws IOException {
        mSheet.createRow(rowNumber);
        for (int i = 0; i < cellValues.length; i++) {
            CellStyle cellFormat = null;
            if (Constants.CONNECTION_TEST_PASS.equals(cellValues[i])) cellFormat = mGreenFormat;
            else if (Constants.CONNECTION_TEST_FAIL.equals(cellValues[i])) cellFormat = mRedFormat;
            insertCell(cellValues[i], rowNumber, i, cellFormat);
        }
        mRowCount++;
    }

    @Override
    void writeFooter() throws IOException {
        Log.v(TAG, "writeFooter");
        for (int i = 0; i < mColumnCount; i++)
            resizeColumn(i);
        // Set the heading row height to 4 lines tall.  Using autoSize doesn't seem to work (the resulting file has only one row of characters in the header row).
        // Not sure how to dynamically calculate the optimal height of the header row, so we just assume the largest column heading will be four lines tall.
        Row headerRow = mSheet.getRow(0);
        headerRow.setHeight((short) (headerRow.getHeight() * 4));
        File file = new File(mContext.getExternalFilesDir(null), EXCEL_FILE);
        FileOutputStream os = new FileOutputStream(file);
        mWorkbook.write(os);
    }

    /**
     * Calculates the optimal width of the column and sets the column width to that value. The width will be large enough to fit the contents of all the cells
     * after the header, and large enough to fit the largest word in the header.
     */
    private void resizeColumn(int col) {
        String columnName = mSheet.getRow(0).getCell(col).getStringCellValue();
        Log.v(TAG, "resizeColumn " + col + ": " + columnName);

        // Make sure the column width is large enough to fit this column name (plus a space for extra padding).
        int columnWidth = getLongestWordLength(columnName);
        // Make sure the column width is large enough to fit the widest data cell.
        // (Normally I would use setAutosize() but once you autosize a column in jxl, you can't
        // disable the autosize, so I have to calculate this myself...)
        for (int i = 1; i <= mRowCount; i++) {
            String cellValue = mSheet.getRow(i).getCell(col).getStringCellValue();
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
        mSheet.setColumnWidth(col, (columnWidth + 4) * 256);
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

    private void insertCell(String text, int row, int column, CellStyle format) {
        if (format == null) format = mDefaultFormat;
        Row r = mSheet.getRow(row);
        Cell cell = r.createCell(column, Cell.CELL_TYPE_STRING);
        cell.setCellValue(text);
        cell.setCellStyle(format);
    }

    /**
     * In order to set text to bold, red, or green, we need to create cell
     * formats for each style.
     */
    private void createCellFormats() {

        // Insert a dummy empty cell, so we can obtain its cell. This allows to
        // start with a default cell format.

        mDefaultFormat = mWorkbook.createCellStyle();
        mDefaultFormat.setAlignment(CellStyle.ALIGN_CENTER);

        // Create the bold format
        Font boldFont = mWorkbook.createFont();
        mBoldFormat = mWorkbook.createCellStyle();
        boldFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        mBoldFormat.setFont(boldFont);
        mBoldFormat.setWrapText(true);
        mBoldFormat.setAlignment(CellStyle.ALIGN_CENTER);

        // Create the red format
        mRedFormat = mWorkbook.createCellStyle();
        Font redFont = mWorkbook.createFont();
        redFont.setColor(HSSFColor.RED.index);
        mRedFormat.setFont(redFont);
        mRedFormat.setAlignment(CellStyle.ALIGN_CENTER);

        // Create the green format
        mGreenFormat = mWorkbook.createCellStyle();
        Font greenFont = mWorkbook.createFont();
        greenFont.setColor(HSSFColor.GREEN.index);
        mGreenFormat.setFont(greenFont);
        mGreenFormat.setAlignment(CellStyle.ALIGN_CENTER);
    }
}
