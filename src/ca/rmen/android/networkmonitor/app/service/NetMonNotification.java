/*
 * context source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014 Benoit 'BoD' Lubek (BoD@JRAF.org)
 * Copyright (C) 2014 Carmen Alvarez (c@rmen.ca)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use context file except in compliance with the License.
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
package ca.rmen.android.networkmonitor.app.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.NotificationCompat;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.email.EmailPreferencesActivity;
import ca.rmen.android.networkmonitor.app.log.LogActivity;
import ca.rmen.android.networkmonitor.app.main.MainActivity;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.util.Log;

public class NetMonNotification {
    private static final String TAG = Constants.TAG + NetMonNotification.class.getSimpleName();
    private static final String PREFIX = NetMonService.class.getName() + ".";
    private static final String ACTION_DISABLE = PREFIX + "ACTION_DISABLE";
    private static final int NOTIFICATION_ID_FAILED_EMAIL = 456;
    private static final int NOTIFICATION_ID_FAILED_TEST = 567;
    static final int ONGOING_NOTIFICATION_ID = 1;

    /**
     * A notification which has the following functionalities:
     * 1) Tapping on the notification opens the app in the main activity
     * 2) Tapping on the stop button of the notification stops the service
     * 3) Tapping on the logs button of the notification opens the log activity
     */
    static Notification createOngoingNotification(Context context) {
        Log.v(TAG, "createNotification");
        context.registerReceiver(sDisableBroadcastReceiver, new IntentFilter(ACTION_DISABLE));
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setOngoing(true);
        builder.setSmallIcon(R.drawable.ic_stat_service_running);
        builder.setTicker(context.getString(R.string.service_notification_ticker));
        builder.setContentTitle(context.getString(R.string.app_name));
        builder.setContentText(context.getString(R.string.service_notification_text));
        builder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
        builder.addAction(R.drawable.ic_action_stop, context.getString(R.string.service_notification_action_stop),
                PendingIntent.getBroadcast(context, 0, new Intent(ACTION_DISABLE), PendingIntent.FLAG_CANCEL_CURRENT));
        builder.addAction(R.drawable.ic_action_logs, context.getString(R.string.service_notification_action_logs),
                PendingIntent.getActivity(context, 0, new Intent(context, LogActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
        Notification notification = builder.build();
        return notification;
    }

    static void dismissOngoingNotification(Context context) {
        Log.v(TAG, "dismissNotification");
        context.unregisterReceiver(sDisableBroadcastReceiver);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(ONGOING_NOTIFICATION_ID);
    }

    public static void showEmailFailureNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_stat_warning);
        builder.setAutoCancel(true);
        builder.setTicker(context.getString(R.string.warning_notification_ticker_email_failed));
        builder.setContentTitle(context.getString(R.string.app_name));
        builder.setContentText(context.getString(R.string.warning_notification_message_email_failed));
        builder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, EmailPreferencesActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
        Notification notification = builder.build();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID_FAILED_EMAIL, notification);
    }

    public static void dismissEmailFailureNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID_FAILED_EMAIL);
    }

    public static void showFailedTestNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_stat_warning);
        builder.setAutoCancel(true);
        builder.setTicker(context.getString(R.string.warning_notification_ticker_test_failed));
        builder.setContentTitle(context.getString(R.string.app_name));
        builder.setContentText(context.getString(R.string.warning_notification_message_test_failed));
        builder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, LogActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
        Notification notification = builder.build();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID_FAILED_TEST, notification);
    }

    public static void dismissFailedTestNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID_FAILED_TEST);
    }

    /**
     * This class receives the ACTION_DISABLE broadcast (sent when the user taps on the stop button in the notification), and updates the shared preference to
     * disable the service
     */
    private static final BroadcastReceiver sDisableBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "onReceive: " + intent);
            if (ACTION_DISABLE.equals(intent.getAction())) NetMonPreferences.getInstance(context).setServiceEnabled(false);
        }
    };

}
