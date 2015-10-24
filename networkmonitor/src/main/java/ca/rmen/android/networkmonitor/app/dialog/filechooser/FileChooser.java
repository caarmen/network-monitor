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
package ca.rmen.android.networkmonitor.app.dialog.filechooser;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;

import ca.rmen.android.networkmonitor.R;

class FileChooser {
    private FileChooser() {
        // utility class
    }

    static String getShortDisplayName(Context context, File file) {
        if (file.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath()))
            return context.getString(R.string.file_chooser_sdcard);
        else if (TextUtils.isEmpty(file.getName()))
            return context.getString(R.string.file_chooser_root);
        else
            return file.getName();
    }

    static String getFullDisplayName(Context context, File file) {
        if (file.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath()))
            return context.getString(R.string.file_chooser_sdcard);
        else if (TextUtils.isEmpty(file.getName()))
            return context.getString(R.string.file_chooser_root);
        else
            return file.getAbsolutePath();
    }

}
