package com.artech.android.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

import com.artech.R;
import com.artech.application.MyApplication;
import com.artech.base.services.Services;
import com.artech.base.utils.PrimitiveUtils;

public class NotificationHelper
{
	public static int NotificationId = 51;
	private static NotificationManager sNotificationManager;

	private static NotificationManager getNotificationManager()
	{
		if (sNotificationManager == null)
			sNotificationManager = (NotificationManager)MyApplication.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);

		return sNotificationManager;
	}

	public static NotificationCompat.Builder createOngoingNotification( String title, String content, int drawableId)
	{
		NotificationCompat.Builder builder = new NotificationCompat.Builder(MyApplication.getAppContext());
		builder.setOngoing(true).setAutoCancel(true);

		// always sent to main app for now.
		//TODO : Should call some action that show the errors of the syncronizer.
		Intent intent = new Intent("android.intent.action.MAIN"); //$NON-NLS-1$
		intent.setClassName(MyApplication.getAppContext(), MyApplication.getAppContext().getPackageName() + ".Main"); //$NON-NLS-1$
	    intent.addCategory("android.intent.category.LAUNCHER"); //$NON-NLS-1$

	   	intent.setFlags(0);
	   	intent.setAction("android.intent.action.MAIN");

		PendingIntent pendingIntent = PendingIntent.getActivity(MyApplication.getAppContext(), 0,
				intent, /*PendingIntent.FLAG_CANCEL_CURRENT*/ PendingIntent.FLAG_UPDATE_CURRENT);

		builder.setContentIntent(pendingIntent);

		updateOngoingNotification(builder, title, content, drawableId);
		return builder;
	}

	public static void updateOngoingNotification(NotificationCompat.Builder builder, String title, String content, int drawableId)
	{
		builder.setContentTitle(title);
	    builder.setContentText(content);
	    builder.setSmallIcon(drawableId);

		Notification notification = builder.build();

		getNotificationManager().notify(NotificationId, notification);
	}

	public static void closeOngoingNotification(Builder builder)
	{
		getNotificationManager().cancel(NotificationId);
	}

	/**
	 * Creates a new {@link android.support.v4.app.NotificationCompat.Builder NotificationCompat.Builder} with
	 * some preset properties, such as the Lollipop color and notification icon (uses the "Android Notification Icon"
	 * as defined in the main object if available, otherwise the gx_notification_default small icon).
	 */
	public static NotificationCompat.Builder newBuilder(Context context)
	{
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

		builder.setSmallIcon(R.drawable.gx_notification_default);
		Integer customResourceId = Services.Resources.getImageResourceId("gx_notification_icon");
		if (PrimitiveUtils.isNonZero(customResourceId))
			builder.setSmallIcon(customResourceId);

		int colorResId = context.getResources().getIdentifier("gx_colorPrimary", "color", context.getPackageName());
		if (colorResId != 0)
		{
			int color = context.getResources().getColor(colorResId);
			builder.setColor(color);
			// Eventually we could also do something like: builder.setLights(color, onMs, offMs);
		}

		return builder;
	}
}
