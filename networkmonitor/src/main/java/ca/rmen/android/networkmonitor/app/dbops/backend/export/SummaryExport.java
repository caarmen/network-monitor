/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013-2019 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.dbops.backend.export;

import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import androidx.annotation.NonNull;
import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.Constants.ConnectionType;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.provider.ConnectionTestStatsColumns;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;

public class SummaryExport {
    private static final String TAG = Constants.TAG + SummaryExport.class.getSimpleName();

    /**
     * Contains data for network tests common to all network types.
     */
    private static class TestResult implements Comparable<TestResult> {
        private int mPassCount, mFailCount, mSlowCount;
        private int mTestCount;
        int passRate;
        final String label;
        final String id1, id2, id3;

        private TestResult(String label, String id1, String id2, String id3) {
            if (TextUtils.isEmpty(label)) this.label = "*";
            else
                this.label = label;
            this.id1 = id1;
            this.id2 = id2;
            this.id3 = id3;
        }

        private void calculatePassRate() {
            mTestCount = mPassCount + mFailCount + mSlowCount;
            passRate = mTestCount > 0 ? 100 * mPassCount / mTestCount : 0;
        }

        void setPassCount(int passCount) {
            mPassCount = passCount;
            calculatePassRate();
        }

        void setFailCount(int failCount) {
            mFailCount = failCount;
            calculatePassRate();
        }

        void setSlowCount(int slowCount) {
            mSlowCount = slowCount;
            calculatePassRate();
        }

        @Override
        @NonNull
        public String toString() {
            return passRate + "% (" + mTestCount + " tests)";
        }

        private int compare(String s1, String s2) {
            if (s1 == null && s2 == null) return 0;
            if (s1 != null && s2 != null) return s1.compareTo(s2);
            if (s1 == null) return -1;
            return 1;
        }

        @Override
        public int compareTo(@NonNull TestResult other) {
            if (other.getClass().equals(other.getClass())) {
                // First, compare by score
                int rateDiff = passRate - other.passRate;
                if (rateDiff > 0) return 1;
                else if (rateDiff < 0) return -1;

                // If two TestResults have the same score, compare by their ids.
                final int id1compare = compare(id1, other.id1);
                if (id1compare != 0) return id1compare;
                final int id2compare = compare(id2, other.id2);
                if (id2compare != 0) return id2compare;
                final int id3compare = compare(id3, other.id3);
                if (id3compare != 0) return id3compare;

                // Same score and same ids, compare by number of tests.
                return mTestCount - other.mTestCount;
            } else {
                return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
            }
        }
    }

    /**
     * Test data and GSM cell identifiers for a cell.
     */
    private static class GsmCellResult extends TestResult {

        private GsmCellResult(String label, String lac, String longCellId, String shortCellId) {
            super(label, lac, longCellId, shortCellId);
        }

        @Override
        @NonNull
        public String toString() {
            return "LAC=" + id1 + ",CID=" + id2 + "(" + id3 + "): " + super.toString();
        }
    }


    /**
     * Test data and CDMA cell identifiers for a cell.
     */
    private static class CdmaCellResult extends TestResult {

        private CdmaCellResult(String label, String baseStationId, String networkId, String systemId) {
            super(label, baseStationId, networkId, systemId);
        }

        @Override
        @NonNull
        public String toString() {
            return "BSSID=" + id1 + ",SID=" + id2 + ",NID=" + id3 + ": " + super.toString();
        }
    }

    /**
     * Test data for a WiFi access point.
     */
    private static class WiFiResult extends TestResult {

        private WiFiResult(String ssid, String bssid) {
            super(ssid, bssid, null, null);
        }

        @Override
        @NonNull
        public String toString() {
            return "BSSID=" + id1 + ": " + super.toString();
        }
    }

    /**
     * @return a summary report listing the tested cells: the cell ids, % pass rate, and number of tests are shown.
     */
    public static String getSummary(Context context) {
        Log.v(TAG, "getSummary");

        String[] projection = new String[] { ConnectionTestStatsColumns.TYPE, ConnectionTestStatsColumns.ID1, ConnectionTestStatsColumns.ID2,
                ConnectionTestStatsColumns.ID3, ConnectionTestStatsColumns.LABEL, ConnectionTestStatsColumns.TEST_COUNT, ConnectionTestStatsColumns.TEST_RESULT };
        String orderBy = ConnectionTestStatsColumns.ID1 + "," + ConnectionTestStatsColumns.ID2 + "," + ConnectionTestStatsColumns.ID3;
        Cursor c = context.getContentResolver().query(ConnectionTestStatsColumns.CONTENT_URI, projection, null, null, orderBy);
        SortedMap<String, TreeSet<TestResult>> testResults = new TreeMap<>();
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    TestResult lastTestResult = null;
                    do {

                        ConnectionType connectionType = ConnectionType.valueOf(c.getString(c.getColumnIndex(ConnectionTestStatsColumns.TYPE)));
                        int resultCount = c.getInt(c.getColumnIndex(ConnectionTestStatsColumns.TEST_COUNT));
                        String resultType = c.getString(c.getColumnIndex(ConnectionTestStatsColumns.TEST_RESULT));

                        String label = c.getString(c.getColumnIndex(ConnectionTestStatsColumns.LABEL));
                        String id1 = c.getString(c.getColumnIndex(ConnectionTestStatsColumns.ID1));
                        String id2 = c.getString(c.getColumnIndex(ConnectionTestStatsColumns.ID2));
                        String id3 = c.getString(c.getColumnIndex(ConnectionTestStatsColumns.ID3));
                        if (lastTestResult == null) lastTestResult = createTestResult(connectionType, label, id1, id2, id3);
                        boolean newCell = !TextUtils.equals(id1, lastTestResult.id1) || !TextUtils.equals(id2, lastTestResult.id2)
                                || !TextUtils.equals(id3, lastTestResult.id3);
                        if (newCell) {
                            add(testResults, lastTestResult.label, lastTestResult);
                            lastTestResult = createTestResult(connectionType, label, id1, id2, id3);
                        }
                        if (!TextUtils.isEmpty(resultType)) {
                            switch(resultType) {
                                case Constants.CONNECTION_TEST_PASS:
                                    lastTestResult.setPassCount(resultCount);
                                    break;
                                case Constants.CONNECTION_TEST_FAIL:
                                    lastTestResult.setFailCount(resultCount);
                                    break;
                                case Constants.CONNECTION_TEST_SLOW:
                                    lastTestResult.setSlowCount(resultCount);
                                    break;
                                default:
                                    break;
                            }
                        }
                        if (c.isLast()) add(testResults, lastTestResult.label, lastTestResult);
                    } while (c.moveToNext());
                }
            } finally {
                c.close();
            }
        }
        return generateReport(context, testResults);
    }

    /**
     * @return a user-friendly string including the earliest and latest timestamps of all data collected.
     */
    public static String getDataCollectionDateRange(Context context) {
        Log.v(TAG, "getDataCollectionDateRange");
        String[] projection = new String[] { "MIN(" + NetMonColumns.TIMESTAMP + ")", "MAX(" + NetMonColumns.TIMESTAMP + ")" };
        Cursor c = context.getContentResolver().query(NetMonColumns.CONTENT_URI, projection, null, null, null);
        String dateRange = "";
        if (c != null) {
            if (c.moveToNext()) {
                long firstTimestamp = c.getLong(0);
                long lastTimestamp = c.getLong(1);
                dateRange = DateUtils.formatDateRange(context, firstTimestamp, lastTimestamp, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
            }
            c.close();
        }
        return dateRange;
    }

    private static <T> void add(Map<String, TreeSet<T>> map, String key, T value) {
        TreeSet<T> set = map.get(key);
        if (set == null) {
            set = new TreeSet<>();
            map.put(key, set);
        }
        set.add(value);
    }

    private static String generateReport(Context context, SortedMap<String, TreeSet<TestResult>> cellResults) {
        StringBuilder sb = new StringBuilder();
        sb.append(Build.MODEL).append("/").append(Build.VERSION.RELEASE).append("\n");
        if (cellResults == null || cellResults.size() == 0) {
            sb.append(context.getString(R.string.export_error_no_mobile_tests));
        } else
            for (String extraInfo : cellResults.keySet()) {
                sb.append(extraInfo).append(":\n");
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

    private static TestResult createTestResult(ConnectionType connectionType, String label, String id1, String id2, String id3) {
        switch (connectionType) {
            case GSM:
                return new GsmCellResult(label, id1, id2, id3);
            case CDMA:
                return new CdmaCellResult(label, id1, id2, id3);
            case WIFI:
            default:
                return new WiFiResult(label, id1);
        }
    }
}
