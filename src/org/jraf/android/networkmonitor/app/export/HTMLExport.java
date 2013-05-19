package org.jraf.android.networkmonitor.app.export;

import java.io.File;
import java.io.FileNotFoundException;

import org.jraf.android.networkmonitor.R;

import android.content.Context;

public class HTMLExport extends FileExport {
	private static final String HTML_FILE = "networkmonitor.html";

	public HTMLExport(Context context) throws FileNotFoundException {
		super(context, new File(context.getCacheDir(), HTML_FILE));
	}

	@Override
	void writeHeader(String[] columnNames) {
		mPrintWriter.println("<html>");
		mPrintWriter.println("  <head>");
		mPrintWriter.println(mContext.getString(R.string.css));
		mPrintWriter.println("  </head><body>");
		mPrintWriter.println("<table><thead>");

		mPrintWriter.println("  <tr>");
		for (String columnName : columnNames) {
			columnName = columnName.replaceAll("_"," ");
			mPrintWriter.println("    <th>" + columnName + "</th>");
		}
		mPrintWriter.println("  </tr></thead><tbody>");
	}

	@Override
	void writeFooter() {
		mPrintWriter.println("</tbody></table></body></html>");
	}

	@Override
	void writeRow(int rowNumber, String[] cellValues) {
		String tdClass = "odd";
		if(rowNumber % 2 == 0)
			tdClass = "even";
		mPrintWriter.println("  <tr class=\""+ tdClass+ "\">");
		for (String cellValue : cellValues)
			mPrintWriter.println("    <td>" + cellValue + "</td>");
		mPrintWriter.println("  </tr>");
	}
}