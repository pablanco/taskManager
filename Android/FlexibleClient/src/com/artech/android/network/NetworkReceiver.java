package com.artech.android.network;

import java.io.File;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.artech.application.MyApplication;
import com.artech.base.model.EntityList;
import com.artech.base.services.AndroidContext;
import com.artech.base.services.Services;
import com.artech.base.synchronization.SynchronizationHelper;
import com.artech.base.synchronization.SynchronizationSendAsyncTask.ProcedureExecutionDummy;
import com.artech.base.synchronization.SynchronizationSendHelper;

public class NetworkReceiver extends BroadcastReceiver {   
      
	
	@Override
	public void onReceive(Context context, Intent intent) {
		ConnectivityManager conn =  (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = conn.getActiveNetworkInfo();
       
		if (MyApplication.getApp()== null)
		{
			Services.Log.Error("NetworkReceiver onReceive, current app null"); //$NON-NLS-1$
			//Toast.makeText(context, "current app null ", Toast.LENGTH_SHORT).show();
			return;
		}
	
		//Services.Log.debug("NetworkReceiver onReceive" + MyApplication.getApp().getAppEntry());
		
		if (MyApplication.getApp().isOfflineApplication()
				&& MyApplication.getApp().getSynchronizerSendAutomatic() 
				 )
		{
			if (ProcedureExecutionDummy.isRunningSendDummyBackground)
			{
				//Services.Log.warning("NetworkReceiver onReceive, other receiving working, wait."); //$NON-NLS-1$
				return;
			}
		
			if (SynchronizationSendHelper.isRunningSendBackground)
			{
				Services.Log.warning("NetworkReceiver onReceive, other receiving working."); //$NON-NLS-1$
				return;
			}
		
			if (SynchronizationHelper.isRunningSendOrReceive)
			{
				Services.Log.warning("NetworkReceiver onReceive, Send Or Receive running, cannot do a send."); //$NON-NLS-1$
				return;
			}
		
			String filePath = AndroidContext.ApplicationContext.getDataBaseFilePath();
			File file = new File(filePath);
			if (!file.exists())
			{
				Services.Log.warning("NetworkReceiver onReceive, Database File not exits."); //$NON-NLS-1$
				return;
			}
			if (!Services.Application.isLoaded())
			{
				Services.Log.warning("NetworkReceiver onReceive, Application is not loaded yet"); //$NON-NLS-1$
				return;
				
			}
			
			// Checks the user prefs and the network connection. Based on the result, decides whether
			// 	to refresh the display or keep the current display.
			// 	If the userpref is Wi-Fi only, checks to see if the device has a Wi-Fi connection.
			if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI
					&& networkInfo.isConnected()) {
				// If device has its Wi-Fi connection, sets refreshDisplay
				// 	to true. This causes the display to be refreshed when the user
				// 	returns to the app.
			
				//Toast.makeText(context, "Wifi conected " , Toast.LENGTH_SHORT).show();
				Services.Log.debug(" Wifi conected " ); //$NON-NLS-1$
			
				//Send
				sendPendingEvents();
			
				// If the setting is ANY network and there is a network connection
				// (which by process of elimination would be mobile), sets refreshDisplay to true.
			} else if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE
					&& networkInfo.isConnected() ) {
				
				Services.Log.debug("Mobile conected " ); //$NON-NLS-1$
				
				//Send
				sendPendingEvents();
				
			} else if (networkInfo != null && networkInfo.isConnected()) {
				
				Services.Log.debug("Any conected " ); //$NON-NLS-1$
				// Otherwise, the app can't download content--either because there is no network
				// connection (mobile or Wi-Fi), or because the pref setting is WIFI, and there 
				// is no Wi-Fi connection.
				// Sets refreshDisplay to false.
				Services.Log.debug("Dont send, any conected " ); //$NON-NLS-1$
			} else {
			
				Services.Log.debug(" Network Disconect. " ); //$NON-NLS-1$
			}
		}
		
	}

	private void sendPendingEvents() {
		EntityList pendings = SynchronizationHelper.getPendingEventsList("1"); //$NON-NLS-1$ // Pending
		if (pendings.size()>0)
		{
			Services.Log.debug(" Has Pending events, send. " ); //$NON-NLS-1$
			//		Send events
			//Test get pending events

			//	List<Entity> pendings = SynchronizationHelper.getPendingEventsList();
			//pendings.toString();

			// TODO: change for the correct send event method.
			//send Pending event temp: remove, do it if online and has pending event or by property
			Services.Log.debug("sendPendingsToServerInBackground (Sync.Send) from NetworkReceiver onReceive "); //$NON-NLS-1$
			SynchronizationHelper.sendPendingsToServerInBackground();
			//SynchronizationHelper.sendPendingsToServerDummy();
		}
		else
		{
			Services.Log.debug("No Pending events, wait. " ); //$NON-NLS-1$
			
			// Only allow to not receive more connection change for some seconds if not pending events 
			SynchronizationHelper.sendPendingsToServerDummy();
		}
	}
}	

