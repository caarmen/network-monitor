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
 * Copyright (C) 2013 Benoit 'BoD' Lubek (BoD@JRAF.org)
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
package org.jraf.android.networkmonitor.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import org.jraf.android.networkmonitor.Constants;

public class NetMonProvider extends ContentProvider {
    private static final String TAG = Constants.TAG + NetMonProvider.class.getSimpleName();

    private static final String TYPE_CURSOR_ITEM = "vnd.android.cursor.item/";
    private static final String TYPE_CURSOR_DIR = "vnd.android.cursor.dir/";

    public static final String AUTHORITY = "org.jraf.android.networkmonitor.provider";
    public static final String CONTENT_URI_BASE = "content://" + AUTHORITY;

    public static final String QUERY_NOTIFY = "QUERY_NOTIFY";
    public static final String QUERY_GROUP_BY = "QUERY_GROUP_BY";

    private static final int URI_TYPE_NETWORKMONITOR = 0;
    private static final int URI_TYPE_NETWORKMONITOR_ID = 1;
    private static final int URI_TYPE_GSM_SUMMARY = 2;
    private static final int URI_TYPE_CDMA_SUMMARY = 3;

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(AUTHORITY, NetMonColumns.TABLE_NAME, URI_TYPE_NETWORKMONITOR);
        URI_MATCHER.addURI(AUTHORITY, NetMonColumns.TABLE_NAME + "/#", URI_TYPE_NETWORKMONITOR_ID);
        URI_MATCHER.addURI(AUTHORITY, NetMonColumns.TABLE_NAME + NetMonColumns.GSM_SUMMARY, URI_TYPE_GSM_SUMMARY);
        URI_MATCHER.addURI(AUTHORITY, NetMonColumns.TABLE_NAME + NetMonColumns.CDMA_SUMMARY, URI_TYPE_CDMA_SUMMARY);
    }

    private NetMonDatabase mNetworkMonitorDatabase;

    @Override
    public boolean onCreate() {
        mNetworkMonitorDatabase = new NetMonDatabase(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        final int match = URI_MATCHER.match(uri);
        switch (match) {
            case URI_TYPE_NETWORKMONITOR:
                return TYPE_CURSOR_DIR + NetMonColumns.TABLE_NAME;
            case URI_TYPE_NETWORKMONITOR_ID:
                return TYPE_CURSOR_ITEM + NetMonColumns.TABLE_NAME;
            case URI_TYPE_GSM_SUMMARY:
                return TYPE_CURSOR_DIR + NetMonColumns.TABLE_NAME;
            case URI_TYPE_CDMA_SUMMARY:
                return TYPE_CURSOR_DIR + NetMonColumns.TABLE_NAME;

        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "insert uri=" + uri + " values=" + values);
        final String table = uri.getLastPathSegment();
        final long rowId = mNetworkMonitorDatabase.getWritableDatabase().insert(table, null, values);
        String notify;
        if (rowId != -1 && ((notify = uri.getQueryParameter(QUERY_NOTIFY)) == null || "true".equals(notify))) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return uri.buildUpon().appendEncodedPath(String.valueOf(rowId)).build();
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        Log.d(TAG, "bulkInsert uri=" + uri + " values.length=" + values.length);
        final String table = uri.getLastPathSegment();
        final SQLiteDatabase db = mNetworkMonitorDatabase.getWritableDatabase();
        int res = 0;
        db.beginTransaction();
        try {
            for (final ContentValues v : values) {
                final long id = db.insert(table, null, v);
                if (id != -1) {
                    res++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        String notify;
        if (res != 0 && ((notify = uri.getQueryParameter(QUERY_NOTIFY)) == null || "true".equals(notify))) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return res;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.d(TAG, "update uri=" + uri + " values=" + values + " selection=" + selection);
        final QueryParams queryParams = getQueryParams(uri, selection, false);
        final int res = mNetworkMonitorDatabase.getWritableDatabase().update(queryParams.table, values, queryParams.whereClause, selectionArgs);
        String notify;
        if (res != 0 && ((notify = uri.getQueryParameter(QUERY_NOTIFY)) == null || "true".equals(notify))) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return res;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.d(TAG, "delete uri=" + uri + " selection=" + selection);
        final QueryParams queryParams = getQueryParams(uri, selection, false);
        final int res = mNetworkMonitorDatabase.getWritableDatabase().delete(queryParams.table, queryParams.whereClause, selectionArgs);
        String notify;
        if (res != 0 && ((notify = uri.getQueryParameter(QUERY_NOTIFY)) == null || "true".equals(notify))) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return res;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final String groupBy = uri.getQueryParameter(QUERY_GROUP_BY);
        Log.d(TAG, "query uri=" + uri + " selection=" + selection + " sortOrder=" + sortOrder + " groupBy=" + groupBy);

        final int matchedId = URI_MATCHER.match(uri);
        final Cursor res;
        switch (matchedId) {
            case URI_TYPE_NETWORKMONITOR:
            case URI_TYPE_NETWORKMONITOR_ID:

                final QueryParams queryParams = getQueryParams(uri, selection, true);
                res = mNetworkMonitorDatabase.getReadableDatabase().query(queryParams.table, projection, queryParams.whereClause, selectionArgs, groupBy, null,
                        sortOrder == null ? queryParams.orderBy : sortOrder);
                break;
            case URI_TYPE_GSM_SUMMARY:
                String[] gsmCellIdColumns = new String[] { NetMonColumns.GSM_CELL_LAC, NetMonColumns.GSM_SHORT_CELL_ID, NetMonColumns.GSM_FULL_CELL_ID };
                res = buildGoogleConnectionTestCursor(gsmCellIdColumns);
                break;
            case URI_TYPE_CDMA_SUMMARY:
                String[] cdmaCellIdColumns = new String[] { NetMonColumns.CDMA_CELL_BASE_STATION_ID, NetMonColumns.CDMA_CELL_NETWORK_ID,
                        NetMonColumns.CDMA_CELL_SYSTEM_ID };
                res = buildGoogleConnectionTestCursor(cdmaCellIdColumns);
                break;
            default:
                return null;
        }
        res.setNotificationUri(getContext().getContentResolver(), uri);
        return res;
    }

    private static class QueryParams {
        public String table;
        public String whereClause;
        public String orderBy;
    }

    /**
     * @param cellIdColumns the columns in the networkmonitor table which uniquely identify a cell.
     * @return a Cursor which returns the number of passes and fails for each cell.
     */
    private Cursor buildGoogleConnectionTestCursor(String[] cellIdColumns) {
        String mainTableAlias = "n";
        String passSubquery = buildGoogleConnectionTestSubQuery(cellIdColumns, mainTableAlias, "pass", NetMonColumns.PASS_COUNT);
        String failSubquery = buildGoogleConnectionTestSubQuery(cellIdColumns, mainTableAlias, "fail", NetMonColumns.FAIL_COUNT);
        String[] dbProjection = new String[cellIdColumns.length + 2];
        System.arraycopy(cellIdColumns, 0, dbProjection, 0, cellIdColumns.length);
        dbProjection[dbProjection.length - 2] = passSubquery;
        dbProjection[dbProjection.length - 1] = failSubquery;
        String dbTable = NetMonColumns.TABLE_NAME + " as " + mainTableAlias;
        String dbSelection = mainTableAlias + "." + NetMonColumns.DATA_STATE + " = ?";
        String[] dbSelectionArgs = new String[] { Constants.CONNECTION_TEST_PASS, Constants.DATA_STATE_CONNECTED, Constants.CONNECTION_TEST_FAIL,
                Constants.DATA_STATE_CONNECTED, Constants.DATA_STATE_CONNECTED };
        String dbGroupBy = TextUtils.join(",", cellIdColumns);
        String dbOrderBy = null;

        Cursor cursor = mNetworkMonitorDatabase.getReadableDatabase().query(dbTable, dbProjection, dbSelection, dbSelectionArgs, dbGroupBy, null, dbOrderBy);
        return cursor;
    }

    /**
     * @param cellIdColumns the columns in the networkmonitor table which uniquely identify a cell
     * @param mainTableAlias the alias for the networkmonitor table in the main query
     * @param subQueryTableAlias the alias for the networkmonitor table in the subquery
     * @param subqueryAlias the alias of the whole subquery.
     * @return a query which returns the number of passes or fails
     */
    private String buildGoogleConnectionTestSubQuery(String[] cellIdColumns, String mainTableAlias, String subQueryTableAlias, String subqueryAlias) {
        String tableAlias = NetMonColumns.TABLE_NAME + "_" + subQueryTableAlias;
        String query = "( SELECT COUNT(" + NetMonColumns.GOOGLE_CONNECTION_TEST + ") " + " FROM " + NetMonColumns.TABLE_NAME + " " + tableAlias + " WHERE ";
        // Join the subquery to the main query.
        StringBuilder join = new StringBuilder();
        for (String cellIdColumn : cellIdColumns) {
            join.append(tableAlias + "." + cellIdColumn + "=" + mainTableAlias + "." + cellIdColumn + " AND ");
        }
        query += join.toString();
        // Filter on the pass/fail value.
        // Include only tests where the data connection was CONNECTED.
        query += tableAlias + "." + NetMonColumns.GOOGLE_CONNECTION_TEST + "=? " + " AND " + tableAlias + "." + NetMonColumns.DATA_STATE + "=?";
        query += ") as " + subqueryAlias;
        return query;
    }

    private QueryParams getQueryParams(Uri uri, String selection, boolean isQuery) {
        final QueryParams res = new QueryParams();
        String id = null;
        final int matchedId = URI_MATCHER.match(uri);
        switch (matchedId) {
            case URI_TYPE_NETWORKMONITOR:
            case URI_TYPE_NETWORKMONITOR_ID:
                res.table = NetMonColumns.TABLE_NAME;
                res.orderBy = NetMonColumns.DEFAULT_ORDER;
                break;
            case URI_TYPE_GSM_SUMMARY:
            case URI_TYPE_CDMA_SUMMARY:
                // Nothing to do here.  We will construct our query params in query().
                break;
            default:
                throw new IllegalArgumentException("The uri '" + uri + "' is not supported by this ContentProvider");
        }

        switch (matchedId) {
            case URI_TYPE_NETWORKMONITOR_ID:
                id = uri.getLastPathSegment();
        }
        if (id != null) {
            if (selection != null) {
                res.whereClause = BaseColumns._ID + "=" + id + " and (" + selection + ")";
            } else {
                res.whereClause = BaseColumns._ID + "=" + id;
            }
        } else {
            res.whereClause = selection;
        }
        return res;
    }

    public static Uri notify(Uri uri, boolean notify) {
        return uri.buildUpon().appendQueryParameter(QUERY_NOTIFY, String.valueOf(notify)).build();
    }

    public static Uri groupBy(Uri uri, String groupBy) {
        return uri.buildUpon().appendQueryParameter(QUERY_GROUP_BY, groupBy).build();
    }
}
