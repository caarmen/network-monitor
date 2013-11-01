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
import android.text.TextUtils;
import android.util.Log;

import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.provider.NetMonColumns;

public class SummaryExport {
    private static final String TAG = SummaryExport.class.getSimpleName();

    /**
     * Contains data for network tests common to all network types.
     */
    private static class TestResult implements Comparable<TestResult> {
        private final int passRate;
        final int testCount;
        final String label;

        /**
         * @param passRate the percent of tests which pass (0..100)
         * @param testCount the number of tests on this cell.
         */
        private TestResult(String label, int passRate, int testCount) {
            if (TextUtils.isEmpty(label)) this.label = "*";
            else
                this.label = label;
            this.passRate = passRate;
            this.testCount = testCount;

        }

        @Override
        public String toString() {
            return passRate + "% (" + testCount + " tests)";
        }

        @Override
        public int compareTo(TestResult other) {
            if (other.getClass().equals(other.getClass())) {
                TestResult otherCell = other;
                int rateDiff = passRate - otherCell.passRate;
                if (rateDiff > 0) return 1;
                else if (rateDiff < 0) return -1;
                else
                    return testCount - otherCell.testCount;
            } else {
                return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
            }
        }
    }

    /**
     * Test data and GSM cell identifiers for a cell.
     */
    private static class GsmCellResult extends TestResult {
        private final int lac;
        private final int longCellId;
        private final int shortCellId;

        private GsmCellResult(String label, int lac, int longCellId, int shortCellId, int passRate, int testCount) {
            super(label, passRate, testCount);
            this.lac = lac;
            this.longCellId = longCellId;
            this.shortCellId = shortCellId;
        }

        @Override
        public String toString() {
            return "LAC=" + lac + ",CID=" + shortCellId + "(" + longCellId + "): " + super.toString();
        }

        @Override
        public int compareTo(TestResult other) {
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
    private static class CdmaCellResult extends TestResult {
        private final int baseStationId;
        private final int networkId;
        private final int systemId;

        private CdmaCellResult(String label, int baseStationId, int networkId, int systemId, int passRate, int testCount) {
            super(label, passRate, testCount);
            this.baseStationId = baseStationId;
            this.networkId = networkId;
            this.systemId = systemId;
        }

        @Override
        public String toString() {
            return "BSID=" + baseStationId + ",SID=" + systemId + ",NID=" + networkId + ": " + super.toString();
        }

        @Override
        public int compareTo(TestResult other) {
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
     * Test data for a WiFi access point.
     */
    private static class WiFiResult extends TestResult {
        private final String ssid;

        private WiFiResult(String ssid, int passRate, int testCount) {
            super(ssid, passRate, testCount);
            this.ssid = ssid;
        }

        @Override
        public String toString() {
            return "SSID=" + ssid + ": " + super.toString();
        }

        @Override
        public int compareTo(TestResult other) {
            int result = super.compareTo(other);
            if (result != 0) return result;
            if (other instanceof WiFiResult) {
                WiFiResult otherWiFiResult = (WiFiResult) other;
                return ssid.compareTo(otherWiFiResult.ssid);
            }
            return -1;
        }
    }

    /**
     * @return a summary report listing the tested cells: the cell ids, % pass rate, and number of tests are shown.
     */
    public static final String getSummary(Context context) {
        Log.v(TAG, "getSummary");
        SortedMap<String, SortedSet<TestResult>> testResults = new TreeMap<String, SortedSet<TestResult>>();
        SortedMap<String, SortedSet<TestResult>> gsmCellResults = getTestResults(context, NetMonColumns.CONTENT_URI_GSM_SUMMARY, GsmCellResult.class);
        SortedMap<String, SortedSet<TestResult>> cdmaCellResults = getTestResults(context, NetMonColumns.CONTENT_URI_CDMA_SUMMARY, CdmaCellResult.class);
        SortedMap<String, SortedSet<TestResult>> wifiResults = getTestResults(context, NetMonColumns.CONTENT_URI_WIFI_SUMMARY, WiFiResult.class);
        testResults.putAll(gsmCellResults);
        testResults.putAll(cdmaCellResults);
        testResults.putAll(wifiResults);
        return generateReport(context, testResults);
    }

    private static SortedMap<String, SortedSet<TestResult>> getTestResults(Context context, Uri uri, Class<?> clazz) {
        Cursor c = context.getContentResolver().query(uri, null, null, null, null);
        SortedMap<String, SortedSet<TestResult>> cellResults = new TreeMap<String, SortedSet<TestResult>>();
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    do {
                        for (int i = 0; i < c.getColumnCount(); i++) {

                            int passCount = c.getInt(c.getColumnIndex(NetMonColumns.PASS_COUNT));
                            int failCount = c.getInt(c.getColumnIndex(NetMonColumns.FAIL_COUNT));
                            int slowCount = c.getInt(c.getColumnIndex(NetMonColumns.SLOW_COUNT));
                            int testCount = passCount + failCount + slowCount;
                            int passRate = testCount > 0 ? 100 * passCount / testCount : 0;

                            TestResult cellResult = readCellResult(clazz, c, passRate, testCount);
                            if (cellResult != null) {
                                SortedSet<TestResult> cellResultsForExtraInfo = cellResults.get(cellResult.label);
                                if (cellResultsForExtraInfo == null) {
                                    cellResultsForExtraInfo = new TreeSet<TestResult>();
                                    cellResults.put(cellResult.label, cellResultsForExtraInfo);
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
        return cellResults;
    }

    private static String generateReport(Context context, SortedMap<String, SortedSet<TestResult>> cellResults) {
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
                Set<TestResult> cellResultsForExtraInfo = cellResults.get(extraInfo);
                for (TestResult cellResult : cellResultsForExtraInfo)
                    sb.append(cellResult).append("\n");
                sb.append("\n");
            }
        return sb.toString();

    }

    private static GsmCellResult readGsmCellResult(Cursor c, int passRate, int testCount) {
        int lac = c.getInt(c.getColumnIndex(NetMonColumns.GSM_CELL_LAC));
        int longCellId = c.getInt(c.getColumnIndex(NetMonColumns.GSM_FULL_CELL_ID));
        int shortCellId = c.getInt(c.getColumnIndex(NetMonColumns.GSM_SHORT_CELL_ID));
        String label = c.getString(c.getColumnIndex(NetMonColumns.EXTRA_INFO));
        GsmCellResult result = new GsmCellResult(label, lac, longCellId, shortCellId, passRate, testCount);
        return result;
    }

    private static CdmaCellResult readCdmaCellResult(Cursor c, int passRate, int testCount) {
        int baseStationId = c.getInt(c.getColumnIndex(NetMonColumns.CDMA_CELL_BASE_STATION_ID));
        int networkId = c.getInt(c.getColumnIndex(NetMonColumns.CDMA_CELL_NETWORK_ID));
        int systemId = c.getInt(c.getColumnIndex(NetMonColumns.CDMA_CELL_SYSTEM_ID));
        String label = c.getString(c.getColumnIndex(NetMonColumns.EXTRA_INFO));
        CdmaCellResult result = new CdmaCellResult(label, baseStationId, networkId, systemId, passRate, testCount);
        return result;
    }

    private static WiFiResult readWiFiResult(Cursor c, int passRate, int testCount) {
        String ssid = c.getString(c.getColumnIndex(NetMonColumns.WIFI_SSID));
        WiFiResult result = new WiFiResult(ssid, passRate, testCount);
        return result;
    }

    private static TestResult readCellResult(Class<?> clazz, Cursor c, int passRate, int testCount) {
        if (clazz.equals(GsmCellResult.class)) return readGsmCellResult(c, passRate, testCount);
        else if (clazz.equals(CdmaCellResult.class)) return readCdmaCellResult(c, passRate, testCount);
        else if (clazz.equals(WiFiResult.class)) return readWiFiResult(c, passRate, testCount);
        else
            return null;
    }
}
