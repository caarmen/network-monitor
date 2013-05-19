package org.jraf.android.networkmonitor.app.export;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.provider.NetMonColumns;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public abstract class FileExport {
	private static final String TAG = Constants.TAG
			+ FileExport.class.getSimpleName();
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd' 'HH:mm:ss", Locale.US);

	protected PrintWriter mPrintWriter;
	protected final Context mContext;
	protected final File mFile;

	FileExport(Context context, File file) throws FileNotFoundException {
		mContext = context;
		mFile = file;
	}

	abstract void writeHeader(String[] columnNames);

	abstract void writeFooter();

	abstract void writeRow(int rowNumber, String[] cellValues);

	public File export() {
		Log.v(TAG, "export");
		Cursor c = mContext.getContentResolver().query(
				NetMonColumns.CONTENT_URI, null, null, null,
				NetMonColumns.TIMESTAMP);
		if (c != null) {
			try {
				mPrintWriter = new PrintWriter(mFile);
				String[] columnNames = c.getColumnNames();
				String[] usedColumnNames = new String[c.getColumnCount()-1];
				System.arraycopy(columnNames, 1, usedColumnNames, 0, c.getColumnCount()-1);
				writeHeader(usedColumnNames);
				if (c.moveToFirst()) {
					while (c.moveToNext()) {
						String[] cellValues = new String[c.getColumnCount()-1];
						for (int i = 1; i < c.getColumnCount(); i++) {
							String cellValue;
							if (NetMonColumns.TIMESTAMP.equals(c
									.getColumnName(i))) {
								long timestamp = c.getLong(i);
								Date date = new Date(timestamp);
								cellValue = DATE_FORMAT.format(date);
							} else {
								cellValue = c.getString(i);
							}
							if (cellValue == null)
								cellValue = "";
							cellValues[i-1] = cellValue;
						}
						writeRow(c.getPosition(), cellValues);
						mPrintWriter.flush();
					}
				}
				writeFooter();
				mPrintWriter.close();
				return mFile;
			} catch (FileNotFoundException e) {
				Log.e(TAG, "export Could not export file " + mFile + ": " + e.getMessage(), e);
			} finally {
				c.close();
			}
		}
		return null;
	}
}
