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

	protected final PrintWriter mPrintWriter;
	private final Context mContext;
	private final File mFile;

	public FileExport(Context context, File file) throws FileNotFoundException {
		mPrintWriter = new PrintWriter(file);
		mContext = context;
		mFile = file;
	}

	abstract void writeHeader(String[] columnNames);

	abstract void writeFooter();

	abstract void writeRow(String[] cellValues);

	public File export() {
		Log.v(TAG, "export");
		Cursor c = mContext.getContentResolver().query(
				NetMonColumns.CONTENT_URI, null, null, null,
				NetMonColumns.TIMESTAMP);
		if (c != null) {
			try {
				writeHeader(c.getColumnNames());
				if (c.moveToFirst()) {
					while (c.moveToNext()) {
						String[] cellValues = new String[c.getColumnCount()];
						for (int i = 0; i < c.getColumnCount(); i++) {
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
							cellValues[i] = cellValue;
						}
						writeRow(cellValues);
						mPrintWriter.flush();
					}
				}
				writeFooter();
				mPrintWriter.close();
				return mFile;
			} finally {
				c.close();
			}
		}
		return null;
	}
}
