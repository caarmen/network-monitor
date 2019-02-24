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

public class SpeedTestUploadConfig {
    final String server;
    final int port;
    final String user;
    final String password;
    final String path;
    final File file;

    /**
     * @param server the hostname or IP address of the FTP server
     * @param port the port of the FTP server
     * @param user the username
     * @param password the password
     * @param path the path on the FTP server where we will put the file
     * @param file the file we will upload
     * 
     */
    public SpeedTestUploadConfig(String server, int port, String user, String password, String path, File file) {
        this.server = server;
        this.port = port;
        this.user = user;
        this.password = password;
        this.path = path;
        this.file = file;
    }

    /**
     * @return true if we have enough info to attempt to upload a file.
     */
    public boolean isValid() {
        return !TextUtils.isEmpty(server) && port > 0 && !TextUtils.isEmpty(user) && !TextUtils.isEmpty(password);
    }

    @Override
    @NonNull
    public String toString() {
        return SpeedTestUploadConfig.class.getSimpleName() + " [server=" + server + ", port=" + port + ", user=" + user + ", path=" + path + ", file=" + file
                + "]";
    }

}