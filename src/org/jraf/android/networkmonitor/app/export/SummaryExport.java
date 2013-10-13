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

import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.provider.NetMonColumns;
import org.jraf.android.networkmonitor.util.TelephonyUtil;

public class SummaryExport {
    /**
     * Contains data for cell tests common to all cell types.
     */
    private static class CellResult implements Comparable<CellResult> {
        private final int passRate;
        final int testCount;

        /**
         * @param passRate the percent of tests which pass (0..100)
         * @param testCount the number of tests on this cell.
         */
        private CellResult(int passRate, int testCount) {
            this.passRate = passRate;
            this.testCount = testCount;
        }

        @Override
        public String toString() {
            return passRate + "% (" + testCount + " tests)";
        }

        @Override
        public int compareTo(CellResult other) {
            CellResult otherCell = other;
            int rateDiff = passRate - otherCell.passRate;
            if (rateDiff > 0) return 1;
            else if (rateDiff < 0) return -1;
            else
                return testCount - otherCell.testCount;
        }
    }

    /**
     * Test data and GSM cell identifiers for a cell.
     */
    private static class GsmCellResult extends CellResult {
        private final int lac;
        private final int longCellId;
        private final int shortCellId;

        private GsmCellResult(int lac, int longCellId, int shortCellId, int passRate, int testCount) {
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
        public int compareTo(CellResult other) {
            int result = super.compareTo(other);
            if (result != 0) return result;
            if (other instanceof GsmCellResult) {
                GsmCellResult otherGsmCell = (GsmCellResult) other;
                if (lac != otherGsmCell.lac) return lac - otherGsmCell.lac;
                return longCellId - otherGsmCell.longCellId;
            }
            return -1;
        }
    }

    /**
     * Test data and CDMA cell identifiers for a cell.
     */
    private static class CdmaCellResult extends CellResult {
        private final int baseStationId;
        private final int networkId;
        private final int systemId;

        private CdmaCellResult(int baseStationId, int networkId, int systemId, int passRate, int testCount) {
            super(passRate, testCount);
            this.baseStationId = baseStationId;
            this.networkId = networkId;
            this.systemId = systemId;
        }

        @Override
        public String toString() {
            return "BSID=" + baseStationId + ",SID=" + systemId + ",NID=" + networkId + ": " + super.toString();
        }

        @Override
        public int compareTo(CellResult other) {
            int result = super.compareTo(other);
            if (result != 0) return result;
            if (other instanceof CdmaCellResult) {
                CdmaCellResult otherCdmaCell = (CdmaCellResult) other;
                if (baseStationId != otherCdmaCell.baseStationId) return baseStationId - otherCdmaCell.baseStationId;
                if (networkId != otherCdmaCell.networkId) return networkId - otherCdmaCell.networkId;
                return systemId - otherCdmaCell.systemId;
            }
            return -1;
        }

    }

    /**
     * @return a summary report listing the tested cells: the cell ids, % pass rate, and number of tests are shown.
     */
    public static final String getSummary(Context context) {
        Uri uri = null;
        int phoneType = TelephonyUtil.getDeviceType(context);
        if (phoneType == TelephonyManager.PHONE_TYPE_GSM) uri = NetMonColumns.CONTENT_URI_GSM_SUMMARY;
        else if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) uri = NetMonColumns.CONTENT_URI_CDMA_SUMMARY;
        else
            throw new IllegalArgumentException("Error: unknown phone type " + phoneType);

        Cursor c = context.getContentResolver().query(uri, null, null, null, null);
        SortedMap<String, SortedSet<CellResult>> cellResults = new TreeMap<String, SortedSet<CellResult>>();
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    do {
                        for (int i = 0; i < c.getColumnCount(); i++) {
                            String extraInfo = c.getString(c.getColumnIndex(NetMonColumns.EXTRA_INFO));
                            if (TextUtils.isEmpty(extraInfo)) extraInfo = "*";
                            int passCount = c.getInt(c.getColumnIndex(NetMonColumns.PASS_COUNT));
                            int failCount = c.getInt(c.getColumnIndex(NetMonColumns.FAIL_COUNT));
                            int slowCount = c.getInt(c.getColumnIndex(NetMonColumns.SLOW_COUNT));
                            int testCount = passCount + failCount + slowCount;
                            int passRate = testCount > 0 ? 100 * passCount / testCount : 0;
                            CellResult cellResult = null;
                            if (phoneType == TelephonyManager.PHONE_TYPE_GSM) cellResult = readGsmCellResult(c, passRate, testCount);
                            else if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) cellResult = readCdmaCellResult(c, passRate, testCount);
                            if (cellResult != null) {
                                SortedSet<CellResult> cellResultsForExtraInfo = cellResults.get(extraInfo);
                                if (cellResultsForExtraInfo == null) {
                                    cellResultsForExtraInfo = new TreeSet<CellResult>();
                                    cellResults.put(extraInfo, cellResultsForExtraInfo);
                                }
                                cellResultsForExtraInfo.add(cellResult);
                            }
                        }
                    } while (c.moveToNext());
                }
            } finally {
                c.close();
            }
        }
        return generateReport(context, cellResults);
    }

    private static String generateReport(Context context, SortedMap<String, SortedSet<CellResult>> cellResults) {
        StringBuilder sb = new StringBuilder();
        sb.append(Build.MODEL + "/" + Build.VERSION.RELEASE + "\n");
        if (cellResults == null || cellResults.size() == 0) {
            sb.append(context.getString(R.string.error_no_mobile_tests));
        } else
            for (String extraInfo : cellResults.keySet()) {
                sb.append(extraInfo + ":\n");
                for (int i = 0; i < extraInfo.length(); i++)
                    sb.append("-");
                sb.append("\n");
                Set<CellResult> cellResultsForExtraInfo = cellResults.get(extraInfo);
                for (CellResult cellResult : cellResultsForExtraInfo)
                    sb.append(cellResult).append("\n");
                sb.append("\n");
            }
        return sb.toString();

    }

    private static GsmCellResult readGsmCellResult(Cursor c, int passRate, int testCount) {
        int lac = c.getInt(c.getColumnIndex(NetMonColumns.GSM_CELL_LAC));
        int longCellId = c.getInt(c.getColumnIndex(NetMonColumns.GSM_FULL_CELL_ID));
        int shortCellId = c.getInt(c.getColumnIndex(NetMonColumns.GSM_SHORT_CELL_ID));
        GsmCellResult result = new GsmCellResult(lac, longCellId, shortCellId, passRate, testCount);
        return result;
    }

    private static CdmaCellResult readCdmaCellResult(Cursor c, int passRate, int testCount) {
        int baseStationId = c.getInt(c.getColumnIndex(NetMonColumns.CDMA_CELL_BASE_STATION_ID));
        int networkId = c.getInt(c.getColumnIndex(NetMonColumns.CDMA_CELL_NETWORK_ID));
        int systemId = c.getInt(c.getColumnIndex(NetMonColumns.CDMA_CELL_SYSTEM_ID));
        CdmaCellResult result = new CdmaCellResult(baseStationId, networkId, systemId, passRate, testCount);
        return result;
    }
}
