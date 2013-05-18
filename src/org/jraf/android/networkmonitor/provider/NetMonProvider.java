package org.jraf.android.networkmonitor.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
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



    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(AUTHORITY, NetMonColumns.TABLE_NAME, URI_TYPE_NETWORKMONITOR);
        URI_MATCHER.addURI(AUTHORITY, NetMonColumns.TABLE_NAME + "/#", URI_TYPE_NETWORKMONITOR_ID);

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
        final QueryParams queryParams = getQueryParams(uri, selection, true);
        final Cursor res = mNetworkMonitorDatabase.getReadableDatabase().query(queryParams.tableWithJoins, projection, queryParams.whereClause, selectionArgs,
                groupBy, null, sortOrder == null ? queryParams.orderBy : sortOrder);
        res.setNotificationUri(getContext().getContentResolver(), uri);
        return res;
    }

    private static class QueryParams {
        public String table;
        public String tableWithJoins;
        public String whereClause;
        public String orderBy;
    }

    private QueryParams getQueryParams(Uri uri, String selection, boolean isQuery) {
        final QueryParams res = new QueryParams();
        String id = null;
        final int matchedId = URI_MATCHER.match(uri);
        switch (matchedId) {
            case URI_TYPE_NETWORKMONITOR:
            case URI_TYPE_NETWORKMONITOR_ID:
                res.table = res.tableWithJoins = NetMonColumns.TABLE_NAME;
                res.orderBy = NetMonColumns.DEFAULT_ORDER;
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
