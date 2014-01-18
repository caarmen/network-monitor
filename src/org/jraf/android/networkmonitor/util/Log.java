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
package org.jraf.android.networkmonitor.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;


/**
 * A logger that appends messages to a file on the disk.<br/> {@link #init(Context, int, boolean)} must be called prior to using the other methods of this class
 * (typically this should be done in {@link Application#onCreate()}).<br/>
 * Before using the log file (for instance to send it to server), {@link #prepareLogFile()} must be called.
 * However, this is automatically called in case of an uncaught Exception.
 */
public class Log {
    public static final String FILE = "log.txt";

    private static final String FILE_0 = "log0.txt";
    private static final String FILE_1 = "log1.txt";
    protected static final int MSG_V = 0;
    protected static final int MSG_D = 1;
    protected static final int MSG_I = 2;
    protected static final int MSG_W = 3;
    protected static final int MSG_E = 4;
    protected static final String KEY_TAG = "KEY_TAG";
    protected static final String KEY_MESSAGE = "KEY_MESSAGE";
    protected static final String KEY_DATE = "KEY_DATE";
    protected static final String KEY_THREADID = "KEY_THREADID";
    protected static final String KEY_THROWABLE = "KEY_THROWABLE";

    private static int sMaxLogSize;
    private static BufferedWriter sWriter;
    private static File sFile;
    private static File sFile0;
    private static File sFile1;
    private static File sCurrentFile;
    private static Handler sHandler;
    private static boolean sError = true;
    private static boolean sErrorLogged;
    private static boolean sAndroidLogD;

    /**
     * If you are using ACRA, this method must be called <em>after</em> calling {@code ACRA.init()}.
     * 
     * @param maxLogSize Max log size in bytes.
     * @param androidLogD If {@code true}, then {@link #d(String, String)} and {@link #v(String, String)} calls will also log
     *            to the standard Android Logcat facility.
     */
    public static void init(Context context, int maxLogSize, boolean androidLogD) {
        if (!sError) logError("Fatal error! Init must be called only once.");

        sMaxLogSize = maxLogSize;
        sAndroidLogD = androidLogD;

        sFile = new File(context.getExternalFilesDir(null), FILE);
        sFile0 = new File(context.getFilesDir(), FILE_0);
        sFile1 = new File(context.getFilesDir(), FILE_1);

        try {
            initFile();
        } catch (IOException e) {
            logError("Fatal error! Could not open log file.", e);
            return;
        }

        HandlerThread handlerThread = new HandlerThread(Log.class.getName(), android.os.Process.THREAD_PRIORITY_LOWEST);
        handlerThread.start();
        sHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (sCurrentFile.length() >= sMaxLogSize / 2) {
                    android.util.Log.d("Log", "File is " + sCurrentFile.length() + " bytes: switch");
                    // Switch files
                    sCurrentFile = sCurrentFile == sFile0 ? sFile1 : sFile0;
                    try {
                        IoUtil.closeSilently(sWriter);
                        sWriter = new BufferedWriter(new FileWriter(sCurrentFile, false));
                    } catch (IOException e) {
                        logError("Fatal error! Could not open log file.", e);
                        sError = true;
                    }
                }

                try {
                    sWriter.write(String.valueOf(System.currentTimeMillis()));
                    switch (msg.what) {
                        case MSG_V:
                            sWriter.write(" V ");
                            break;
                        case MSG_D:
                            sWriter.write(" D ");
                            break;
                        case MSG_I:
                            sWriter.write(" I ");
                            break;
                        case MSG_W:
                            sWriter.write(" W ");
                            break;
                        case MSG_E:
                            sWriter.write(" E ");
                            break;
                    }
                    Bundle data = msg.getData();
                    sWriter.write(data.getString(KEY_THREADID));
                    sWriter.write(" ");
                    sWriter.write(data.getString(KEY_TAG));
                    sWriter.write(' ');
                    sWriter.write(data.getString(KEY_MESSAGE));
                    sWriter.write('\n');
                    Throwable throwable = (Throwable) data.getSerializable(KEY_THROWABLE);
                    if (throwable != null) {
                        throwable.printStackTrace(new PrintWriter(sWriter));
                    }
                    sWriter.flush();
                } catch (IOException e) {
                    logError("Fatal error! Could not write to log file.", e);
                    sError = true;
                }
            }
        };

        // Install an exception handler
        final UncaughtExceptionHandler previousExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Log.prepareLogFile();
                previousExceptionHandler.uncaughtException(thread, ex);
            }
        });

        sError = false;
    }


    private static void initFile() throws IOException {
        if (!sFile0.exists() || !sFile1.exists()) {
            // Use file 0 by default (the first time)
            sCurrentFile = sFile0;
        } else {
            // Keep using the file we used last time (the most recently modified one)
            if (sFile0.lastModified() > sFile1.lastModified()) {
                android.util.Log.d("Log", "Using log0");
                sCurrentFile = sFile0;
            } else {
                android.util.Log.d("Log", "Using log1");
                sCurrentFile = sFile1;
            }
        }
        sWriter = new BufferedWriter(new FileWriter(sCurrentFile, true));
    }

    private static void log(int messageId, String tag, String message, Throwable throwable) {
        if (sError) {
            logError("Fatal error! You must call Log.init() prior to calling any logging methods.");
            return;
        }

        Message msg = Message.obtain(sHandler, messageId, message);
        Bundle data = msg.getData();
        data.putString(KEY_TAG, tag);
        data.putString(KEY_MESSAGE, message);
        data.putLong(KEY_DATE, System.currentTimeMillis());
        data.putString(KEY_THREADID, String.valueOf(Thread.currentThread().getId()));
        if (throwable != null) data.putSerializable(KEY_THROWABLE, throwable);
        sHandler.sendMessage(msg);
    }


    /*
     * Verbose.
     */

    public static void v(String tag, String message) {
        v(tag, message, null);
    }

    public static void v(String tag, String message, Throwable throwable) {
        if (sAndroidLogD) {
            if (throwable != null) {
                android.util.Log.v(tag, message, throwable);
            } else {
                android.util.Log.v(tag, message);
            }
        }
        log(MSG_V, tag, message, throwable);
    }


    /*
     * Debug.
     */

    public static void d(String tag, String message) {
        d(tag, message, null);
    }

    public static void d(String tag, String message, Throwable throwable) {
        if (sAndroidLogD) {
            if (throwable != null) {
                android.util.Log.d(tag, message, throwable);
            } else {
                android.util.Log.d(tag, message);
            }
        }
        log(MSG_D, tag, message, throwable);
    }


    /*
     * Info.
     */

    public static void i(String tag, String message) {
        i(tag, message, null);
    }

    public static void i(String tag, String message, Throwable throwable) {
        if (throwable != null) {
            android.util.Log.i(tag, message, throwable);
        } else {
            android.util.Log.i(tag, message);
        }
        log(MSG_I, tag, message, throwable);
    }


    /*
     * Warning.
     */

    public static void w(String tag, String message) {
        w(tag, message, null);
    }

    public static void w(String tag, String message, Throwable throwable) {
        if (throwable != null) {
            android.util.Log.w(tag, message, throwable);
        } else {
            android.util.Log.w(tag, message);
        }
        log(MSG_W, tag, message, throwable);
    }


    /*
     * Error.
     */

    public static void e(String tag, String message) {
        e(tag, message, null);
    }

    public static void e(String tag, String message, Throwable throwable) {
        if (throwable != null) {
            android.util.Log.e(tag, message, throwable);
        } else {
            android.util.Log.e(tag, message);
        }
        log(MSG_E, tag, message, throwable);
    }


    /*
     * Internal errors.
     */

    private static void logError(String message) {
        logError(message, new Exception(message));
    }

    private static void logError(String message, Throwable throwable) {
        // This will be logged only once
        if (!sErrorLogged) {
            android.util.Log.e("Log", message, throwable);
            sErrorLogged = true;
        }
    }

    /**
     * Prepares the log file by retrieving contents from the temporary files.<br/>
     * This must not be called from the UI thread since it accesses the disk.
     */
    public static void prepareLogFile() {
        android.util.Log.d("Log", "Preparing log file...");
        BufferedInputStream in0 = null;
        BufferedInputStream in1 = null;
        BufferedOutputStream out = null;
        try {
            if (sFile0.exists()) in0 = new BufferedInputStream(new FileInputStream(sFile0));
            if (sFile1.exists()) in1 = new BufferedInputStream(new FileInputStream(sFile1));
            out = new BufferedOutputStream(new FileOutputStream(sFile, false));

            if (sFile0.exists() && sFile1.exists()) {
                if (sFile0.lastModified() < sFile1.lastModified()) {
                    IoUtil.copy(in0, out);
                    IoUtil.copy(in1, out);
                } else {
                    IoUtil.copy(in1, out);
                    IoUtil.copy(in0, out);
                }
            } else if (sFile0.exists()) {
                IoUtil.copy(in0, out);
            } else if (sFile1.exists()) {
                IoUtil.copy(in1, out);
            }
            out.flush();
        } catch (IOException e) {
            android.util.Log.e("Log", "Could not prepare log file.", e);
        } finally {
            IoUtil.closeSilently(in0, in1, out);
        }
        android.util.Log.d("Log", "Done.");
    }
}
