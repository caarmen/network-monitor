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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.DrawableRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.squareup.otto.Produce;

import java.io.File;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.bus.NetMonBus;
import ca.rmen.android.networkmonitor.app.dbops.backend.clean.DBCompress;
import ca.rmen.android.networkmonitor.app.dbops.backend.clean.DBPurge;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.CSVExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.DBExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.ExcelExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.FileExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.GnuplotExport;
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
    private final static Object lock = new Object();

    public enum ExportFormat {
        CSV,
        DB,
        EXCEL,
        HTML,
        KML,
        GNUPLOT,
        SUMMARY
    }

    public static final String ACTION_STOP_DB_OP = "ca.rmen.android.networkmonitor.app.dbops.backend.action.STOP_DB_OP";

    private static final String ACTION_COMPRESS = "ca.rmen.android.networkmonitor.app.dbops.backend.action.COMPRESS";
    private static final String ACTION_PURGE = "ca.rmen.android.networkmonitor.app.dbops.backend.action.PURGE";
    private static final String ACTION_EXPORT = "ca.rmen.android.networkmonitor.app.dbops.backend.action.EXPORT";
    private static final String ACTION_IMPORT = "ca.rmen.android.networkmonitor.app.dbops.backend.action.IMPORT";

    private static final String EXTRA_PURGE_NUM_ROWS_TO_KEEP = "ca.rmen.android.networkmonitor.app.dbops.backend.extra.PURGE_NUM_ROWS_TO_KEEP";
    private static final String EXTRA_EXPORT_FORMAT = "ca.rmen.android.networkmonitor.app.dbops.backend.extra.EXPORT_FILE_FORMAT";
    private static final String EXTRA_EXPORT_KML_PLACEMARK_COLUMN_NAME = "ca.rmen.android.networkmonitor.app.dbops.backend.extra.EXPORT_KML_PLACEMARK_COLUMN_NAME";
    private static final String EXTRA_DB_OP_TOAST = "ca.rmen.android.networkmonitor.app.dbops.backend.extra.DP_OP_TOAST";
    private static final String EXTRA_DB_OP_NAME = "ca.rmen.android.networkmonitor.app.dbops.backend.extra.DP_OP_NAME";

    private DBOperation mDBOperation = null;
    private NetMonBus.DBOperationStarted mDBOperationStarted;


    public static void startActionCompress(Context context) {
        Intent intent = new Intent(context, DBOpIntentService.class);
        intent.setAction(ACTION_COMPRESS);
        intent.putExtra(EXTRA_DB_OP_TOAST, context.getString(R.string.compress_toast_start));
        intent.putExtra(EXTRA_DB_OP_NAME, context.getString(R.string.compress_feature_name));
        context.startService(intent);
    }

    public static void startActionPurge(Context context, int numRowsToKeep) {
        Intent intent = new Intent(context, DBOpIntentService.class);
        intent.setAction(ACTION_PURGE);
        intent.putExtra(EXTRA_PURGE_NUM_ROWS_TO_KEEP, numRowsToKeep);
        intent.putExtra(EXTRA_DB_OP_TOAST, context.getString(R.string.purge_toast_start));
        intent.putExtra(EXTRA_DB_OP_NAME, context.getString(R.string.purge_feature_name));
        context.startService(intent);
    }

    public static void startActionExport(Context context, ExportFormat exportFormat) {
        Intent intent = new Intent(context, DBOpIntentService.class);
        intent.setAction(ACTION_EXPORT);
        intent.putExtra(EXTRA_EXPORT_FORMAT, exportFormat);
        intent.putExtra(EXTRA_DB_OP_TOAST, context.getString(R.string.export_progress_preparing_export));
        intent.putExtra(EXTRA_DB_OP_NAME, context.getString(R.string.export_feature_name));
        context.startService(intent);
    }

    public static void startActionKMLExport(Context context, String placemarkNameColumn) {
        Intent intent = new Intent(context, DBOpIntentService.class);
        intent.setAction(ACTION_EXPORT);
        intent.putExtra(EXTRA_EXPORT_FORMAT, ExportFormat.KML);
        intent.putExtra(EXTRA_EXPORT_KML_PLACEMARK_COLUMN_NAME, placemarkNameColumn);
        intent.putExtra(EXTRA_DB_OP_TOAST, context.getString(R.string.export_progress_preparing_export));
        intent.putExtra(EXTRA_DB_OP_NAME, context.getString(R.string.export_feature_name));
        context.startService(intent);
    }

    public static void startActionImport(Context context, Uri uri) {
        Intent intent = new Intent(context, DBOpIntentService.class);
        intent.setAction(ACTION_IMPORT);
        intent.setData(uri);
        intent.putExtra(EXTRA_DB_OP_TOAST, context.getString(R.string.import_toast_start));
        intent.putExtra(EXTRA_DB_OP_NAME, context.getString(R.string.import_feature_name));
        context.startService(intent);
    }

    @SuppressWarnings("WeakerAccess")
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
                        R.string.compress_notif_progress_title,
                        R.string.compress_notif_progress_content,
                        R.string.compress_notif_complete_title);
        mPurgeProgressListener =
                new NotificationProgressListener(this,
                        DBPurge.class.hashCode(),
                        R.drawable.ic_stat_db_op_delete,
                        R.string.purge_notif_progress_title,
                        R.string.purge_notif_progress_content,
                        R.string.purge_notif_complete_title);
        mExportProgressListener =
                new NotificationProgressListener(this,
                        FileExport.class.hashCode(),
                        R.drawable.ic_stat_db_op_export,
                        R.string.export_notif_progress_title,
                        R.string.export_notif_progress_content,
                        R.string.export_notif_complete_title);
        mImportProgressListener =
                new NotificationProgressListener(this,
                        DBImport.class.hashCode(),
                        R.drawable.ic_stat_db_op_import,
                        R.string.import_notif_progress_title,
                        R.string.import_notif_progress_content,
                        R.string.import_notif_complete_title);
        NetMonBus.getBus().register(this);
        registerReceiver(mStopSelfReceiver, new IntentFilter(ACTION_STOP_DB_OP));
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            String dbOp = intent.getStringExtra(EXTRA_DB_OP_NAME);
            synchronized (lock) {
                mDBOperationStarted = new NetMonBus.DBOperationStarted(getString(R.string.db_op_in_progress, dbOp));
            }

            // Show a toast
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    String toast = intent.getStringExtra(EXTRA_DB_OP_TOAST);
                    Toast.makeText(DBOpIntentService.this, toast, Toast.LENGTH_LONG).show();
                    synchronized (lock) {
                        if (mDBOperationStarted != null) NetMonBus.getBus().post(mDBOperationStarted);
                    }
                }
            });

            // Do the db operation
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
            mDBOperation = null;
            synchronized (lock) {
                mDBOperationStarted = null;
            }
        }
    }

    @Override
    public void onDestroy() {
        NetMonBus.getBus().post(new NetMonBus.DBOperationEnded());
        NetMonBus.getBus().unregister(this);
        unregisterReceiver(mStopSelfReceiver);
        super.onDestroy();
    }

    @SuppressWarnings("unused")
    @Produce
    public NetMonBus.DBOperationStarted produceDBOperationStarted() {
        return mDBOperationStarted;
    }

    private void handleActionCompress() {
        Log.d(TAG, "handleActionCompress()");

        mDBOperation = new DBCompress(this);
        mDBOperation.execute(mCompressProgressListener);
    }

    private void handleActionPurge(int numRowsToKeep) {
        Log.d(TAG, "handleActionPurge() called with " + "numRowsToKeep = [" + numRowsToKeep + "]");
        mDBOperation = new DBPurge(this, numRowsToKeep);
        mDBOperation.execute(mPurgeProgressListener);
    }

    private void handleActionExport(ExportFormat exportFileFormat, Bundle extras) {
        Log.d(TAG, "handleActionExport() called with exportFileFormat = [" + exportFileFormat + "]");
        FileExport fileExport;
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
            case GNUPLOT:
                fileExport = new GnuplotExport(this);
                break;
            case SUMMARY:
            default:
                fileExport = null;
        }
        mDBOperation = fileExport;
        File file = null;
        if (fileExport != null) {
            fileExport.execute(mExportProgressListener);
            file = fileExport.getFile();
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Start the summary report
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification almostDoneNotification =
                prepareFileExportNotification(R.drawable.ic_stat_db_op_export, getString(R.string.export_notif_progress_title), getString(R.string.export_progress_finalizing_export), pendingIntent, false).build();
        notificationManager.notify(FileExport.class.hashCode(), almostDoneNotification);

        Intent shareIntent = FileExport.getShareIntent(this, file);

        // All done
        pendingIntent = PendingIntent.getActivity(this, 0, shareIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        final NotificationCompat.Builder notificationBuilder;
        if (fileExport != null && fileExport.isCanceled()) {
            notificationBuilder =  prepareFileExportNotification(R.drawable.ic_stat_action_done, getString(R.string.export_notif_canceled_title), getString(R.string.export_notif_canceled_content), pendingIntent, true);
        } else {
            notificationBuilder = prepareFileExportNotification(R.drawable.ic_stat_action_done, getString(R.string.export_notif_complete_title), getString(R.string.export_notif_complete_content), pendingIntent, true);
        }
        notificationBuilder.addAction(R.drawable.ic_pref_share, getString(R.string.action_share), pendingIntent);
        notificationManager.notify(FileExport.class.hashCode(), notificationBuilder.build());
    }

    private NotificationCompat.Builder prepareFileExportNotification(@DrawableRes int iconId, String titleText, String contentText, PendingIntent pendingIntent, boolean autoCancel) {
        return new NotificationCompat.Builder(this)
                .setSmallIcon(iconId)
                .setTicker(titleText)
                .setContentTitle(titleText)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setAutoCancel(autoCancel)
                .setOngoing(!autoCancel)
                .setContentIntent(pendingIntent)
                .setColor(ActivityCompat.getColor(this, R.color.netmon_color));
    }

    private void handleActionImport(Uri uri) {
        Log.d(TAG, "handleActionImport() called with " + "uri = [" + uri + "]");
        mDBOperation = new DBImport(this, uri);
        mDBOperation.execute(mImportProgressListener);
    }

    private final BroadcastReceiver mStopSelfReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive() called with " + "context = [" + context + "], intent = [" + intent + "]");
            if (ACTION_STOP_DB_OP.equals(intent.getAction())) {
                if (mDBOperation != null) mDBOperation.cancel();
            }
        }
    };
}
