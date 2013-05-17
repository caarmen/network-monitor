package org.jraf.android.networkmonitor.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import org.jraf.android.networkmonitor.Config;
import org.jraf.android.networkmonitor.Constants;

public class NetMonService extends Service {
    private static final String TAG = Constants.TAG + NetMonService.class.getSimpleName();

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss", Locale.US);
    //    private static final String HOST = "www.google.com";
    private static final String HOST = "173.194.34.16";
    private static final int PORT = 80;
    private static final int TIMEOUT = 15000;
    private static final String HTTP_GET = "GET / HTTP/1.1\r\n\r\n";

    private PrintWriter mOutputStream;
    private volatile boolean mDestroyed;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        if (!isServiceEnabled()) {
            if (Config.LOGD) Log.d(TAG, "onCreate Service is disabled: stopping now");
            stopSelf();
            return;
        }
        if (Config.LOGD) Log.d(TAG, "onCreate Service is enabled: starting monitor loop");
        new Thread() {
            @Override
            public void run() {
                try {
                    initFile();
                } catch (IOException e) {
                    Log.e(TAG, "run Could not init file", e);
                    return;
                }
                monitorLoop();
            }
        }.start();
    }

    private boolean isServiceEnabled() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_SERVICE_ENABLED, Constants.PREF_SERVICE_ENABLED_DEFAULT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    protected void initFile() throws IOException {
        File file = new File(getExternalFilesDir(null), Constants.FILE_NAME_LOG);
        mOutputStream = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
    }

    protected void monitorLoop() {
        while (!mDestroyed) {
            boolean networkUp = isNetworkUp();
            if (Config.LOGD) Log.d(TAG, "mainLoop networkUp=" + networkUp);
            mOutputStream.println(DATE_FORMAT.format(new Date()) + " " + (networkUp ? "UP" : "DOWN"));
            mOutputStream.flush();

            // Sleep
            long updateInterval = getUpdateInterval();
            if (Config.LOGD) Log.d(TAG, "monitorLoop Sleeping " + updateInterval / 1000 + " seconds...");
            SystemClock.sleep(updateInterval);

            // Loop if service is still enabled, otherwise stop
            if (!isServiceEnabled()) {
                if (Config.LOGD) Log.d(TAG, "onCreate Service is disabled: stopping now");
                stopSelf();
                return;
            }
        }
    }

    private long getUpdateInterval() {
        String updateIntervalStr = PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.PREF_UPDATE_INTERVAL,
                Constants.PREF_UPDATE_INTERVAL_DEFAULT);
        long updateInterval = Long.valueOf(updateIntervalStr);
        return updateInterval;
    }

    private boolean isNetworkUp() {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.setSoTimeout(TIMEOUT);
            if (Config.LOGD) Log.d(TAG, "isNetworkUp Resolving " + HOST);
            InetSocketAddress remoteAddr = new InetSocketAddress(HOST, PORT);
            InetAddress address = remoteAddr.getAddress();
            if (address == null) {
                if (Config.LOGD) Log.d(TAG, "isNetworkUp Could not resolve");
                return false;
            }
            if (Config.LOGD) Log.d(TAG, "isNetworkUp Resolved " + address.getHostAddress());
            if (Config.LOGD) Log.d(TAG, "isNetworkUp Connecting...");
            socket.connect(remoteAddr, TIMEOUT);
            if (Config.LOGD) Log.d(TAG, "isNetworkUp Connected");

            if (Config.LOGD) Log.d(TAG, "isNetworkUp Sending GET...");
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(HTTP_GET.getBytes("utf-8"));
            outputStream.flush();
            if (Config.LOGD) Log.d(TAG, "isNetworkUp Sent GET");
            InputStream inputStream = socket.getInputStream();
            if (Config.LOGD) Log.d(TAG, "isNetworkUp Reading...");
            int read = inputStream.read();
            if (Config.LOGD) Log.d(TAG, "isNetworkUp Read read=" + read);
            return read != -1;
        } catch (Throwable t) {
            if (Config.LOGD) Log.d(TAG, "isNetworkUp Caught an exception", t);
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.w(TAG, "isNetworkUp Could not close socket", e);
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        mDestroyed = true;
        if (mOutputStream != null) mOutputStream.close();
        super.onDestroy();
    }
}
