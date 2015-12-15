/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.artech.android.gcm;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.artech.R;
import com.artech.actions.Action;
import com.artech.actions.ActionExecution;
import com.artech.actions.ActionFactory;
import com.artech.actions.ActionParameters;
import com.artech.actions.UIContext;
import com.artech.activities.IntentParameters;
import com.artech.android.notification.NotificationHelper;
import com.artech.application.MyApplication;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.DashboardItem;
import com.artech.base.metadata.DashboardMetadata;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.metadata.loader.LoadResult;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.compatibility.CompatibilityHelper;
import com.google.android.gms.gcm.GoogleCloudMessaging;


public class GcmIntentService extends IntentService
{
	  public GcmIntentService()
	  {
	        super("GcmIntentService");
	  }

	  public static final String TAG = "GCM Client Service";

	  @Override
	    protected void onHandleIntent(Intent intent) {
	        Bundle extras = intent.getExtras();
	        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
	        // The getMessageType() intent parameter must be the intent you received
	        // in your BroadcastReceiver.
	        String messageType = gcm.getMessageType(intent);

	        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
	            /*
	             * Filter messages based on message type. Since it is likely that GCM will be
	             * extended in the future with new message types, just ignore any message types you're
	             * not interested in, or that you don't recognize.
	             */
	            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
	                //sendNotification("Send error: " + extras.toString());
	            	Services.Log.Error(TAG, "Send error: " + extras.toString());
	            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
	                //sendNotification("Deleted messages on server: " + extras.toString());
	                Services.Log.Error(TAG, "Deleted messages on server: " + extras.toString());
	            // If it's a regular GCM message, do some work.
	            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
	            	Log.i(TAG, "Received: " + extras.toString());
	            	// Process the real message
	            	onMessageReceive(intent);
	            }
	        }
	        // Release the wake lock provided by the WakefulBroadcastReceiver.
	        GcmBroadcastReceiver.completeWakefulIntent(intent);
	    }

    // Actually process the GCM message here
    public void onMessageReceive(Intent intent) {
    	Log.d("onMessage", intent.toString()); //$NON-NLS-1$
    	Log.w("GCM", "Message Receiver called"); //$NON-NLS-1$ //$NON-NLS-2$

    	Bundle extras = intent.getExtras();
        if (extras != null) 
        {
        	// read message data
        	String payload = null; 
        	String action = null; 
        	String parameters = null; 
        	String executionTime = null; 
        	
        	String from = null; 
        	String cmd = null;
    		        	
        	final String jsonData = extras.getString("data"); //$NON-NLS-1$
        	boolean readFromData = false;
        	if (jsonData!=null)
        	{
        		//Special read from Parse data
        		JSONObject jsonObject;
        		try 
        		{
    				jsonObject = new JSONObject(jsonData);		
    			    				
    				payload = getJsonString(jsonObject, "alert"); //$NON-NLS-1$
                	action = getJsonString(jsonObject, "action"); //$NON-NLS-1$
                	parameters = getJsonString(jsonObject, "parameters"); //$NON-NLS-1$
                	executionTime = getJsonString(jsonObject, "executiontime"); //$NON-NLS-1$
        	
                	from = getJsonString(jsonObject, "from"); //$NON-NLS-1$
                	cmd = getJsonString(jsonObject, "CMD"); //$NON-NLS-1$
                	
                	readFromData = true;
    			} 
        		catch (JSONException e) 
        		{
    				e.printStackTrace();
    			}
        	}
        	
        	//Standard read from intent.
        	if (jsonData==null || !readFromData)
        	{
        		payload = extras.getString("payload"); //$NON-NLS-1$
        		action = extras.getString("action"); //$NON-NLS-1$
        		parameters = extras.getString("parameters"); //$NON-NLS-1$
        		executionTime = extras.getString("executiontime"); //$NON-NLS-1$

        		from = extras.getString("from"); //$NON-NLS-1$
        		cmd = extras.getString("CMD"); //$NON-NLS-1$
        	}

			//show some log about payload.
        	Log.d("GCM", "dmControl: payload = " + payload + " action = " + action); //$NON-NLS-1$ //$NON-NLS-2$

        	// Filter gcm new api message, filter using from? :
        	// http://stackoverflow.com/questions/30479424/weird-push-message-received-on-app-start
        	if (Services.Strings.hasValue(from) && Services.Strings.hasValue(cmd) && !Services.Strings.hasValue(action) && !Services.Strings.hasValue(payload)
        			&& !Services.Strings.hasValue(parameters) )
        	{
        		Log.d("GCM", "ignore GCM messsage from: = " + from); //$NON-NLS-1$ //$NON-NLS-2$
        		return;
        	}
        		
        	// Silent if not message and has action, only run this with the new notification API
        	if (Strings.hasValue(action) && Strings.hasValue(executionTime) && !Strings.hasValue(payload) && executionTime.equalsIgnoreCase("1"))  // 1= OnNotificationArrive
        		callSilentAction(action, parameters);
        	else
        		createNotification(payload, action, parameters);
	    }
    }

	private void callSilentAction(String notificationAction, String notificationParameters )
	{
		//Warning entity without definition.
		Entity entityParam = new Entity(StructureDefinition.EMPTY);

		//If app data is not loaded, load it.
		if (!Services.Application.isLoaded())
		{
			Services.Log.warning("Reload app metadata from silent notification"); //$NON-NLS-1$
			LoadResult loadResult;
			try
			{
				// Load the Application.
				loadResult = Services.Application.initialize();
			}
			catch (Exception ex)
			{
				// Uncaught exception, possibly "out of memory".
				loadResult = LoadResult.error(ex);
			}
			if (loadResult.getCode() != LoadResult.RESULT_OK)
			{
				Services.Log.Error("Metadata could not be load. Silent Notification Failed", "Message: " + loadResult.getErrorMessage());
				return;
			}
		}

		//Get Main Action
		ActionDefinition action = null;
		Connectivity connectivity = Connectivity.Online;

		// Get Main Definition, action and connectivity
		if (MyApplication.getApp().getMain() instanceof IDataViewDefinition)
		{
			IDataViewDefinition dataViewDef = (IDataViewDefinition)MyApplication.getApp().getMain();
			action = dataViewDef.getEvent(notificationAction);
			if (dataViewDef.getMainDataSource()!=null)
				entityParam = new Entity(dataViewDef.getMainDataSource().getStructure());
			entityParam.setExtraMembers(dataViewDef.getVariables());
			connectivity = dataViewDef.getConnectivitySupport();
		}
		else if (MyApplication.getApp().getMain() instanceof DashboardMetadata)
		{
			DashboardMetadata dashboardMetadata = ((DashboardMetadata)MyApplication.getApp().getMain());
			DashboardItem dashboardItem = dashboardMetadata.getNotificationActions().get(notificationAction);
			if (dashboardItem!=null)
				action = dashboardItem.getActionDefinition();
			entityParam.setExtraMembers(dashboardMetadata.getVariables());
			connectivity = dashboardMetadata.getConnectivitySupport();
		}

		//Set parameters to entity.
		if (Services.Strings.hasValue(notificationParameters))
		{
			GcmIntentService.addNotificationParametersToEntity(entityParam, notificationParameters);
		}

		// Warning UIContext with out activity.
		UIContext UIcontext = new UIContext(this, connectivity);

		if (action!= null)
		{
			Action doAction = ActionFactory.getAction(UIcontext, action, new ActionParameters(entityParam));
			ActionExecution exec = new ActionExecution(doAction);
			exec.executeAction();
		}
		else
		{
			Services.Log.Error("Silent Notification Failed. Action is null");
		}

	}

	// Notification constructor deprecated in API level 11, added notification builder also in API level 11
	// Cannot use yet because we support API level 8
	@SuppressLint("InlinedApi")
	public void createNotification(String payload, String action, String notificationParameters)
	{
		NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

		String appName = getString(R.string.app_name);

		SharedPreferences settings = MyApplication.getInstance().getAppSharedPreferences();
		int notificatonID = settings.getInt("notificationID", 0); // allow multiple notifications //$NON-NLS-1$

		Intent intent = new Intent("android.intent.action.MAIN"); //$NON-NLS-1$
		intent.setClassName(this, getPackageName() + ".Main"); //$NON-NLS-1$
	    intent.addCategory("android.intent.category.LAUNCHER"); //$NON-NLS-1$

	    if (Services.Strings.hasValue(action))
	    {
	    	// call main as root of the stack with the action as intent parameter. Use clear_task like GoHome
	    	if (CompatibilityHelper.isApiLevel(Build.VERSION_CODES.HONEYCOMB))
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
	    	else
	    		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

	    	intent.putExtra(IntentParameters.NotificationAction, action);
	    	intent.putExtra(IntentParameters.NotificationParameters, notificationParameters);
	    	intent.setAction(String.valueOf(Math.random()));
	    }
	    else
	    {
	    	// call main like main application shortcut
	    	intent.setFlags(0);
	    	intent.setAction("android.intent.action.MAIN");
	    }

		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		Notification notification = NotificationHelper.newBuilder(this)
			.setWhen(System.currentTimeMillis())
			.setContentTitle(appName)
			.setContentText(payload)
			.setContentIntent(pendingIntent)
			.setStyle(new NotificationCompat.BigTextStyle().bigText(payload))
			.setDefaults(Notification.DEFAULT_ALL)
			.setAutoCancel(true)
			.build();

		notificationManager.notify(notificatonID, notification);

		SharedPreferences.Editor editor = settings.edit();
	    editor.putInt("notificationID", ++notificatonID % 32); //$NON-NLS-1$
	    editor.commit();
	}

    public static void addNotificationParametersToEntity(Entity objEntity, String notificationParameters)
    {
    	try {
			JSONArray jsonArray = new JSONArray(notificationParameters);
			JSONObject jsonObject = jsonArray.getJSONObject(0);

			Iterator<?> iter = jsonObject.keys();
		    while(iter.hasNext()){
		        String key = (String)iter.next();
		        String value = jsonObject.getString(key);
		        objEntity.setProperty(key, value);
		    }

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }

    private String getJsonString(JSONObject jObj, String key) {
		if (jObj.has(key)) {
			try{
				return jObj.getString(key);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
		return null;
	}
}
