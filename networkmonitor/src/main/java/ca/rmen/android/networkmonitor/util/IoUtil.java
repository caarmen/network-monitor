/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013 Benoit 'BoD' Lubek (BoD@JRAF.org)
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

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ca.rmen.android.networkmonitor.Constants;

public class IoUtil {
    private static final String TAG = Constants.TAG + IoUtil.class.getSimpleName();
    private static final int BUFFER_SIZE = 1500;

    /**
     * Silently close the given {@link Closeable}s, ignoring any {@link IOException}.<br/> {@code null} objects are ignored.
     * 
     * @param toClose The {@link Closeable}s to close.
     */
    public static void closeSilently(Closeable... toClose) {
        for (Closeable closeable : toClose) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    // Ignored
                }
            }
        }
    }

    /**
     * Copy the contents of the given {@link InputStream} into the given {@link OutputStream}.<br/>
     * Note: the given {@link InputStream} and {@link OutputStream} won't be closed.
     * 
     * @param in The {@link InputStream} to read.
     * @param out The {@link OutputStream} to write to.
     * @return the actual number of bytes that were read.
     * @throws IOException If a error occurs while reading or writing.
     */
    public static long copy(InputStream in, OutputStream out) throws IOException {
        long res = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            out.flush();
            res += read;
        }
        return res;
    }

    /**
     * Copy the contents of file in to file out.
     * @return true if the file could be copied, false if there was an error.
     */
    public static boolean copy(File in, File out) {
        Log.v(TAG, "copy " + in + " to " + out);
        FileInputStream is = null;
        FileOutputStream os = null;
        try {
            is = new FileInputStream(in);
            os = new FileOutputStream(out);
            long size = copy(is, os);
            return size > 0;
        } catch (IOException e) {
            Log.w(TAG, "Could not copy " + in + " to " + out, e);
            return false;
        } finally {
            closeSilently(is, os);
        }
    }

    public static boolean copy(Context context, Uri in, File out) {
        Log.v(TAG, "copy " + in + " to " + out);
        InputStream is = null;
        OutputStream os = null;
        try {
            is = context.getContentResolver().openInputStream(in);
            os = new FileOutputStream(out);
            long size = copy(is, os);
            return size > 0;
        } catch (IOException e) {
            Log.w(TAG, "Could not copy " + in + " to " + out, e);
            return false;
        } finally {
            closeSilently(is, os);
        }
    }

    public static boolean copy(Context context, Uri in, Uri out) {
        Log.v(TAG, "copy " + in + " to " + out);
        InputStream is = null;
        OutputStream os = null;
        try {
            is = context.getContentResolver().openInputStream(in);
            os = context.getContentResolver().openOutputStream(out);
            long size = copy(is, os);
            return size > 0;
        } catch (IOException e) {
            Log.w(TAG, "Could not copy " + in + " to " + out, e);
            return false;
        } finally {
            closeSilently(is, os);
        }
    }

}
