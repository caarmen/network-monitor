package org.jraf.android.networkmonitor.app.export;

import java.util.SortedSet;
import java.util.TreeSet;

import android.content.Context;
import android.database.Cursor;

import org.jraf.android.networkmonitor.provider.NetMonColumns;

public class SummaryExport {
    static class CellResult implements Comparable {
        final int passRate;
        final int testCount;

        public CellResult(int passRate, int testCount) {
            this.passRate = passRate;
            this.testCount = testCount;
        }

        @Override
        public String toString() {
            return passRate + "% (" + testCount + " tests)";
        }

        @Override
        public int compareTo(Object other) {
            if (other instanceof CellResult) {
                CellResult otherCell = (CellResult) other;
                double rateDiff = passRate - otherCell.passRate;
                if (rateDiff > 0) return 1;
                else if (rateDiff < 0) return -1;
                else
                    return testCount - otherCell.testCount;
            }
            return -1;
        }
    }

    static class GsmCellResult extends CellResult {
        final int lac;
        final int longCellId;
        final int shortCellId;

        public GsmCellResult(int lac, int longCellId, int shortCellId, int passRate, int testCount) {
            super(passRate, testCount);
            this.lac = lac;
            this.longCellId = longCellId;
            this.shortCellId = shortCellId;
        }

        @Override
        public String toString() {
            return "LAC=" + lac + ",CID=" + shortCellId + "(" + longCellId + "): " + super.toString();
        }

        @Override
        public int compareTo(Object other) {
            if (other instanceof CellResult) {
                int result = super.compareTo(other);
                if (result != 0) return result;
            }
            if (other instanceof GsmCellResult) {
                GsmCellResult otherGsmCell = (GsmCellResult) other;
                if (lac != otherGsmCell.lac) return lac - otherGsmCell.lac;
                return longCellId - otherGsmCell.longCellId;
            }
            return -1;
        }

    }

    public static final String getSummary(Context context) {
        Cursor c = context.getContentResolver().query(NetMonColumns.CONTENT_URI_GSM_SUMMARY, null, null, null, null);
        if (c != null) {
            SortedSet<CellResult> cellResults = new TreeSet<CellResult>();
            try {
                if (c.moveToFirst()) {
                    do {
                        for (int i = 0; i < c.getColumnCount(); i++) {
                            int lac = c.getInt(c.getColumnIndex(NetMonColumns.GSM_CELL_LAC));
                            int longCellId = c.getInt(c.getColumnIndex(NetMonColumns.GSM_FULL_CELL_ID));
                            int shortCellId = c.getInt(c.getColumnIndex(NetMonColumns.GSM_SHORT_CELL_ID));
                            int passCount = c.getInt(c.getColumnIndex(NetMonColumns.PASS_COUNT));
                            int failCount = c.getInt(c.getColumnIndex(NetMonColumns.FAIL_COUNT));
                            int passRate = 100 * passCount / (passCount + failCount);
                            int testCount = passCount + failCount;
                            GsmCellResult gsmCellResult = new GsmCellResult(lac, longCellId, shortCellId, passRate, testCount);
                            cellResults.add(gsmCellResult);
                        }
                    } while (c.moveToNext());
                    StringBuilder sb = new StringBuilder();
                    for (CellResult cellResult : cellResults)
                        sb.append(cellResult).append("\n");
                    return sb.toString();
                }
            } finally {
                c.close();
            }
        }
        return null;
    }
}
