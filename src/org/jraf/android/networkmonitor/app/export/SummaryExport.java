package org.jraf.android.networkmonitor.app.export;

import android.content.Context;
import android.database.Cursor;

import org.jraf.android.networkmonitor.provider.NetMonColumns;

public class SummaryExport {
    public static final String getSummary(Context context) {
        Cursor c = context.getContentResolver().query(NetMonColumns.CONTENT_URI_GSM_SUMMARY, null, null, null, null);
        if (c != null) {
            StringBuilder sb = new StringBuilder();
            try {
                if (c.moveToFirst()) {
                    for (String columnName : c.getColumnNames()) {
                        sb.append(columnName + ",");
                    }
                    sb.append("\n");
                    do {
                        for (int i = 0; i < c.getColumnCount(); i++) {
                            sb.append(c.getString(i)).append(",");
                        }
                        sb.append("\n");
                    } while (c.moveToNext());
                }
            } finally {
                c.close();
            }
            return sb.toString();
        }
        return null;
    }
}
