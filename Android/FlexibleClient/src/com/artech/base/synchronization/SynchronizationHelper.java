package com.artech.base.synchronization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.app.ProgressDialog;

import com.artech.R;
import com.artech.activities.ActivityHelper;
import com.artech.application.MyApplication;
import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.model.PropertiesObject;
import com.artech.base.services.AndroidContext;
import com.artech.base.services.ServiceResponse;
import com.artech.base.services.Services;
import com.artech.base.synchronization.bc.SdtGxPendingEvent;
import com.artech.base.synchronization.dbcreate.Reorganization;
import com.artech.base.synchronization.dbcreate.reorg;
import com.artech.base.synchronization.dps.getpendingeventbytimestamp;
import com.artech.base.utils.ReflectionHelper;
import com.artech.base.utils.Strings;
import com.artech.base.utils.ThreadUtils;
import com.artech.common.ServiceHelper;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.layers.LocalUtils;
import com.artech.synchronization.ISynchronizationHelper;
import com.genexus.GXutil;

import json.org.json.JSONObject;

public class SynchronizationHelper implements ISynchronizationHelper
{

	private static final String OUTPUT_PARAMETER = "ReturnValue"; //$NON-NLS-1$
	private final static String SYNC_LAST_TIME = "sync_last_time"; //$NON-NLS-1$
	private final static String SEND_LAST_TIME = "send_last_time"; //$NON-NLS-1$
	
	public final static String SYNC_BLOB_PLACEHOLDER = "<!gxfile%s!>"; //$NON-NLS-1$

	public static boolean isRunningSendOrReceive = false;
	public static boolean isRunningReceive = false;

	// SYNC constants result START
	public static final int SYNC_OK = 0;
	// not used, Change constants for IOS compat.
	public static final int SYNC_FAIL_SYNCNOTNEEDED = 1;
	public static final int SYNC_FAIL_APPNOTOFFLINE = 2;
	
	// used
	public static final int SYNC_FAIL_ERRORHASPENDINGEVENTS = 3;
	public static final int SYNC_FAIL_UNKNOWN = 99;
	public static final int SYNC_FAIL_ALREADYRUNNING = 8;
	
	//server constants
	public static final int SYNC_FAIL_SERVERBYROWVERSIONINVALID = 51;
	public static final int SYNC_FAIL_SERVERREINSTALL = 52;
	public static final int SYNC_FAIL_SERVERVERSIONINVALID = 53;
	// SYNC constants result END

	// SYNC check Constant
	public static final int SYNC_CHECK_FAIL = 3;
	// END SYNC check constant

	public static long getSyncLastTime()
	{
		return MyApplication.getInstance().getLongPreference(SYNC_LAST_TIME, 0);
	}

	public static void setSyncLastTime(long lastTime)
	{
		MyApplication.getInstance().setLongPreference(SYNC_LAST_TIME, lastTime);
	}

	public static long getSendLastTime()
	{
		return MyApplication.getInstance().getLongPreference(SEND_LAST_TIME, 0);
	}

	public static void setSendLastTime(long lastTime)
	{
		MyApplication.getInstance().setLongPreference(SEND_LAST_TIME, lastTime);
	}
	
	public enum DataSyncCriteria {
		 Automatic, Manual, AfterElapsedTime
    }

	public enum LocalChangesProcessing {
		 WhenConnected, UserDefined, Never
    }

	public static void callReorCreatePendingEvents(boolean runAsFullReor)
	{
		if (runAsFullReor)
		{
			Reorganization gxReor = new Reorganization() ;
			gxReor.execute();
		}
		else
		{
			//call the reor to create the sync events table.
			// reor run in auto commit mode
			reorg eventReor = new reorg(-1);
			try {
				eventReor.CreateGxPendingEvent();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	//Replicator

	public static EntityList getPendingEventsList(String status)
	{
		PropertiesObject parameters = new PropertiesObject();

		//Only With this Status.
		parameters.setProperty("PendingEventStatus", status);

		EntityList resultData = new EntityList();

		//call DP to get pending events
		getpendingeventbytimestamp pendingEvent = new getpendingeventbytimestamp(MyApplication.getApp().getRemoteHandle());

		LocalUtils.beginTransaction();

		try
		{
			pendingEvent.execute(parameters);

			Object output = parameters.getProperty(OUTPUT_PARAMETER);
			List<?> outputList = (List<?>)output;
			for (Object outputItem : outputList)
			{
				if (outputItem instanceof Entity)
					resultData.add((Entity)outputItem);
			}
		} finally
		{
			LocalUtils.endTransaction();
		}

		return resultData;
	}

	public static boolean sendPendingsToServerInBackground()
	{

		//TODO: run in a background thread or a service...
		//TODO: Run only if has Internet connection.
		//TODO: run only one an a Time , put a synchronized semaphore?

		// Only call send if time between send has elapsed.
		// calculate time dif
		long minTimeBetweenSends = MyApplication.getApp().getSynchronizerMinTimeBetweenSends();
		long nowTime = new Date().getTime();
		long lastSend = SynchronizationHelper.getSendLastTime();
		// minTimeBetweenSync in seconds
		if (lastSend!=0 && ((nowTime-lastSend) < (minTimeBetweenSends * 1000)))
		{
			Services.Log.debug("MinTimeBetweenSends time not happened yet.");  //$NON-NLS-1$
			return true;
		}
		
		CompatibilityHelper.executeAsyncTask(new SynchronizationSendAsyncTask());
		return true;
	}

	/* Temporary, For testing only */
	public static boolean sendPendingsToServerDummy()
	{
		CompatibilityHelper.executeAsyncTask(new SynchronizationSendAsyncTask.ProcedureExecutionDummy());
		return true;
	}

	//Synchronizer
	public static int callSynchronizer(boolean useLoadingIndicator, boolean includeHashesinPost, boolean waitIfRunning)
	{
		if (isRunningReceive)
		{
			if (waitIfRunning)
			{
				Services.Log.debug("callSynchronizer wait because another Receive is already running (Sync.Receive) "); //$NON-NLS-1$
				while(isRunningReceive) //another receive is running, wait
				{
					//wait half sec for other receive to finish
					ThreadUtils.sleep(500);
					Services.Log.debug("callSynchronizer", "wait to another Receive to finish"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				Services.Log.debug("callSynchronizer finish wait, start new (Sync.Receive) "); //$NON-NLS-1$
				
			}
			else
			{
				Services.Log.warning("SynchronizationHelper" , "callSynchronizer not run because Received is already running");
				return SYNC_FAIL_ALREADYRUNNING;
			}
		}
		isRunningSendOrReceive = true;
		isRunningReceive = true;
		
		// if has pending events, sync should fail
		EntityList pendings = SynchronizationHelper.getPendingEventsList("1"); //$NON-NLS-1$ // Pending
		if (pendings.size()>0)
		{
			// force a send if send automatic is set.
			if (MyApplication.getApp().isOfflineApplication()
					&& MyApplication.getApp().getSynchronizerSendAutomatic() )
			{
				Services.Log.debug("callOfflineReplicator (Sync.Send) from callSynchronizer (Sync.Receive) "); //$NON-NLS-1$
				//Send
				SynchronizationSendHelper.callOfflineReplicator();
				// replicator could set isRunningSendOrReceive to false
				isRunningSendOrReceive = true;
			}

			pendings = SynchronizationHelper.getPendingEventsList("1"); //$NON-NLS-1$ // Pending
			if (pendings.size()>0)
			{
				Services.Log.Error(" Has Pending events, cannot do a receive. " ); //$NON-NLS-1$
				isRunningSendOrReceive = false;
				isRunningReceive = false;
				return SYNC_FAIL_ERRORHASPENDINGEVENTS;
			}
		}

		try
		{
			String synchronizerName = MyApplication.getApp().getSynchronizer();
			if (!Services.Strings.hasValue(synchronizerName))
			{
				Services.Log.Error("Could not get syncronizer:" + synchronizerName); //$NON-NLS-1$
				return SYNC_FAIL_UNKNOWN;
			}

			// Call synchronizer like a procedure
			String url = MyApplication.getApp().UriMaker.MakeGetAllUriBC(synchronizerName);

			JSONArray jsonParameters = new JSONArray(); //should contains tables checksum

			// get hashMap from disk
			LinkedHashMap<String, String> tablesCheckSum = readHashMapFromDisk();

			if (includeHashesinPost)
			{
				// convert hashMap to json array string.
				jsonParameters = convertHashToJsonArray(tablesCheckSum);
			}

			// Add tables checksum.
			// [["TABLE1","bv834j22bjRqh9kh54EUrUXvxcAc+yNmY"],["TABLE2","bwR0SVLUk8wLKaMhy5ZRtl45T4rKS5KrJlrpZ4t"],["TABLE3",..."], ....]

			// Create Sync class
			String syncClassName = Strings.toLowerCase(synchronizerName);

			Class<? extends GXOfflineDatabase> syncClass = ReflectionHelper.getClass(GXOfflineDatabase.class, syncClassName);
			Object syncObj = ReflectionHelper.createDefaultInstance(syncClass, false);

			// Obtain the synchronizer's version
			String syncVersion;
			try
			{
				Method methodSyncVersion = syncClass.getMethod("getSyncVersion"); //$NON-NLS-1$
				syncVersion = (String) methodSyncVersion.invoke(syncObj); //+ "a";
			}
			catch (Exception e)
			{
				Services.Log.Error("Error calling method 'getSyncVersion' from synchronizer:" + synchronizerName, e); //$NON-NLS-1$
				return SYNC_FAIL_UNKNOWN;
			}

			// Execute
			ServiceResponse syncResponse = null;
			try
			{
				try
				{
					syncResponse = ServiceHelper.postJsonSyncResponse(url, jsonParameters, syncVersion);
				}
				catch (IOException e)
				{
					Services.Log.Error("Error calling when receiving the sync's json response from server", e); //$NON-NLS-1$
					return SYNC_FAIL_UNKNOWN;
				}
	
				if (!syncResponse.getResponseOk() || syncResponse.Stream == null)
				{
					Services.Log.Error("Invalid sync response from server"); //$NON-NLS-1$
					return SYNC_FAIL_UNKNOWN;
				}
	
				Integer errorCode;
				try
				{
					Method methodMetadataErrorCode = syncClass.getMethod("startJsonParser", InputStream.class); //$NON-NLS-1$
					errorCode = (Integer) methodMetadataErrorCode.invoke(syncObj, syncResponse.Stream);
				}
				catch (Exception e)
				{
					Services.Log.Error("Error calling method 'startJsonParser' from synchronizer:" + synchronizerName, e); //$NON-NLS-1$
					return SYNC_FAIL_UNKNOWN;
				}
				
				if (errorCode != 0)
				{
					Services.Log.Error("Metadata error code received. Code: " + errorCode); //$NON-NLS-1$
				}

				switch (errorCode)
				{
					// should return sync_fail .
					case 1: return SYNC_FAIL_SERVERBYROWVERSIONINVALID;
					case 2: return SYNC_FAIL_SERVERREINSTALL;
					case 3: return SYNC_FAIL_SERVERVERSIONINVALID;
				}

				if (useLoadingIndicator) createIndicator();
	
				Services.Log.debug("Start invoke local sync proc"); //$NON-NLS-1$
				LocalUtils.beginTransaction();
	
				try
				{
					try
					{
						Method methodExecuteGXAllSync = syncClass.getMethod("executeGXAllSync"); //$NON-NLS-1$
						methodExecuteGXAllSync.invoke(syncObj);
					}
					catch (Exception e)
					{
						Services.Log.Error("Error calling method 'executeGXAllSync' from synchronizer:" + synchronizerName, e); //$NON-NLS-1$
						return SYNC_FAIL_UNKNOWN;
					}

					// We are done with the response, close the stream before doing a new HTTP request.
					IOUtils.closeQuietly(syncResponse.Stream);
					
					Services.Log.debug("End invoke local sync proc" ); //$NON-NLS-1$
					Services.Log.debug("Local sync commit changes" ); //$NON-NLS-1$
	
					LinkedHashMap<String, String> newTablesCheckSum;
	
					try
					{
						// Get each table checksum and store it.
						Method methodGetTableChecksum = syncClass.getMethod("getTableChecksum"); //$NON-NLS-1$
						@SuppressWarnings("unchecked") // Using a temporal local variable so that the suppress warning affects only this call in stead of the whole method.
						LinkedHashMap<String, String> result = (LinkedHashMap<String, String>) methodGetTableChecksum.invoke(syncObj);
						newTablesCheckSum = result;
					}
					catch (Exception e)
					{
						Services.Log.Error("Error calling method 'executeGXAllSync' from synchronizer:" + synchronizerName, e); //$NON-NLS-1$
						return SYNC_FAIL_UNKNOWN;
					}
	
					// Update the checksum's table.
					for (Entry<String, String> entry : newTablesCheckSum.entrySet()) {
						tablesCheckSum.put(entry.getKey(), entry.getValue());
					}
	
					// store check sum.
					storeHashMapOnDisk(tablesCheckSum);
	
					// store check sum as json.
					jsonParameters = convertHashToJsonArray(tablesCheckSum);
					storeJsonOnDisk(jsonParameters);
	
					Services.Log.debug("DATABASE SYNCHRONIZATION FINISHED" ); //$NON-NLS-1$
					Services.Log.debug("Database file: " + AndroidContext.ApplicationContext.getDataBaseFilePath() ); //$NON-NLS-1$
					Services.Log.debug("Hashes file: " + AndroidContext.ApplicationContext.getDataBaseSyncHashesFilePath() ); //$NON-NLS-1$
	
					// Post confirm sync to server event.
					String urlConfirm = url + "&event=gxconfirmsync"; //$NON-NLS-1$
					ServiceResponse confirmSyncResponse = ServiceHelper.postJsonSyncResponse(urlConfirm, jsonParameters, syncVersion);
					IOUtils.closeQuietly(confirmSyncResponse.Stream);
	
					//set last time sync
					setSyncLastTime(new Date().getTime());
				}
				finally
				{
					LocalUtils.endTransaction();
					if (useLoadingIndicator) dismissIndicator();
				}
			}
			finally
			{
				if (syncResponse != null)
					IOUtils.closeQuietly(syncResponse.Stream);
			}
		}
		catch (Exception e)
		{
			// Any Error should return sync_Failed
			Services.Log.Error("Error running callSynchronizer method", e); //$NON-NLS-1$
			return SYNC_FAIL_UNKNOWN;
		}
		finally
		{
			isRunningSendOrReceive = false;
			isRunningReceive = false;
		}

		return SYNC_OK;
	}

	//Synchronizer Check
	public static int callSynchronizerCheck()
	{
		// get Sync Name
		String synchronizerName = MyApplication.getApp().getSynchronizer();
		if (!Services.Strings.hasValue(synchronizerName))
		{
			Services.Log.Error("Could not get syncronizer:" + synchronizerName); //$NON-NLS-1$
			return SYNC_CHECK_FAIL;
		}

		// Call synchronizer like a procedure
		String url = MyApplication.getApp().UriMaker.MakeGetAllUriBC(synchronizerName);
		
		// get hashMap from disk
		LinkedHashMap<String, String> tablesCheckSum = readHashMapFromDisk();
		// convert hashMap to json array string.
		JSONArray jsonParameters = convertHashToJsonArray(tablesCheckSum); //should contains tables checksum

		// Create Sync class
		String syncClassName = Strings.toLowerCase(synchronizerName);
		Class<? extends GXOfflineDatabase> syncClass = ReflectionHelper.getClass(GXOfflineDatabase.class, syncClassName);
		Object syncObj = ReflectionHelper.createDefaultInstance(syncClass, false);
		// Obtain the synchronizer's version
		String syncVersion;
		try
		{
			Method methodSyncVersion = syncClass.getMethod("getSyncVersion"); //$NON-NLS-1$
			syncVersion = (String) methodSyncVersion.invoke(syncObj);
		}
		catch (Exception e)
		{
			Services.Log.Error("Error calling method 'getSyncVersion' from synchronizer:" + synchronizerName, e); //$NON-NLS-1$
			return SYNC_CHECK_FAIL;
		}


		//Post confirm sync to server event.
		String urlConfirm = url + "&event=gxchecksync"; //$NON-NLS-1$
		try {
			ServiceResponse response = ServiceHelper.postJsonSyncResponse(urlConfirm, jsonParameters, syncVersion);
			// read responce.
			StringWriter writer = new StringWriter();
			IOUtils.copy(response.Stream, writer);
			String testOutput = writer.toString();

			json.org.json.JSONArray metadataArray;
			try
			{
				metadataArray = new json.org.json.JSONArray(testOutput);
				if (metadataArray!=null)
				{
					json.org.json.JSONArray metadataArrayInner = metadataArray.optJSONArray(0);
					if (metadataArrayInner!=null)
					{
						String errorCode = metadataArrayInner.optString(4);
						return Integer.parseInt(errorCode);
					}
				}

			} catch (json.org.json.JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


			// Read sync check result.
			return SYNC_CHECK_FAIL;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return SYNC_CHECK_FAIL;
	}

	public static void storeHashMapOnDisk(
			LinkedHashMap<String, String> tablesCheckSum) {
		String filePath = AndroidContext.ApplicationContext.getDataBaseSyncFilePath();


		FileOutputStream fos;
		try {
				fos = new FileOutputStream(filePath);

				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(tablesCheckSum);
				oos.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	private static void storeJsonOnDisk(
			JSONArray jsonParameters ) {
		String filePath = AndroidContext.ApplicationContext.getDataBaseSyncHashesFilePath();


		FileOutputStream fos;
		try {
				fos = new FileOutputStream(filePath);

				OutputStreamWriter osw = new OutputStreamWriter(fos);
				osw.write(jsonParameters.toString());
				osw.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	@SuppressWarnings("unchecked")
	private static LinkedHashMap<String, String> readHashMapFromDisk() {
		LinkedHashMap<String, String> tablesCheckSum = new LinkedHashMap<String, String>();

		String filePath = AndroidContext.ApplicationContext.getDataBaseSyncFilePath();
		File file = new File(filePath);

	    try {

	    	if (file.exists())
			{
	    		FileInputStream fis = new FileInputStream(filePath);

	    		ObjectInputStream ois = new ObjectInputStream(fis);
	    		tablesCheckSum = (LinkedHashMap<String, String>) ois.readObject();
	    		ois.close();
			}

		} catch (OptionalDataException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    return tablesCheckSum;
	}

	public static JSONArray readJsonArrayFromDisk() {
		JSONArray jsonArray = new JSONArray();

		String filePath = AndroidContext.ApplicationContext.getDataBaseSyncHashesFilePath();
		File file = new File(filePath);

	    try {

	    	if (file.exists())
			{
	    		FileInputStream fis = new FileInputStream(filePath);
	    		String fileContent = "";
	    		InputStreamReader isr = new InputStreamReader(fis);
	    		BufferedReader buffreader = new BufferedReader(isr);
	            String line;

	    	    // read every line of the file into the line-variable, on line at the time
	    	    while ( ( line = buffreader.readLine())!=null ) {
	    	        // do something with the settings from the file
	    	    	fileContent += line;
	    	    }
	    		jsonArray = new JSONArray(fileContent);
	    		isr.close();
			}

		} catch (OptionalDataException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    return jsonArray;
	}

	// [["TABLE1","bv834j22bjRqh9kh54EUrUXvxcAc+yNmY"],["TABLE2","bwR0SVLUk8wLKaMhy5ZRtl45T4rKS5KrJlrpZ4t"],["TABLE3",..."], ....]


	private static JSONArray convertHashToJsonArray(
			LinkedHashMap<String, String> tablesCheckSum) {
		JSONArray jsonParameters = new JSONArray(); //should contains tables checksum

		for (String key : tablesCheckSum.keySet()) {
			JSONArray tableInfo = new JSONArray();
			tableInfo.put(key);
			tableInfo.put(tablesCheckSum.get(key));

			//Services.Log.debug("key: " + key + " value "+ tablesCheckSum.get(key) );

			jsonParameters.put(tableInfo);

		 }
		Services.Log.debug("jsonParameters" + jsonParameters.toString() ); //$NON-NLS-1$

		return jsonParameters;
	}

	public static LinkedHashMap<String, String> convertJsonArraytoHash(
			JSONArray jsonParameters) {
		LinkedHashMap<String, String> tablesCheckSum = new LinkedHashMap<String, String>(); //should contains tables checksum

		for (int index =0; index<jsonParameters.length(); index++ )
		{
			JSONArray tableinfo = jsonParameters.optJSONArray(index);
			if (tableinfo!=null)
			{
				String tableName = tableinfo.optString(0);
		    	String tableChecksum = tableinfo.optString(1);

		    	tablesCheckSum.put(tableName, tableChecksum );

			}
		 }
		return tablesCheckSum;
	}

	private static ProgressDialog progressDialog = null;

	//UI Helper , show indicator.
	private static void createIndicator() {
		// run in ui thread
		Services.Device.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				// Hide previous dialog, if any.

				// Create dialog from configured data (and store it for later operations).
				Activity activity = ActivityHelper.getCurrentActivity();
				if (activity!=null)
				{
					progressDialog = new ProgressDialog(activity);
					progressDialog.setTitle(Services.Strings.getResource(R.string.GXM_ReceivingData));
					progressDialog.setMessage(Services.Strings.getResource(R.string.GXM_PleaseWait));
					progressDialog.setCancelable(false);

					// Show the new dialog.
					progressDialog.show();
				}
			}
		});
	}

	private static void dismissIndicator()
	{
		if (progressDialog == null)
			return;

		Services.Device.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				if (progressDialog != null)
				{
					try
					{
						progressDialog.dismiss();
					}
					catch (Exception e) 
					{
						e.printStackTrace();
					}
					progressDialog = null;
				}
			}
		});
	}

	//Called inside a proc only
	@Override
	public short syncReceive()
	{
		//Call From a Gx Procedure, should close transaction first
		LocalUtils.commit();
		LocalUtils.endTransaction();

		Services.Log.debug("callSynchronizer (Sync.Receive) from Procedure code "); //$NON-NLS-1$
		int result = SynchronizationHelper.callSynchronizer( false, true, true);

		//Start proc transaction again
		LocalUtils.beginTransaction();

		return (short) result;
	}

	//Called inside a proc only
	@Override
	public short syncSend()
	{
		//Call From a Gx Procedure, should close transaction first
		LocalUtils.commit();
		LocalUtils.endTransaction();

		Services.Log.debug("callOfflineReplicator (Sync.Send) from Procedure code"); //$NON-NLS-1$
		int result = SynchronizationSendHelper.callOfflineReplicator();

		//Start proc transaction again
		LocalUtils.beginTransaction();

		return (short) result;
	}

	//Called inside a proc only
	@Override
	public short syncStatus() {

		int result = SynchronizationHelper.callSynchronizerCheck();

		return (short) result;
	}

	@Override
	public void processBCBlobsBeforeSaved(String bcName, String data, String dataOlds, TreeMap<String, String> mapFilesToSend)
	{

		StructureDefinition bcDef = MyApplication.getApp().getDefinition().getBusinessComponent(bcName);
		if (bcDef != null)
		{
			List<DataItem> items = bcDef.getItems();
			for (DataItem def : items)
			{
				if (def.isMediaOrBlob())
				{
					String attBlobName = def.getName();

					try {
						JSONObject dataObject = new JSONObject(dataOlds);
						String attBlobGXIValue = dataObject.optString(attBlobName + "_GXI", null);

						mapFilesToSend.put(attBlobName, attBlobGXIValue);

					} catch (json.org.json.JSONException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	@Override
	public String replaceBCBlobsAfterSave(String bcName, String action, String data, String dataOlds, TreeMap<String, String> mapFilesToSend, json.org.json.JSONArray arrayFilesToSend)
	{
		int arrayIndex = 0;
		StructureDefinition bcDef = MyApplication.getApp().getDefinition().getBusinessComponent(bcName);
		if (bcDef != null)
		{
			List<DataItem> items = bcDef.getItems();
			for (DataItem def : items)
			{
				if (def.isMediaOrBlob())
				{
					String attBlobName = def.getName();

					try {
						JSONObject dataObject = new JSONObject(data);
						String attBlobValue = dataObject.getString(attBlobName);

						//get the file from hashMap
						String attBlobPreviusGXIValue = mapFilesToSend.get(attBlobName);

						JSONObject dataObjectOlds = new JSONObject(dataOlds);
						String attBlobGXIValue = dataObjectOlds.optString(attBlobName + "_GXI", null);

						// not send always , only when change, if not change , don't send the image.
						if (attBlobGXIValue!=null && attBlobPreviusGXIValue!=null &&
							attBlobGXIValue.equalsIgnoreCase(attBlobPreviusGXIValue) &&
								action.equalsIgnoreCase("upd") )
						{
							// not sent image
							dataObject.remove(attBlobName);
							data = dataObject.toString();
						}
						else
						{
							File file = new File(attBlobValue);

							if (file.exists())
							{
								// copy file to upload Directory.
								String filename = file.getName();
					        	String outputFile = AndroidContext.ApplicationContext.getFilesSubApplicationDirectory("upload") + "/" + filename; //$NON-NLS-1$
					    		try {
									FileUtils.copyFile(file, new File(outputFile));
								} catch (IOException e) {
									e.printStackTrace();
								} 
					    		// add file to upload list.		
					    		file = new File(outputFile);
					    		if (file.exists())
					    		{
					    			//	replace path with placeholder.
					    			String valuePlaceHolder = String.format(SYNC_BLOB_PLACEHOLDER, String.valueOf(arrayIndex));
					    			dataObject.put(attBlobName, valuePlaceHolder);
					    			arrayFilesToSend.put(outputFile);
					    			arrayIndex++;
					    			data = dataObject.toString();
					    		}
							}
						}
					} catch (json.org.json.JSONException e)
					{
						e.printStackTrace();
					}
				}
			}
		// if something get replaced return data modified
		}
		return data;
	}


	 public static void restorePendingToDatabase(EntityList paramEntityList)
	 {
		 @SuppressWarnings("rawtypes")
		 Iterator localIterator = paramEntityList.iterator();
		 while (true)
		 {
			 if (!localIterator.hasNext())
				 return;
			 Entity localEntity = (Entity)localIterator.next();
			 SdtGxPendingEvent localSdtGxPendingEvent = new SdtGxPendingEvent(MyApplication.getApp().getRemoteHandle());
			 UUID localUUID = UUID.fromString(localEntity.optStringProperty("EventId")); //$NON-NLS-1$
			 localSdtGxPendingEvent.setgxTv_SdtGxPendingEvent_Pendingeventid(localUUID);
			 localSdtGxPendingEvent.setgxTv_SdtGxPendingEvent_Pendingeventaction(Short.parseShort(localEntity.optStringProperty("EventAction"))); //$NON-NLS-1$
			 localSdtGxPendingEvent.setgxTv_SdtGxPendingEvent_Pendingeventtimestamp(GXutil.charToTimeREST(localEntity.optStringProperty("EventTimestamp"))); //$NON-NLS-1$
			 localSdtGxPendingEvent.setgxTv_SdtGxPendingEvent_Pendingeventbc(localEntity.optStringProperty("EventBC")); //$NON-NLS-1$
			 localSdtGxPendingEvent.setgxTv_SdtGxPendingEvent_Pendingeventdata(localEntity.optStringProperty("EventData")); //$NON-NLS-1$
			 localSdtGxPendingEvent.setgxTv_SdtGxPendingEvent_Pendingeventextras(localEntity.optStringProperty("EventExtras")); //$NON-NLS-1$
			 String status = localEntity.optStringProperty("EventStatus");
			 localSdtGxPendingEvent.setgxTv_SdtGxPendingEvent_Pendingeventstatus(Short.parseShort(status));
			 localSdtGxPendingEvent.setgxTv_SdtGxPendingEvent_Pendingeventerrors(localEntity.optStringProperty("EventErrors")); //$NON-NLS-1$
			 localSdtGxPendingEvent.setgxTv_SdtGxPendingEvent_Pendingeventfiles(localEntity.optStringProperty("EventFiles")); //$NON-NLS-1$
			 if (localSdtGxPendingEvent.getTransaction() == null)
				 continue;
			 try
			 {
				 LocalUtils.beginTransaction();
				 localSdtGxPendingEvent.getTransaction().SetMode("INS"); //$NON-NLS-1$
				 localSdtGxPendingEvent.getTransaction().Save();
				 if (localSdtGxPendingEvent.success())
					 LocalUtils.commit();
				 Services.Log.debug("restorePendingToDatabase", "Save sucessfully " + localUUID + " , " + status); //$NON-NLS-1$
			 }
			 catch (Exception localException)
			 {
				 Services.Log.Error("restorePendingToDatabase", "Save failed " + localSdtGxPendingEvent.getTransaction().GetMessages().toString()); //$NON-NLS-1$
				 localException.printStackTrace();
			 }
			 finally
			 {
				 LocalUtils.endTransaction();
			 }
		 }
	  }



}
