package org.jraf.android.networkmonitor.app.export;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;

import jxl.Cell;
import jxl.JXLException;
import jxl.Workbook;
import jxl.format.CellFormat;
import jxl.format.Colour;
import jxl.write.Label;
import jxl.write.WritableCell;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class ExcelExport extends FileExport {

	private static final String TAG = Constants.TAG
			+ ExcelExport.class.getSimpleName();
	private static final String EXCEL_FILE = "networkmonitor.xls";

	private WritableWorkbook mWorkbook;
	private WritableSheet mSheet;
	private WritableCellFormat mBoldFormat;
	private WritableCellFormat mRedFormat;
	private WritableCellFormat mGreenFormat;

	public ExcelExport(Context context) throws FileNotFoundException {
		super(context, new File(context.getExternalFilesDir(null), EXCEL_FILE));
	}

	@Override
	void writeHeader(String[] columnNames) {
		try {
			mWorkbook = Workbook.createWorkbook(mFile);
			mSheet = mWorkbook.createSheet(
					mContext.getString(R.string.app_name), 0);
			mSheet.insertRow(0);
			createCellFormats();
			for (int i = 0; i < columnNames.length; i++) {
				mSheet.insertColumn(i);
				insertCell(columnNames[i], 0, i, mBoldFormat);
			}
		} catch (IOException e) {
			Log.e(TAG, "writeHeader Could not create workbook", e);
		}
	}

	@Override
	void writeFooter() {
		try {
			mWorkbook.write();
			mWorkbook.close();
		} catch (IOException e) {
			Log.e(TAG, "writeFooter Could not close file", e);
		} catch (JXLException e) {
			Log.e(TAG, "writeHeader Could not close file", e);
		}
	}

	@Override
	void writeRow(int rowNumber, String[] cellValues) {
		mSheet.insertRow(rowNumber);
		for (int i = 0; i < cellValues.length; i++) {
			CellFormat cellFormat = null;
			if("PASS".equals(cellValues[i]))
				cellFormat = mGreenFormat;
			else if("FAIL".equals(cellValues[i]))
				cellFormat = mRedFormat;
			insertCell(cellValues[i], rowNumber, i, cellFormat);
		}
	}

	private WritableCell insertCell(String text, int row, int column,
			CellFormat format) {
		Label label = format == null ? new Label(column, row, text)
				: new Label(column, row, text, format);
		try {
			mSheet.addCell(label);
			return label;
		} catch (JXLException e) {
			Log.e(TAG, "writeHeader Could not insert cell " + text + " at row="
					+ row + ", col=" + column, e);
			return null;
		}
	}

	private void createCellFormats() {
		Label cell = new Label(0,0," ");
		
		CellFormat cellFormat = cell.getCellFormat();

		WritableFont boldFont = new WritableFont(cellFormat.getFont());
		mBoldFormat = new WritableCellFormat(cellFormat);
		try {
			boldFont.setBoldStyle(WritableFont.BOLD);
			mBoldFormat.setFont(boldFont);

			mRedFormat = new WritableCellFormat(cellFormat);
			final WritableFont redFont = new WritableFont(cellFormat.getFont());
			redFont.setColour(Colour.RED);
			mRedFormat.setFont(redFont);

			mGreenFormat = new WritableCellFormat(cellFormat);
			final WritableFont greenFont = new WritableFont(cellFormat.getFont());
			greenFont.setColour(Colour.GREEN);
			mGreenFormat.setFont(greenFont);
		} catch (WriteException e) {
			Log.e(TAG, "createCellFormats Could not create cell formats", e);
		}
	}

}