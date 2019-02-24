/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013-2015 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.provider;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ca.rmen.android.networkmonitor.BuildConfig;
import ca.rmen.android.networkmonitor.Constants;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

public class NetMonProvider extends ContentProvider { // NO_UCD (use default)
    private static final String TAG = Constants.TAG + NetMonProvider.class.getSimpleName();

    private static final String TYPE_CURSOR_ITEM = "vnd.android.cursor.item/";
    private static final String TYPE_CURSOR_DIR = "vnd.android.cursor.dir/";

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".provider";
    static final String CONTENT_URI_BASE = "content://" + AUTHORITY;

    public static final String QUERY_PARAMETER_NOTIFY = "QUERY_PARAMETER_NOTIFY";
    public static final String QUERY_PARAMETER_LIMIT = "QUERY_PARAMETER_LIMIT";
    private static final String QUERY_PARAMETER_GROUP_BY = "QUERY_PARAMETER_GROUP_BY";

    private static final int URI_TYPE_NETWORKMONITOR = 0;
    private static final int URI_TYPE_NETWORKMONITOR_ID = 1;
    private static final int URI_TYPE_SUMMARY = 2;
    private static final int URI_TYPE_UNIQUE_VALUES_ID = 3;

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    private Context mContext;

    static {
        URI_MATCHER.addURI(AUTHORITY, NetMonColumns.TABLE_NAME, URI_TYPE_NETWORKMONITOR);
        URI_MATCHER.addURI(AUTHORITY, NetMonColumns.TABLE_NAME + "/#", URI_TYPE_NETWORKMONITOR_ID);
        URI_MATCHER.addURI(AUTHORITY, ConnectionTestStatsColumns.VIEW_NAME, URI_TYPE_SUMMARY);
        URI_MATCHER.addURI(AUTHORITY, UniqueValuesColumns.NAME + "/*", URI_TYPE_UNIQUE_VALUES_ID);
    }

    private NetMonDatabase mNetworkMonitorDatabase;

    @Override
    public boolean onCreate() {
        mNetworkMonitorDatabase = new NetMonDatabase(mContext);
        return true;
    }

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        mContext = context;
        super.attachInfo(context, info);
    }

    @Override
    public String getType(@NonNull Uri uri) {
        final int match = URI_MATCHER.match(uri);
        switch (match) {
            case URI_TYPE_NETWORKMONITOR:
            case URI_TYPE_SUMMARY:
                return TYPE_CURSOR_DIR + NetMonColumns.TABLE_NAME;
            case URI_TYPE_NETWORKMONITOR_ID:
                return TYPE_CURSOR_ITEM + NetMonColumns.TABLE_NAME;
            case URI_TYPE_UNIQUE_VALUES_ID:
                return TYPE_CURSOR_DIR + UniqueValuesColumns.NAME;
        }
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        Log.d(TAG, "insert uri=" + uri + " values=" + values);
        final String table = uri.getLastPathSegment();
        final long rowId = mNetworkMonitorDatabase.getWritableDatabase().insert(table, null, values);
        String notify;
        if (rowId != -1 && ((notify = uri.getQueryParameter(QUERY_PARAMETER_NOTIFY)) == null || "true".equals(notify))) {
            mContext.getContentResolver().notifyChange(uri, null);
        }
        return uri.buildUpon().appendEncodedPath(String.valueOf(rowId)).build();
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
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
        if (res != 0 && ((notify = uri.getQueryParameter(QUERY_PARAMETER_NOTIFY)) == null || "true".equals(notify))) {
            mContext.getContentResolver().notifyChange(uri, null);
        }

        return res;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.d(TAG, "update uri=" + uri + " values=" + values + " selection=" + selection);
        final QueryParams queryParams = getQueryParams(uri, selection);
        final int res = mNetworkMonitorDatabase.getWritableDatabase().update(queryParams.table, values, queryParams.whereClause, selectionArgs);
        String notify;
        if (res != 0 && ((notify = uri.getQueryParameter(QUERY_PARAMETER_NOTIFY)) == null || "true".equals(notify))) {
            mContext.getContentResolver().notifyChange(uri, null);
        }
        return res;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        Log.d(TAG, "delete uri=" + uri + " selection=" + selection);
        final QueryParams queryParams = getQueryParams(uri, selection);
        final int res = mNetworkMonitorDatabase.getWritableDatabase().delete(queryParams.table, queryParams.whereClause, selectionArgs);
        String notify;
        if (res != 0 && ((notify = uri.getQueryParameter(QUERY_PARAMETER_NOTIFY)) == null || "true".equals(notify))) {
            mContext.getContentResolver().notifyChange(uri, null);
        }
        return res;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final String groupBy = uri.getQueryParameter(QUERY_PARAMETER_GROUP_BY);
        final String limit = uri.getQueryParameter(QUERY_PARAMETER_LIMIT);
        Log.d(TAG,
                "query uri=" + uri + ", projection = " + Arrays.toString(projection) + ", selection=" + selection + ", selectionArgs = "
                        + Arrays.toString(selectionArgs) + ", sortOrder=" + sortOrder + ", groupBy=" + groupBy);

        final int matchedId = URI_MATCHER.match(uri);
        final Cursor res;
        switch (matchedId) {
            case URI_TYPE_NETWORKMONITOR:
            case URI_TYPE_NETWORKMONITOR_ID:

                final QueryParams queryParams = getQueryParams(uri, selection);
                res = mNetworkMonitorDatabase.getReadableDatabase().query(queryParams.table, projection, queryParams.whereClause, selectionArgs, groupBy, null,
                        sortOrder == null ? queryParams.orderBy : sortOrder, limit);
                break;
            case URI_TYPE_SUMMARY:
                res = mNetworkMonitorDatabase.getReadableDatabase().query(ConnectionTestStatsColumns.VIEW_NAME, projection, selection, selectionArgs, groupBy,
                        null, sortOrder, limit);
                break;
            case URI_TYPE_UNIQUE_VALUES_ID:
                String columnName = uri.getLastPathSegment();
                Map<String, String> projectionMap = new HashMap<>();
                projectionMap.put(UniqueValuesColumns.VALUE, columnName);
                projectionMap.put(UniqueValuesColumns.COUNT, "count(*)");
                SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
                qb.setDistinct(true);
                qb.setTables(NetMonColumns.TABLE_NAME);
                qb.setProjectionMap(projectionMap);
                res = qb.query(mNetworkMonitorDatabase.getReadableDatabase(), projection, selection, selectionArgs, columnName, null, sortOrder, limit);
                break;
            default:
                return null;
        }
        res.setNotificationUri(mContext.getContentResolver(), uri);
        logCursor(res, selectionArgs);
        return res;
    }

    /**
     * Perform all operations in a single transaction and notify all relevant URIs at the end.
     *
     * @see android.content.ContentProvider#applyBatch(java.util.ArrayList)
     */
    @Override
    public @NonNull ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        Log.v(TAG, "applyBatch: " + operations.size());
        Set<Uri> urisToNotify = StreamSupport.stream(operations)
                        .map(ContentProviderOperation::getUri)
                        .collect(Collectors.toSet());
        Log.v(TAG, "applyBatch: will notify these uris after persisting: " + urisToNotify);
        SQLiteDatabase db = mNetworkMonitorDatabase.getWritableDatabase();
        db.beginTransaction();
        try {
            int batchSize = 100;
            int operationsProcessed = 0;
            ContentProviderResult[] result = new ContentProviderResult[operations.size()];
            while (!operations.isEmpty()) {
                ArrayList<ContentProviderOperation> batch = new ArrayList<>(batchSize);
                for (int i = 0; i < batchSize && !operations.isEmpty(); i++)
                    batch.add(operations.remove(0));
                Log.v(TAG, "applyBatch of " + batch.size() + " operations");
                ContentProviderResult[] batchResult = super.applyBatch(batch);
                System.arraycopy(batchResult, 0, result, operationsProcessed, batchResult.length);
                operationsProcessed += batch.size();
            }
            db.setTransactionSuccessful();
            for (Uri uri : urisToNotify)
                mContext.getContentResolver().notifyChange(uri, null);
            return result;
        } finally {
            db.endTransaction();
        }
    }

    private static class QueryParams {
        public String table;
        public String whereClause;
        public String orderBy;
    }


    private QueryParams getQueryParams(Uri uri, String selection) {
        final QueryParams res = new QueryParams();
        String id = null;
        final int matchedId = URI_MATCHER.match(uri);
        switch (matchedId) {
            case URI_TYPE_NETWORKMONITOR:
            case URI_TYPE_NETWORKMONITOR_ID:
                res.table = NetMonColumns.TABLE_NAME;
                res.orderBy = NetMonColumns.DEFAULT_ORDER;
                break;
            case URI_TYPE_SUMMARY:
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

    /**
     * Log the query of the given cursor.
     */
    private void logCursor(Cursor cursor, String[] selectionArgs) {
        try {
            Field queryField = SQLiteCursor.class.getDeclaredField("mQuery");
            queryField.setAccessible(true);
            SQLiteQuery sqliteQuery = (SQLiteQuery) queryField.get(cursor);
            Log.v(TAG, sqliteQuery.toString() + ": " + Arrays.toString(selectionArgs));
        } catch (Exception e) {
            Log.v(TAG, e.getMessage(), e);
        }
    }
}
