package org.jraf.android.networkmonitor.app.export;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.Context;
import android.text.TextUtils;

public class CSVExport extends FileExport {
	private static final String CSV_FILE = "networkmonitor.csv";

	public CSVExport(Context context) throws FileNotFoundException {
		super(context, new File(context.getExternalFilesDir(null), CSV_FILE));
	}

	@Override
	void writeHeader(String[] columnNames) {
		mPrintWriter.println(TextUtils.join(",", columnNames));
	}

	@Override
	void writeFooter() {
	}

	@Override
	void writeRow(String[] cellValues) {

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < cellValues.length; i++) {
			if (cellValues[i].contains(",")) {
				cellValues[i].replaceAll("\"", "\"\"");
				cellValues[i] = "\"" + cellValues[i] + "\"";
			}
			sb.append(cellValues[i]);
			if (i < cellValues.length - 1)
				sb.append(",");
		}
		mPrintWriter.println(sb);
	}
}