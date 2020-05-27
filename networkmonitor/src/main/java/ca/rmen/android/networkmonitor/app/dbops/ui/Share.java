/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2015-2020 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.dbops.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;
import ca.rmen.android.networkmonitor.BuildConfig;
import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.backend.DBOpIntentService;

/**
 *
 */
public class Share {
    private static final String TAG = Constants.TAG + Share.class.getSimpleName();
    private static final String EXPORT_FOLDER_PATH = "export";

    @Nullable
    public static File getExportFolder(Context context) {
        File exportFolder = new File(context.getFilesDir(), EXPORT_FOLDER_PATH);
        if (!exportFolder.exists() && !exportFolder.mkdirs()) {
            Log.v(TAG, "Couldn't find or create export folder " + exportFolder);
            return null;
        }
        return exportFolder;
    }

    private static Uri getUriForFilename(Context context, String filename) {
        File exportFolder = new File(context.getFilesDir(), EXPORT_FOLDER_PATH);
        return getUriForFile(context, new File(exportFolder, filename));
    }

    private static Uri getUriForFile(Context context, File file) {
        return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", file);
    }

    public static void addFileToShareIntent(Context context, Intent intent, String filename) {
        Uri uri = getUriForFilename(context, filename);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setType("message/rfc822");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Log.v(TAG, "grant permission to " + packageName);
            }
        }
    }

    @WorkerThread
    public static String readDisplayName(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        }
        return uri.getLastPathSegment();
    }

    /**
     * @return File in the share folder that we can write to before sharing.
     */
    @Nullable
    public static File getExportFile(Context context, String filename) {
        File exportFolder = getExportFolder(context);
        if (exportFolder == null) return null;
        return new File(exportFolder, filename);
    }

    /**
     * Export the log file in the given format, and display the list of apps to share the file.
     *
     * @param activity The progress of the file export will be displayed in a progress dialog on this activity.
     * @param selectedShareFormat the label of the file format selected by the user.
     */
    public static void share(FragmentActivity activity, String selectedShareFormat) {
        Log.v(TAG, "share " + selectedShareFormat);
        DBOpIntentService.ExportFormat exportFormat;
        if (activity.getString(R.string.export_choice_csv).equals(selectedShareFormat)) {
            exportFormat = DBOpIntentService.ExportFormat.CSV;
        } else if (activity.getString(R.string.export_choice_html).equals(selectedShareFormat)) {
            exportFormat = DBOpIntentService.ExportFormat.HTML;
        } else if (activity.getString(R.string.export_choice_gnuplot).equals(selectedShareFormat)) {
            // The gnuplot export requires a second activity before we can share, so we return here.
            shareGnuplot(activity);
            return;
        } else if (activity.getString(R.string.export_choice_excel).equals(selectedShareFormat)) {
            exportFormat = DBOpIntentService.ExportFormat.EXCEL;
        } else if (activity.getString(R.string.export_choice_db).equals(selectedShareFormat)) {
            exportFormat = DBOpIntentService.ExportFormat.DB;
        } else {
            exportFormat = DBOpIntentService.ExportFormat.SUMMARY;
        }
        DBOpIntentService.startActionExport(activity, exportFormat);
    }

    private static void shareGnuplot(final FragmentActivity activity) {
        Log.v(TAG, "shareGnuplot");
        Intent intent = new Intent(activity, GnuplotSettingsActivity.class);
        activity.startActivity(intent);
    }


}
