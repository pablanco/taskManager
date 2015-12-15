package com.artech.android.notification;
 
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.artech.android.api.LNotificationsAPI;
import com.artech.base.services.Services;
 
public class LocalNotificationsBootReceiver extends BroadcastReceiver {
	
 	@Override
	public void onReceive(Context context, Intent intent) 
 	{
		Services.Log.debug("LocalNotificationsBootReceiver");
 		LNotificationsAPI.reSetAlertsInAlarmManagerStatic();
	}
 
}