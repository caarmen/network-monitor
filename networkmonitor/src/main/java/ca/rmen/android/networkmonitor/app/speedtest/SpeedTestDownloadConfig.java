/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2019 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.speedtest;

import android.text.TextUtils;

import java.io.File;

import androidx.annotation.NonNull;

public class SpeedTestDownloadConfig {
    final String url;
    final File file;

    /**
     * @param url the url of the file to download
     * @param file where we will save the downloaded file.
     */
    public SpeedTestDownloadConfig(String url, File file) {
        this.url = url;
        this.file = file;
    }

    /**
     * @return true if we have enough info to attempt to download a file.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isValid() {
        return !TextUtils.isEmpty(url);
    }

    @Override
    @NonNull
    public String toString() {
        return SpeedTestDownloadConfig.class.getSimpleName() + "[url=" + url + "]";
    }
}