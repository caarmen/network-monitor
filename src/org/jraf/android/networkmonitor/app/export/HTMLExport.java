package org.jraf.android.networkmonitor.app.export;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.Context;

public class HTMLExport extends FileExport {
	private static final String HTML_FILE = "networkmonitor.html";

	public HTMLExport(Context context)
			throws FileNotFoundException {
		super(context, new File(context.getFilesDir(), HTML_FILE));
	}

	@Override
	void writeHeader(String[] columnNames) {
		mPrintWriter.println("<html><body><table>");
		mPrintWriter.println("<th>");
		for (String columnName : columnNames)
			mPrintWriter.println("<td>" + columnName + "</td>");
		mPrintWriter.println("</tr>");
	}

	@Override
	void writeFooter() {
		mPrintWriter.println("</table></body></html>");
	}

	@Override
	void writeRow(String[] cellValues) {
		mPrintWriter.println("  <tr>");
		for (String cellValue : cellValues)
			mPrintWriter.println("    <td>" + cellValue + "</td>");
		mPrintWriter.println("  </tr>");
	}
}