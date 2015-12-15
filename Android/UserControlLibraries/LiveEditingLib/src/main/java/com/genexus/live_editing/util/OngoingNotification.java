package com.genexus.live_editing.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.artech.application.MyApplication;
import com.artech.base.services.Services;
import com.genexus.live_editing.R;
import com.genexus.live_editing.ui.activities.SettingsActivity;

public class OngoingNotification {
    private static final int LIVE_EDITING_NOTIFICATION_ID = 53723;

    public static void show() {
        Context context = MyApplication.getAppContext();
        String appUrl = MyApplication.getApp().getAPIUri();
        String defaultLiveEditingUrl = Services.Application.getPatternSettings().getIDEConnectionString();
        Intent intent = new Intent(context, SettingsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(SettingsActivity.EXTRA_APP_URL, appUrl)
                .putExtra(SettingsActivity.EXTRA_DEFAULT_LIVE_EDITING_URL, defaultLiveEditingUrl);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                SettingsActivity.REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification =
                new NotificationCompat.Builder(context)
                        .setContentIntent(pendingIntent)
                        .setSmallIcon(android.R.drawable.ic_menu_edit)
                        .setContentTitle(context.getString(R.string.settings_activity_title))
                        .setContentText(context.getString(R.string.notification_text))
                        .setOngoing(true)
                        .setLocalOnly(true)
                        .setOnlyAlertOnce(true)
                        .build();
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(LIVE_EDITING_NOTIFICATION_ID, notification);
    }

    public static void dismiss() {
        Context context = MyApplication.getAppContext();
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(LIVE_EDITING_NOTIFICATION_ID);
    }
}
