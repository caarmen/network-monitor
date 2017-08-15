/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2017 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.dbops.backend.imp0rt;

import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.RemoteException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.ProgressListener;
import ca.rmen.android.networkmonitor.app.dbops.backend.DBOperation;
import ca.rmen.android.networkmonitor.app.dbops.ui.Share;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.provider.NetMonProvider;
import ca.rmen.android.networkmonitor.util.IoUtil;
import android.util.Log;

/**
 * Replace the contents of the current database with the contents of another database.
 * This is based on DBImport from the scrum chatter project.
 */
public class DBImport implements DBOperation {
    private static final String TAG = Constants.TAG + DBImport.class.getSimpleName();

    private final Context mContext;
    private final Uri mUri;
    private final AtomicBoolean mIsCanceled = new AtomicBoolean(false);

    public DBImport(Context context, Uri uri) {
        mContext = context;
        mUri = uri;
    }

    /**
     * Replace the database of our app with the contents of the database found at the given uri.
     */
    @Override
    public void execute(ProgressListener listener) {
        try {
            if (mUri.getScheme().equals("file")) {
                File db = new File(mUri.getEncodedPath());
                importDB(db, listener);
            } else {
                InputStream is = mContext.getContentResolver().openInputStream(mUri);
                File tempDb = new File(mContext.getCacheDir(), "temp" + System.currentTimeMillis() + ".db");
                FileOutputStream os = new FileOutputStream(tempDb);
                if (IoUtil.copy(is, os) > 0) {
                    importDB(tempDb, listener);
                    //noinspection ResultOfMethodCallIgnored
                    tempDb.delete();
                }
            }
        } catch (RemoteException | OperationApplicationException | SQLException | IOException e) {
            Log.w(TAG, "Error importing the db: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancel() {
        mIsCanceled.set(true);
    }

    /**
     * In a single database transaction, delete all the cells from the current database, read the data from the given importDb file, create a batch of
     * corresponding insert operations, and execute the inserts.
     */
    private void importDB(File importDb, ProgressListener listener) throws RemoteException, OperationApplicationException {
        Log.v(TAG, "importDB from " + importDb);
        SQLiteDatabase dbImport = SQLiteDatabase.openDatabase(importDb.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        operations.add(ContentProviderOperation.newDelete(NetMonColumns.CONTENT_URI).build());
        Uri insertUri = new Uri.Builder().authority(NetMonProvider.AUTHORITY).appendPath(NetMonColumns.TABLE_NAME)
                .appendQueryParameter(NetMonProvider.QUERY_PARAMETER_NOTIFY, "false").build();
        buildInsertOperations(dbImport, insertUri, operations, listener);
        dbImport.close();
    }

    /**
     * Read all cells from the given table from the dbImport database, and add corresponding insert operations to the operations parameter.
     *
     * @throws OperationApplicationException if the database couldn't insert the data.
     * @throws RemoteException if the database couldn't insert the data.
     */
    private void buildInsertOperations(SQLiteDatabase dbImport, Uri uri, ArrayList<ContentProviderOperation> operations, ProgressListener listener)
            throws RemoteException, OperationApplicationException {
        Log.v(TAG, "buildInsertOperations: uri = " + uri);
        try {
            Cursor c = dbImport.query(false, NetMonColumns.TABLE_NAME, null, null, null, null, null, null, null);
            if (c != null) {
                try {
                    int count = c.getCount();
                    if (c.moveToFirst()) {
                        int columnCount = c.getColumnCount();
                        do {
                            Builder builder = ContentProviderOperation.newInsert(uri);
                            for (int i = 0; i < columnCount; i++) {
                                String columnName = c.getColumnName(i);
                                Object value = c.getString(i);
                                builder.withValue(columnName, value);
                            }
                            operations.add(builder.build());
                            if (operations.size() >= 100) {
                                mContext.getContentResolver().applyBatch(NetMonProvider.AUTHORITY, operations);
                                operations.clear();
                            }
                            if (listener != null) listener.onProgress(c.getPosition(), count);
                        } while (c.moveToNext() && !mIsCanceled.get());
                        if (operations.size() > 0 && !mIsCanceled.get())
                            mContext.getContentResolver().applyBatch(NetMonProvider.AUTHORITY, operations);
                    }
                    if (listener != null) {
                        if (mIsCanceled.get())
                            listener.onError(mContext.getString(R.string.import_notif_canceled_content));
                        else
                            listener.onComplete(mContext.getString(R.string.import_notif_complete_content, Share.readDisplayName(mContext, mUri)));
                    }
                    return;
                } finally {
                    c.close();
                }
            }
        } catch (SQLiteException e) {
           Log.w(TAG, "Couldn't import database " + mUri, e);
        }
        if (listener != null) listener.onError(mContext.getString(R.string.import_notif_error_content, Share.readDisplayName(mContext, uri)));
    }
}
