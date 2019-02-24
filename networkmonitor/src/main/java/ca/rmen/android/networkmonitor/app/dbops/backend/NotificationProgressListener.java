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
package ca.rmen.android.networkmonitor.app.dbops.backend;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.ProgressListener;
import ca.rmen.android.networkmonitor.app.main.MainActivity;
import ca.rmen.android.networkmonitor.app.service.NetMonNotification;
import android.util.Log;

/**
 * Displays task progress in a system notification.
 */
class NotificationProgressListener implements ProgressListener {
    private static final String TAG = Constants.TAG + NotificationProgressListener.class.getSimpleName();

    private final Context mContext;
    private final NotificationManager mNotificationManager;
    private final int mNotificationId;
    private final int mNotificationIcon;
    private final int mNotificationProgressTitleId;
    private final int mNotificationProgressContentId;
    private final int mNotificationCompleteTitleId;

    private long mLastProgressUpdateTimestamp;

    NotificationProgressListener(Context context,
                                 int notificationId,
                                 @DrawableRes int notificationIcon,
                                 @StringRes int notificationProgressTitleId,
                                 @StringRes int notificationProgressContentId,
                                 @StringRes int notificationCompleteTitleId) {
        mContext = context;
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationId = notificationId;
        mNotificationIcon = notificationIcon;
        mNotificationProgressTitleId = notificationProgressTitleId;
        mNotificationProgressContentId = notificationProgressContentId;
        mNotificationCompleteTitleId = notificationCompleteTitleId;
    }

    @Override
    public void onProgress(int progress, int max) {
        long now = System.currentTimeMillis();
        if(now - mLastProgressUpdateTimestamp < 500) return;
        mLastProgressUpdateTimestamp = now;

        Notification notification = new NotificationCompat.Builder(mContext, NetMonNotification.createOngoingNotificationChannel(mContext))
                .setSmallIcon(mNotificationIcon)
                .setTicker(mContext.getString(mNotificationProgressTitleId))
                .setContentTitle(mContext.getString(mNotificationProgressTitleId))
                .setProgress(max, progress, false)
                .setOngoing(true)
                .addAction(R.drawable.ic_action_stop, mContext.getString(R.string.service_notification_action_stop),
                        PendingIntent.getBroadcast(mContext, 0, new Intent(DBOpIntentService.ACTION_STOP_DB_OP), PendingIntent.FLAG_CANCEL_CURRENT))
                .setColor(ActivityCompat.getColor(mContext, R.color.netmon_color))
                .setContentText(mContext.getString(mNotificationProgressContentId, progress, max))
                .setAutoCancel(false)
                .setContentIntent(getMainActivityPendingIntent(mContext))
                .build();

        mNotificationManager.notify(mNotificationId, notification);
    }

    @Override
    public void onComplete(String message) {
        Log.d(TAG, "onComplete() called with " + "message = [" + message + "]");
        Notification notification = new NotificationCompat.Builder(mContext, NetMonNotification.createOngoingNotificationChannel(mContext))
                .setSmallIcon(R.drawable.ic_stat_action_done)
                .setTicker(mContext.getString(mNotificationCompleteTitleId))
                .setContentTitle(mContext.getString(mNotificationCompleteTitleId))
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setColor(ActivityCompat.getColor(mContext, R.color.netmon_color))
                .setAutoCancel(true)
                .setContentIntent(getMainActivityPendingIntent(mContext))
                .build();

        mNotificationManager.notify(mNotificationId, notification);
    }


    @Override
    public void onWarning(String message) {
        Log.d(TAG, "onWarning() called with " + "message = [" + message + "]");
        Notification notification = new NotificationCompat.Builder(mContext, NetMonNotification.createOngoingNotificationChannel(mContext))
                .setSmallIcon(R.drawable.ic_stat_warning)
                .setTicker(mContext.getString(mNotificationProgressTitleId))
                .setContentTitle(mContext.getString(mNotificationProgressTitleId))
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setContentIntent(getMainActivityPendingIntent(mContext))
                .setColor(ActivityCompat.getColor(mContext, R.color.netmon_color))
                .build();

        mNotificationManager.notify(mNotificationId, notification);
    }

    @Override
    public void onError(String message) {
        Log.d(TAG, "onError() called with " + "message = [" + message + "]");
        Notification notification = new NotificationCompat.Builder(mContext, NetMonNotification.createOngoingNotificationChannel(mContext))
                .setSmallIcon(R.drawable.ic_stat_warning)
                .setTicker(mContext.getString(mNotificationProgressTitleId))
                .setContentTitle(mContext.getString(mNotificationProgressTitleId))
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setContentIntent(getMainActivityPendingIntent(mContext))
                .setColor(ActivityCompat.getColor(mContext, R.color.netmon_color))
                .build();
        mNotificationManager.notify(mNotificationId, notification);
    }

    private static PendingIntent getMainActivityPendingIntent(Context context) {
        return PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
