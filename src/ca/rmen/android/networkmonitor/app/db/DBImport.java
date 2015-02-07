/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.RemoteException;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.provider.NetMonProvider;
import ca.rmen.android.networkmonitor.util.IoUtil;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Replace the contents of the current database with the contents of another database.
 * This is based on DBImport from the scrum chatter project.
 */
public class DBImport {
    private static final String TAG = Constants.TAG + "/" + DBImport.class.getSimpleName();

    /**
     * Replace the database of our app with the contents of the database found at the given uri.
     */
    public static void importDB(Context context, Uri uri) throws RemoteException, OperationApplicationException, IOException {
        if (uri.getScheme().equals("file")) {
            File db = new File(uri.getEncodedPath());
            importDB(context, db);
        } else {
            InputStream is = context.getContentResolver().openInputStream(uri);
            File tempDb = new File(context.getCacheDir(), "temp" + System.currentTimeMillis() + ".db");
            FileOutputStream os = new FileOutputStream(tempDb);
            if (IoUtil.copy(is, os) > 0) {
                importDB(context, tempDb);
                tempDb.delete();
            }
        }
    }

    /**
     * In a single database transaction, delete all the cells from the current database, read the data from the given importDb file, create a batch of
     * corresponding insert operations, and execute the inserts.
     */
    private static void importDB(Context context, File importDb) throws RemoteException, OperationApplicationException, FileNotFoundException {
        Log.v(TAG, "importDB from " + importDb);
        SQLiteDatabase dbImport = SQLiteDatabase.openDatabase(importDb.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        operations.add(ContentProviderOperation.newDelete(NetMonColumns.CONTENT_URI).build());
        Uri insertUri = new Uri.Builder().authority(NetMonProvider.AUTHORITY).appendPath(NetMonColumns.TABLE_NAME)
                .appendQueryParameter(NetMonProvider.QUERY_PARAMETER_NOTIFY, "false").build();
        buildInsertOperations(context, dbImport, insertUri, NetMonColumns.TABLE_NAME, operations);
        dbImport.close();
    }

    /**
     * Read all cells from the given table from the dbImport database, and add corresponding insert operations to the operations parameter.
     *
     * @throws OperationApplicationException
     * @throws RemoteException
     */
    private static void buildInsertOperations(Context context, SQLiteDatabase dbImport, Uri uri, String table, ArrayList<ContentProviderOperation> operations)
            throws RemoteException, OperationApplicationException {
        Log.v(TAG, "buildInsertOperations: uri = " + uri + ", table=" + table);
        Cursor c = dbImport.query(false, table, null, null, null, null, null, null, null);
        if (c != null) {
            try {
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
                            context.getContentResolver().applyBatch(NetMonProvider.AUTHORITY, operations);
                            operations.clear();
                        }
                    } while (c.moveToNext());
                    if (operations.size() > 0) context.getContentResolver().applyBatch(NetMonProvider.AUTHORITY, operations);
                }
            } finally {
                c.close();
            }
        }
    }
}
