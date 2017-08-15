/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013-2017 Carmen Alvarez (c@rmen.ca)
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
import android.content.Intent;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.ProgressListener;
import ca.rmen.android.networkmonitor.app.dbops.backend.DBOperation;
import ca.rmen.android.networkmonitor.app.dbops.ui.Share;
import android.util.Log;

/**
 * Export the Network Monitor data from the DB to a file.
 */
public abstract class FileExport implements DBOperation {
    private static final String TAG = Constants.TAG + FileExport.class.getSimpleName();


    protected final Context mContext;
    protected final File mFile;
    private final AtomicBoolean mIsCanceled = new AtomicBoolean(false);

    protected FileExport(Context context, File file) {
        Log.v(TAG, "FileExport: file " + file);
        mContext = context;
        mFile = file;
    }

    @Override
    abstract public void execute(ProgressListener listener);

    @Override
    public void cancel() {
        mIsCanceled.set(true);
    }

    public boolean isCanceled() {
        return mIsCanceled.get();
    }

    public File getFile() {
        return mFile;
    }

    /**
     * @return a chooser intent to share a report summary text, with an optional attached exported file.
     */
    public static Intent getShareIntent(Context context, File exportedFile) {
        String reportSummary = SummaryExport.getSummary(context);

        // Bring up the chooser to share the file.
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.export_subject_send_log));

        String dateRange = SummaryExport.getDataCollectionDateRange(context);

        String messageBody = context.getString(R.string.export_message_text, dateRange);
        if (exportedFile != null && exportedFile.exists()) {
            Share.addFileToShareIntent(context, sendIntent, exportedFile.getName());
            messageBody += context.getString(R.string.export_message_text_file_attached);
        } else {
            sendIntent.setType("text/plain");
        }
        messageBody += reportSummary;
        sendIntent.putExtra(Intent.EXTRA_TEXT, messageBody);
        return Intent.createChooser(sendIntent, context.getResources().getText(R.string.action_share));
    }
}
