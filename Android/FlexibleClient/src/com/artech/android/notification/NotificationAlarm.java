package com.artech.android.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.artech.R;
import com.artech.activities.dashboard.DashboardActivity;

public class NotificationAlarm extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		//Configure the Intent
		Bundle bundle=intent.getExtras();

		int id = bundle.getInt("ID");
		CharSequence title = context.getText(R.string.app_name);
		CharSequence body = bundle.getString("NOTIFICATION");

		long hour = System.currentTimeMillis();

		Intent notIntent = new Intent(context,DashboardActivity.class);
		PendingIntent contIntent = PendingIntent.getActivity(context, 0, notIntent, 0);

		Notification notif = NotificationHelper.newBuilder(context)
			.setWhen(hour)
			.setContentTitle(title)
			.setContentText(body)
			.setContentIntent(contIntent)
			.setAutoCancel(true)
			.build();

		nm.notify(id, notif);

		LocalNotificationsSQLiteHelper db  = new LocalNotificationsSQLiteHelper(context);
		db.deleteNotification(id);
	 }
}