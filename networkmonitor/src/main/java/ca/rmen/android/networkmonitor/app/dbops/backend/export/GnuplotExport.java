/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2015-2017 Carmen Alvarez (c@rmen.ca)
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
import android.net.Uri;
import android.text.TextUtils;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.ProgressListener;
import ca.rmen.android.networkmonitor.app.dbops.ui.Share;
import ca.rmen.android.networkmonitor.app.prefs.FilterPreferences;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.provider.UniqueValuesColumns;
import ca.rmen.android.networkmonitor.util.IoUtil;
import android.util.Log;

/**
 * Export the Network Monitor data to a gnuplot file.
 */
public class GnuplotExport extends FileExport {
    private static final String TAG = Constants.TAG + GnuplotExport.class.getSimpleName();

    private static final String GNUPLOT_FILE = "networkmonitor.gnuplot";

    private final String mSeriesField;
    private final String mYAxisField;
    private final FilterPreferences.Selection mSelection;
    private PrintWriter mPrintWriter;
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    public GnuplotExport(Context context) {
        super(context, Share.getExportFile(context, GNUPLOT_FILE));
        NetMonPreferences prefs = NetMonPreferences.getInstance(mContext);
        mSeriesField = prefs.getExportGnuplotSeriesField();
        mYAxisField = prefs.getExportGnuplotYAxisField();

        // Get a WHERE clause based on non-null values of the series and y-axis fields, as well
        // as whatever fields the user chose to filter on, in the FilterColumnActivity.
        FilterPreferences.Selection filterSelection = FilterPreferences.getSelectionClause(mContext);
        String seriesAndYAxisSelection = String.format("%s NOT NULL AND %s NOT NULL", mSeriesField, mYAxisField);
        if (TextUtils.isEmpty(filterSelection.selectionString)) {
            mSelection = new FilterPreferences.Selection(seriesAndYAxisSelection, null);
        } else {
            mSelection = new FilterPreferences.Selection(seriesAndYAxisSelection + " AND " + filterSelection.selectionString, filterSelection.selectionArgs);
        }
    }

    @Override
    public void execute(ProgressListener listener) {
        try {
            mPrintWriter = new PrintWriter(mFile, "utf-8");

            String[] projection = new String[]{NetMonColumns.TIMESTAMP, mSeriesField, mYAxisField};
            String orderBy = String.format("%s ASC, %s ASC", mSeriesField, NetMonColumns.TIMESTAMP);

            Cursor c = mContext.getContentResolver().query(NetMonColumns.CONTENT_URI, projection, mSelection.selectionString, mSelection.selectionArgs, orderBy);
            if (c != null) {
                if (c.getCount() < 1) {
                    // We have no data to export. Give up.
                    if (listener != null) listener.onError(mContext.getString(R.string.export_notif_error_content));
                } else {
                    try {
                        // Write the stuff to the gnuplot script that comes before the actual data
                        // (graph title, axis titles, styles, etc).
                        printGraphConfig();
                        printSeriesDefinitions();

                        // Write the actual data.
                        int timestampIndex = c.getColumnIndex(NetMonColumns.TIMESTAMP);
                        int seriesIndex = c.getColumnIndex(mSeriesField);
                        int yAxisIndex = c.getColumnIndex(mYAxisField);

                        String currentSeriesValue = null;
                        while (c.moveToNext() && !isCanceled()) {
                            String seriesValue = c.getString(seriesIndex);
                            double yAxisValue = c.getDouble(yAxisIndex);
                            Date timestamp = new Date(c.getLong(timestampIndex));
                            mPrintWriter.println(String.format("%s|%s",
                                    mDateFormat.format(timestamp),
                                    yAxisValue));

                            // A line with a single "e" in gnuplot signals the end of the data
                            // for the current series.
                            if (c.isLast() || (currentSeriesValue != null && !seriesValue.equals(currentSeriesValue))) {
                                mPrintWriter.println("e");
                            }
                            if (listener != null) {
                                listener.onProgress(c.getPosition(), c.getCount());
                            }
                            currentSeriesValue = seriesValue;
                        }

                    } finally {
                        c.close();
                    }
                    if (listener != null) {
                        if (isCanceled()) {
                            listener.onComplete(mContext.getString(R.string.export_notif_canceled_content));
                        } else {
                            listener.onComplete(mContext.getString(R.string.export_save_to_external_storage_success, mFile.getAbsolutePath()));
                        }
                    }
                }
            }
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            Log.v(TAG, "error exporting gnuplot file: " + e.getMessage(), e);
            if (listener != null) listener.onError(mContext.getString(R.string.export_notif_error_content));
        }

        IoUtil.closeSilently(mPrintWriter);
    }

    /**
     * Write the first part of the gnuplot script (ex: configuring the chart style)
     */
    private void printGraphConfig() {
        // Get the first and last dates for the xrange
        String[] projection = new String[]{
                String.format("MIN(%s)", NetMonColumns.TIMESTAMP),
                String.format("MAX(%s)", NetMonColumns.TIMESTAMP),
        };

        Cursor c = mContext.getContentResolver().query(NetMonColumns.CONTENT_URI, projection, mSelection.selectionString, mSelection.selectionArgs,
                NetMonColumns.TIMESTAMP + " ASC");
        if (c != null) {
            try {
                if (c.moveToNext()) {
                    long beginDate = c.getLong(0);
                    long endDate = c.getLong(1);
                    mPrintWriter.println(
                            mContext.getString(R.string.gnuplot_script,
                                    NetMonColumns.getColumnLabel(mContext, mYAxisField),
                                    NetMonColumns.getColumnLabel(mContext, mSeriesField),
                                    mDateFormat.format(new Date(beginDate)),
                                    mDateFormat.format(new Date(endDate))
                            ));
                }

            } finally {
                c.close();
            }
        }
    }

    /**
     * Write a line to the gnuplot script for each series (includes the series name, and style
     * of the series).
     */
    private void printSeriesDefinitions() {
        String[] projection = new String[]{UniqueValuesColumns.VALUE};
        Uri uri = Uri.withAppendedPath(UniqueValuesColumns.CONTENT_URI, mSeriesField);
        Cursor c = mContext.getContentResolver().query(uri, projection, mSelection.selectionString, mSelection.selectionArgs, mSeriesField + " ASC");
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    String seriesName = c.getString(0);
                    seriesName = seriesName.replaceAll("_", "\\\\_");
                    seriesName = seriesName.replaceAll("&", "\\\\&");
                    if (TextUtils.isEmpty(seriesName)) seriesName = "?";
                    mPrintWriter.print(mContext.getString(R.string.gnuplot_series, seriesName));
                    if (c.isLast()) {
                        mPrintWriter.println();
                    } else {
                        mPrintWriter.println(", \\");
                    }
                }
            } finally {
                c.close();
            }
        }
    }

}
