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

import java.util.SortedSet;
import java.util.TreeSet;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.TelephonyManager;

import org.jraf.android.networkmonitor.provider.NetMonColumns;
import org.jraf.android.networkmonitor.util.TelephonyUtil;

public class SummaryExport {
    /**
     * Contains data for cell tests common to all cell types.
     */
    static class CellResult implements Comparable<CellResult> {
        final int passRate;
        final int testCount;

        /**
         * @param passRate the percent of tests which pass (0..100)
         * @param testCount the number of tests on this cell.
         */
        public CellResult(int passRate, int testCount) {
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
            double rateDiff = passRate - otherCell.passRate;
            if (rateDiff > 0) return 1;
            else if (rateDiff < 0) return -1;
            else
                return testCount - otherCell.testCount;
        }
    }

    /**
     * Test data and GSM cell identifiers for a cell.
     */
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
    static class CdmaCellResult extends CellResult {
        final int baseStationId;
        final int networkId;
        final int systemId;

        public CdmaCellResult(int baseStationId, int networkId, int systemId, int passRate, int testCount) {
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
        if (c != null) {
            SortedSet<CellResult> cellResults = new TreeSet<CellResult>();
            try {
                if (c.moveToFirst()) {
                    do {
                        for (int i = 0; i < c.getColumnCount(); i++) {
                            int passCount = c.getInt(c.getColumnIndex(NetMonColumns.PASS_COUNT));
                            int failCount = c.getInt(c.getColumnIndex(NetMonColumns.FAIL_COUNT));
                            int passRate = 100 * passCount / (passCount + failCount);
                            int testCount = passCount + failCount;
                            CellResult cellResult = null;
                            if (phoneType == TelephonyManager.PHONE_TYPE_GSM) cellResult = readGsmCellResult(c, passRate, testCount);
                            else if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) cellResult = readCdmaCellResult(c, passRate, testCount);
                            if (cellResult != null) cellResults.add(cellResult);
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
