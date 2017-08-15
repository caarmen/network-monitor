/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2015 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.util;

import java.io.File;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import ca.rmen.android.networkmonitor.Constants;

public class FileUtil {

    private static final String TAG = Constants.TAG + FileUtil.class.getSimpleName();

    /**
     * @return a file (existing or not) in external cache directory (if mounted) or the internal cache directory.
     */
    public static File getCacheFile(Context context, @SuppressWarnings("SameParameterValue") String file) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File cacheDir = context.getExternalCacheDir();
            if (cacheDir != null && (cacheDir.mkdirs() || cacheDir.isDirectory())) {
                return new File(cacheDir, file);
            }
        }
        File cacheDir = context.getCacheDir();
        return new File(cacheDir, file);
    }

    /**
     * Delete the files in the internal and external cache directories
     */
    public static void clearCache(Context context) {
        Log.v(TAG, "clearCache");
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File cacheDir = context.getExternalCacheDir();
            if (cacheDir != null && cacheDir.isDirectory()) deleteDirContents(cacheDir);
        }
        File cacheDir = context.getCacheDir();
        deleteDirContents(cacheDir);
    }

    /**
     * Non-recursively delete the files in a directory.
     */
    private static void deleteDirContents(File directory) {
        File[] files = directory.listFiles();
        for (File file : files)
            if (file.isFile()) //noinspection ResultOfMethodCallIgnored
                file.delete();
    }

}
