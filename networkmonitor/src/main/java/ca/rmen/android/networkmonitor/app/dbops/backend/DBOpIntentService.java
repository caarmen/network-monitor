/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2015 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.dbops.backend;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import java.io.File;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.ProgressListener;
import ca.rmen.android.networkmonitor.app.dbops.backend.clean.DBCompress;
import ca.rmen.android.networkmonitor.app.dbops.backend.clean.DBPurge;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.CSVExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.DBExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.ExcelExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.FileExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.HTMLExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.kml.KMLExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.imp0rt.DBImport;
import ca.rmen.android.networkmonitor.app.main.MainActivity;
import ca.rmen.android.networkmonitor.util.Log;

/**
 */
public class DBOpIntentService extends IntentService {

    private static final String TAG = Constants.TAG + DBOpIntentService.class.getSimpleName();
    private NotificationProgressListener mCompressProgressListener;
    private NotificationProgressListener mPurgeProgressListener;
    private NotificationProgressListener mExportProgressListener;
    private NotificationProgressListener mImportProgressListener;

    public enum ExportFormat {
        CSV,
        DB,
        EXCEL,
        HTML,
        KML,
        SUMMARY
    }

    private static final String ACTION_COMPRESS = "ca.rmen.android.networkmonitor.app.dbops.backend.action.COMPRESS";
    private static final String ACTION_PURGE = "ca.rmen.android.networkmonitor.app.dbops.backend.action.PURGE";
    private static final String ACTION_EXPORT = "ca.rmen.android.networkmonitor.app.dbops.backend.action.EXPORT";
    private static final String ACTION_IMPORT = "ca.rmen.android.networkmonitor.app.dbops.backend.action.IMPORT";

    private static final String EXTRA_PURGE_NUM_ROWS_TO_KEEP = "ca.rmen.android.networkmonitor.app.dbops.backend.extra.PURGE_NUM_ROWS_TO_KEEP";
    private static final String EXTRA_EXPORT_FORMAT = "ca.rmen.android.networkmonitor.app.dbops.backend.extra.EXPORT_FILE_FORMAT";
    private static final String EXTRA_EXPORT_KML_PLACEMARK_COLUMN_NAME = "ca.rmen.android.networkmonitor.app.dbops.backend.extra.EXPORT_KML_PLACEMARK_COLUMN_NAME";


    public static void startActionCompress(Context context) {
        Intent intent = new Intent(context, DBOpIntentService.class);
        intent.setAction(ACTION_COMPRESS);
        context.startService(intent);
    }

    public static void startActionPurge(Context context, int numRowsToKeep) {
        Intent intent = new Intent(context, DBOpIntentService.class);
        intent.setAction(ACTION_PURGE);
        intent.putExtra(EXTRA_PURGE_NUM_ROWS_TO_KEEP, numRowsToKeep);
        context.startService(intent);
    }

    public static void startActionExport(Context context, ExportFormat exportFormat) {
        Intent intent = new Intent(context, DBOpIntentService.class);
        intent.setAction(ACTION_EXPORT);
        intent.putExtra(EXTRA_EXPORT_FORMAT, exportFormat);
        context.startService(intent);
    }

    public static void startActionKMLExport(Context context, String placemarkNameColumn) {
        Intent intent = new Intent(context, DBOpIntentService.class);
        intent.setAction(ACTION_EXPORT);
        intent.putExtra(EXTRA_EXPORT_FORMAT, ExportFormat.KML);
        intent.putExtra(EXTRA_EXPORT_KML_PLACEMARK_COLUMN_NAME, placemarkNameColumn);
        context.startService(intent);
    }

    public static void startActionImport(Context context, Uri uri) {
        Intent intent = new Intent(context, DBOpIntentService.class);
        intent.setAction(ACTION_IMPORT);
        intent.setData(uri);
        context.startService(intent);
    }

    public DBOpIntentService() {
        super("DBOpIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mCompressProgressListener =
                new NotificationProgressListener(this,
                        DBCompress.class.hashCode(),
                        R.drawable.ic_stat_db_op_compress,
                        R.string.db_op_compress_progress_title,
                        R.string.db_op_compress_progress_content,
                        R.string.db_op_compress_complete_title,
                        R.string.db_op_compress_complete_content);
        mPurgeProgressListener =
                new NotificationProgressListener(this,
                        DBPurge.class.hashCode(),
                        R.drawable.ic_stat_db_op_delete,
                        R.string.db_op_purge_progress_title,
                        R.string.db_op_purge_progress_content,
                        R.string.db_op_purge_complete_title,
                        R.string.db_op_purge_complete_content);
        mExportProgressListener =
                new NotificationProgressListener(this,
                        FileExport.class.hashCode(),
                        R.drawable.ic_stat_db_op_export,
                        R.string.db_op_export_progress_title,
                        R.string.db_op_export_progress_content,
                        R.string.db_op_export_complete_title,
                        R.string.db_op_export_complete_content);
        mImportProgressListener =
                new NotificationProgressListener(this,
                        DBImport.class.hashCode(),
                        R.drawable.ic_stat_db_op_import,
                        R.string.db_op_import_progress_title,
                        R.string.db_op_import_progress_content,
                        R.string.db_op_import_complete_title,
                        R.string.db_op_import_complete_content);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_COMPRESS.equals(action)) {
                handleActionCompress();
            } else if (ACTION_PURGE.equals(action)) {
                final int numRowsToKeep = intent.getIntExtra(EXTRA_PURGE_NUM_ROWS_TO_KEEP, 0);
                handleActionPurge(numRowsToKeep);
            } else if (ACTION_EXPORT.equals(action)) {
                final ExportFormat exportFileFormat = (ExportFormat) intent.getSerializableExtra(EXTRA_EXPORT_FORMAT);
                handleActionExport(exportFileFormat, intent.getExtras());
            } else if (ACTION_IMPORT.equals(action)) {
                final Uri uri = intent.getData();
                handleActionImport(uri);
            }
        }
    }

    private void handleActionCompress() {
        Log.d(TAG, "handleActionCompress()");
        DBCompress dbCompress = new DBCompress(this);
        int numRowsDeleted = dbCompress.execute(mCompressProgressListener);
    }

    private void handleActionPurge(int numRowsToKeep) {
        Log.d(TAG, "handleActionPurge() called with " + "numRowsToKeep = [" + numRowsToKeep + "]");
        DBPurge dbPurge = new DBPurge(this, numRowsToKeep);
        int numRowsDeleted = dbPurge.execute(mPurgeProgressListener);
    }

    private void handleActionExport(ExportFormat exportFileFormat, Bundle extras) {
        Log.d(TAG, "handleActionExport() called with exportFileFormat = [" + exportFileFormat + "]");
        final FileExport fileExport;
        switch (exportFileFormat) {
            case CSV:
                fileExport = new CSVExport(this);
                break;
            case DB:
                fileExport = new DBExport(this);
                break;
            case EXCEL:
                fileExport = new ExcelExport(this);
                break;
            case HTML:
                fileExport = new HTMLExport(this, true);
                break;
            case KML:
                String placemarkColumnName = extras.getString(EXTRA_EXPORT_KML_PLACEMARK_COLUMN_NAME);
                fileExport = new KMLExport(this, placemarkColumnName);
                break;
            case SUMMARY:
            default:
                fileExport = null;
        }
        File file = null;
        if (fileExport != null) {
            file = fileExport.execute(mExportProgressListener);
        }
        Intent shareIntent = FileExport.getShareIntent(this, file);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_stat_db_op_export);
        builder.setTicker(getString(R.string.db_op_export_complete_title));
        builder.setContentTitle(getString(R.string.db_op_export_complete_title));
        builder.setContentText(getString(R.string.db_op_export_complete_content));
        builder.setAutoCancel(true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, shareIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        Notification notification = builder.build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(FileExport.class.hashCode(), notification);
    }

    private void handleActionImport(Uri uri) {
        Log.d(TAG, "handleActionImport() called with " + "uri = [" + uri + "]");
        DBImport dbImport = new DBImport(this, uri);
        if (dbImport.execute(mImportProgressListener)) {

        }
    }

    private static class NotificationProgressListener implements ProgressListener {
        private final Context mContext;
        private NotificationManager mNotificationManager;
        private final int mNotificationId;
        private final int mNotificationIcon;
        private final int mNotificationProgressTitleId;
        private final int mNotificationProgressContentId;
        private final int mNotificationCompleteTitleId;
        private final int mNotificationCompleteContentId;

        public NotificationProgressListener(Context context,
                                            int notificationId,
                                            int notificationIcon,
                                            int notificationProgressTitleId,
                                            int notificationProgressContentId,
                                            int notificationCompleteTitleId,
                                            int notificationCompleteContentId) {
            mContext = context;
            mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationId = notificationId;
            mNotificationIcon = notificationIcon;
            mNotificationProgressTitleId = notificationProgressTitleId;
            mNotificationProgressContentId = notificationProgressContentId;
            mNotificationCompleteTitleId = notificationCompleteTitleId;
            mNotificationCompleteContentId = notificationCompleteContentId;
        }

        @Override
        public void onProgress(int progress, int max) {
            // Only update in increments of 5%
            int notifUpdateIncrement = (int) (max * 0.05);
            boolean updateNotification = progress == 0 || progress % notifUpdateIncrement == 0;
            if (!updateNotification) return;
            NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
            builder.setSmallIcon(mNotificationIcon);
            builder.setTicker(mContext.getString(mNotificationProgressTitleId));
            builder.setContentTitle(mContext.getString(mNotificationProgressTitleId));
            builder.setContentText(mContext.getString(mNotificationProgressContentId, progress, max));
            builder.setAutoCancel(false);
            builder.setContentIntent(PendingIntent.getActivity(mContext, 0, new Intent(mContext, MainActivity.class), progress == 0 ? 0 : PendingIntent.FLAG_UPDATE_CURRENT));
            Notification notification = builder.build();
            mNotificationManager.notify(mNotificationId, notification);
        }

        @Override
        public void onWarning(String message) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
            builder.setSmallIcon(R.drawable.ic_stat_warning);
            builder.setTicker(mContext.getString(mNotificationProgressTitleId));
            builder.setContentTitle(mContext.getString(mNotificationProgressTitleId));
            builder.setContentText(message);
            builder.setAutoCancel(false);
            builder.setContentIntent(PendingIntent.getActivity(mContext, 0, new Intent(mContext, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
            Notification notification = builder.build();
            mNotificationManager.notify(mNotificationId, notification);
        }

        @Override
        public void onComplete(int total) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
            builder.setSmallIcon(mNotificationIcon);
            builder.setTicker(mContext.getString(mNotificationCompleteTitleId));
            builder.setContentTitle(mContext.getString(mNotificationCompleteTitleId));
            builder.setContentText(mContext.getString(mNotificationCompleteContentId, total));
            builder.setAutoCancel(true);
            builder.setContentIntent(PendingIntent.getActivity(mContext, 0, new Intent(mContext, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
            Notification notification = builder.build();
            mNotificationManager.notify(mNotificationId, notification);
        }
    }


}
