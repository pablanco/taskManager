package com.artech.base.synchronization;

import android.os.AsyncTask;
import android.os.SystemClock;

import com.artech.base.services.Services;

public class SynchronizationSendAsyncTask extends AsyncTask<Void, Integer, Boolean>
{
	@Override
	protected Boolean doInBackground(Void... params)
	{
		if (SynchronizationSendHelper.isRunningSendBackground)
			return true;
		
		SynchronizationSendHelper.isRunningSendBackground = true;
		
		Services.Log.debug("callOfflineReplicator (Sync.Send) from background "); //$NON-NLS-1$
		SynchronizationSendHelper.callOfflineReplicator();
		
		SynchronizationSendHelper.isRunningSendBackground = false;
    	return true;
	}
	
	/* Temporary, for testing only */
	public static class ProcedureExecutionDummy extends AsyncTask<Void, Integer, Boolean>
	{
		public static boolean isRunningSendDummyBackground = false;
		
		@Override
		protected Boolean doInBackground(Void... params) {
			if (isRunningSendDummyBackground)
				return true;
			
			isRunningSendDummyBackground = true;
			
			//Services.Log.debug("before sleep");
			SystemClock.sleep(10000);
			isRunningSendDummyBackground = false;
			//Services.Log.debug("after sleep");
			
			return true;
		}
	}
			
			
}
