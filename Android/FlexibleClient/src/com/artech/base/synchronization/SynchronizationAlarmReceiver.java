package com.artech.base.synchronization;

import java.io.File;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.artech.application.MyApplication;
import com.artech.base.application.IProcedure;
import com.artech.base.application.OutputResult;
import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.model.PropertiesObject;
import com.artech.base.services.AndroidContext;
import com.artech.base.services.Services;

public class SynchronizationAlarmReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent) 
	{
		Services.Log.debug("SynchronizationAlarmReceiver alarm onReceive");
		
		if (MyApplication.getApp()== null)
		{
			Services.Log.Error("SynchronizationAlarmReceiver onReceive, current app null"); //$NON-NLS-1$
			//Toast.makeText(context, "current app null ", Toast.LENGTH_SHORT).show();
			return;
		}
	
		Services.Log.debug("SynchronizationAlarmReceiver onReceive " + MyApplication.getApp().getAppEntry());
		
		// Do a receive automatically after alarm 
		if (MyApplication.getApp().isOfflineApplication()
				&& MyApplication.getApp().getSynchronizerReceiveAfterElapsedTime() )
		{
			if (SynchronizationSendHelper.isRunningSendBackground)
			{
				Services.Log.warning("SynchronizationAlarmReceiver onReceive, other sending working, cannot do a receive."); //$NON-NLS-1$
				return;
			}
		
			if (SynchronizationHelper.isRunningSendOrReceive)
			{
				Services.Log.warning("SynchronizationAlarmReceiver onReceive, Send Or Receive running, cannot do a receive."); //$NON-NLS-1$
				return;
			}
		
			String filePath = AndroidContext.ApplicationContext.getDataBaseFilePath();
			File file = new File(filePath);
			if (!file.exists())
			{
				Services.Log.warning("SynchronizationAlarmReceiver onReceive, Database File not exits."); //$NON-NLS-1$
				return;
			}
			if (!Services.Application.isLoaded())
			{
				Services.Log.warning("SynchronizationAlarmReceiver onReceive, Application is not loaded yet"); //$NON-NLS-1$
				return;
				
			}
				
			CallSyncInBackground();
				
		}
	}
	
	private void CallSyncInBackground()
	{
		Thread thread = new Thread(null, doBackgroundSyncProcessing,"BackgroundSync"); //$NON-NLS-1$
		thread.start();
	}

	private final Runnable doBackgroundSyncProcessing = new Runnable(){
		@Override
		public void run(){
	
			// Do a sync receive or call custom proc.
			if (Services.Strings.hasValue(MyApplication.getApp().getSynchronizerReceiveCustomProcedure()))
			{
				IProcedure procedure = MyApplication.getApplicationServer(Connectivity.Offline).getProcedure(MyApplication.getApp().getSynchronizerReceiveCustomProcedure());
				
				PropertiesObject parameter = new PropertiesObject();
				
				OutputResult procResult = procedure.execute(parameter);
				
				if (procResult.isOk())
				{
					Services.Log.debug("SynchronizationAlarmReceiver call custom proc successfully ");
				}
				else
				{
					Services.Log.warning("SynchronizationAlarmReceiver call custom proc failed ");
						
				}
					
			}
			else
			{
				Services.Log.debug("callSynchronizer (Sync.Receive) from SynchronizationAlarmReceiver onReceive "); //$NON-NLS-1$
				//boolean failed = SynchronizationHelper.callSynchronizer( false)!=SynchronizationHelper.SYNC_OK;
				SynchronizationHelper.callSynchronizer( false, true, false);
			}
		}
	};
	
	
	public void SetAlarm(Context context)
    {
		long minTimeBetweenSync = MyApplication.getApp().getSynchronizerMinTimeBetweenSync();
		// minTimeBetweenSync in seconds
		
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SynchronizationAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        //After after 30 seconds, every 1 minutes
        //am.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()+ (1000 * 30), (1000 * 60) , pi);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()+ (1000 * minTimeBetweenSync), (1000 * minTimeBetweenSync) , pi);
    }

    public void CancelAlarm(Context context)
    {
        Intent intent = new Intent(context, SynchronizationAlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
    
    

}
