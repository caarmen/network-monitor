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
package ca.rmen.android.networkmonitor.app.db.export;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.Context;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.app.useractions.UserActionAsyncTask.ProgressListener;
import ca.rmen.android.networkmonitor.app.useractions.UserActionAsyncTask.Task;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Export the Network Monitor data from the DB to a file.
 */
public abstract class FileExport implements Task<File> {
    private static final String TAG = Constants.TAG + FileExport.class.getSimpleName();


    protected final Context mContext;
    protected final File mFile;

    protected FileExport(Context context, File file) throws FileNotFoundException {
        Log.v(TAG, "FileExport: file " + file);
        mContext = context;
        mFile = file;
    }

    /**
     * @return the file if it was correctly exported, null otherwise.
     */
    @Override
    abstract public File execute(ProgressListener listener);
}
