package com.artech.base.synchronization;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sqldroid.SQLDroidDriver;

import com.artech.android.media.utils.FileUtils;
import com.artech.application.MyApplication;
import com.artech.base.application.IProcedure;
import com.artech.base.application.OutputResult;
import com.artech.base.metadata.StructureDataType;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.metadata.VariableDefinition;
import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.model.PropertiesObject;
import com.artech.base.providers.IApplicationServer;
import com.artech.base.services.Services;
import com.artech.base.synchronization.bc.SdtGxPendingEvent;
import com.artech.base.utils.Strings;
import com.artech.layers.LocalUtils;
import com.genexus.GXutil;

public class SynchronizationSendHelper
{
	public static boolean isRunningSendBackground = false;
	public static boolean isRunningSend = false;

	// SYNC send constants result START
	public static final int SYNC_SEND_OK = 0;
	public static final int SYNC_SEND_ERROROPENTRANSACTIONS = 1;
	public static final int SYNC_SEND_ERRORUNKNOWN = 2;
	public static final int SYNC_SEND_REPLICATORNOTFOUND = 3;
	
	public static final int SYNC_SEND_OKWITHERRORS_UPLOAD = 5;
	public static final int SYNC_SEND_OKWITHERRORS_SAVE = 6;
	public static final int SYNC_SEND_OKWITHERRORS_MAPPINGS = 7;
	public static final int SYNC_SEND_ERROR_ALREADYRUNNING = 8;
	// SYNC send constants result END


	public static int callOfflineReplicator()
	{
		int result = SYNC_SEND_OK;
		if (isRunningSend)
		{
			Services.Log.warning("ProcedureReplicator" , "callOfflineReplicator not run because Send is already running");
			return SYNC_SEND_ERROR_ALREADYRUNNING;
		}
		SynchronizationHelper.isRunningSendOrReceive = true;
		isRunningSend = true;
		
		IProcedure procedure = MyApplication.getApplicationServer(Connectivity.Online).getProcedure("GxOfflineEventReplicator");

		Services.Log.debug("Call OfflineEventReplicator.");

		PropertiesObject parameter = new PropertiesObject();
		EntityList pendings = SynchronizationHelper.getPendingEventsList("1"); //$NON-NLS-1$ // Pending

		Services.Log.debug("OfflineEventReplicator sending " + pendings.size() + " events.");

		if (pendings.size()>0)
		{
			//StructureDefinition defSdt = pendings.get(0).getDefinition();

			Services.Log.debug("Call ProcedureReplicator. input: " + String.valueOf(pendings.size()) );

			// Upload local blobs to server if has any
			// and modified the pending to add blobs file send to server.
			for (Entity pendingEventToSend : pendings)
			{
				uploadFilesFromPendingEvents(pendingEventToSend);
			}

			parameter.setProperty("GxPendingEvents", pendings);

			// Add GxAppVersion parameter to sync send
			String versionMetadata = MyApplication.getApp().getMajorVersion() + "." + MyApplication.getApp().getMinorVersion();
			
			// Set GxSynchroInfo parameter
			StructureDataType defStructureInput = MyApplication.getApp().getDefinition().getSDT("GxSynchroInfoSDT"); //$NON-NLS-1$
			if (defStructureInput!=null)
			{
				Entity myEntityParm = new Entity(defStructureInput.getStructure());
				myEntityParm.setProperty("GxAppVersion", versionMetadata); //$NON-NLS-1$
				parameter.setProperty("GxSyncroInfo", myEntityParm); //$NON-NLS-1$
			}
			else
			{
				Services.Log.Error("ProcedureReplicator" , "Error getting GxSynchroInfoSDT, input SDT not found"); //$NON-NLS-1$
			}
			
			// Log pending events to send
			//Services.Log.debug("OfflineEventReplicator sending Json: " + pendings.toString() + " .");

			OutputResult procResult = procedure.execute(parameter);

			if (procResult.isOk())
			{
				//set last time send
				SynchronizationHelper.setSendLastTime(new Date().getTime());
				
				//Services.Log.debug("GxPendingEvents result: " + parameter.toString());

				// create a temp var to desearialize json result, cannot start with gx.
				StructureDataType defStructureOutput = MyApplication.getApp().getDefinition().getSDT("GxSynchroEventResultSDT");
				if (defStructureOutput==null)
				{
					Services.Log.Error("ProcedureReplicator" , "Error calling ProcedureReplicator , output SDT not found");
					isRunningSendBackground = false;
					SynchronizationHelper.isRunningSendOrReceive = false;
					isRunningSend = false;
					return SYNC_SEND_REPLICATORNOTFOUND;
				}

				StructureDefinition defSdtOutput = defStructureOutput.getStructure();
				VariableDefinition varSdtOutput = new VariableDefinition("PedingsEvents", true, defSdtOutput);
				StructureDefinition localVars = new StructureDefinition(Strings.EMPTY);
				localVars.Root.Items.add(varSdtOutput);

				Entity localResult = new Entity(localVars);

				//localResult.load(new NodeObject(jsonBC));

				localResult.setProperty(varSdtOutput.getName(), parameter.getProperty("EventResults"));

				// process proc output to GxPending Events.
				EntityList results = (EntityList)localResult.getProperty(varSdtOutput.getName());
				EntityList listMappingsWithErrors = new EntityList();
				
				for (Entity pendingEvent : results)
				{

					String pendingId = pendingEvent.getProperty("EventId").toString();
					UUID pendingEventId = GXutil.strToGuid(pendingId);

					SdtGxPendingEvent sdtTrn = new SdtGxPendingEvent(MyApplication.getApp().getRemoteHandle());

					LocalUtils.beginTransaction();

					try
					{
						sdtTrn.Load(pendingEventId);

						if (sdtTrn.Success())
						{
							//update pending event with status and error message.
							String pendingEventStatus = pendingEvent.getProperty("EventStatus").toString();
							Short pendingEventStatusShort = Short.parseShort(pendingEventStatus);
							sdtTrn.setgxTv_SdtGxPendingEvent_Pendingeventstatus(pendingEventStatusShort); //status

							String pendingEventErrors = pendingEvent.getProperty("EventErrors").toString();
							sdtTrn.setgxTv_SdtGxPendingEvent_Pendingeventerrors(pendingEventErrors); //errors

							if (sdtTrn.getTransaction()!=null)
							{
								try
								{
									// TODO Apply mapping from  pendingEvent results.
									EntityList listMappings = pendingEvent.getLevel("Mappings");

									if (listMappings!=null && listMappings.size()>0)
									{
										applyMappings(listMappings, listMappingsWithErrors);
										//if (!applyMappings(listMappings))
										//	result = SYNC_SEND_OKWITHERRORS_MAPPINGS;
									}

									if (pendingEventStatusShort == 3) // Server sucessfully, delete it after apply mapping.
										sdtTrn.getTransaction().SetMode("DLT");

									// save event
									sdtTrn.getTransaction().Save();
									if (sdtTrn.success())
									{
										LocalUtils.commit();
						
										deleteFilesIfNecessary(sdtTrn, pendingEventStatusShort);
									}
									else
									{
										Services.Log.Error("ProcedureReplicator" , "Save failed, not commit " + sdtTrn.getTransaction().GetMessages().toString());
										//if (sdtTrn.getTransaction().GetMessages().size()>0)
										//	Services.Log.Error("ProcedureReplicator" , "Save failed, message " + sdtTrn.getTransaction().GetMessages().get(0).toString());
									}	
									Services.Log.debug("ProcedureReplicator" , "Save sucessfully " + pendingId + " , " + pendingEventStatus);
									
									
								}
								catch (Exception ex)
								{
									// if fail just print stack trace for now.
									Services.Log.Error("ProcedureReplicator" , "Save failed " + sdtTrn.getTransaction().GetMessages().toString());
									ex.printStackTrace();
									result = SYNC_SEND_OKWITHERRORS_SAVE;
								}
							}
						}
						else
						{
							Services.Log.Error("ProcedureReplicator" , "Result not found" + pendingEvent.toString());
						}
					}
					finally
					{
						LocalUtils.endTransaction();
					}
					
				//end for
				}

				if (listMappingsWithErrors.size()>0)
				{
					try
					{
						LocalUtils.beginTransaction();

						EntityList listFinalMappingsWithErrors = new EntityList();
						// 	apply failing mappings if possible.
						if (!applyMappings(listMappingsWithErrors, listFinalMappingsWithErrors))
						{
							Services.Log.Error("ProcedureReplicator" , "Error applying some mappings " + listFinalMappingsWithErrors.size());
							result = SYNC_SEND_OKWITHERRORS_MAPPINGS;
						}
						else
						{
							Services.Log.debug("ProcedureReplicator" , "Applying All mappings ok");
						}
						LocalUtils.commit();
					}
					finally
					{
						SynchronizationHelper.isRunningSendOrReceive = false;
						isRunningSend = false;
						LocalUtils.endTransaction();
					}
				}
				
				
			}
			else
			{
				Services.Log.Error("ProcedureReplicator" , "Error calling ProcedureReplicator " + procResult.getErrorText());
				result = SYNC_SEND_REPLICATORNOTFOUND;
			}
		}
		Services.Log.debug("End Call ProcedureReplicator.");
		SynchronizationHelper.isRunningSendOrReceive = false;
		isRunningSend = false;
		return result;
	}

	private static void uploadFilesFromPendingEvents(Entity pendingEventToSend) 
	{
		String eventFiles = pendingEventToSend.getProperty("EventFiles").toString();
		// if has files to send.
		if (Services.Strings.hasValue(eventFiles))
		{
			try {
				JSONArray eventFilesArray = new JSONArray(eventFiles);
				if (eventFilesArray.length()>0)
				{
					String eventData = pendingEventToSend.getProperty("EventData").toString();

					for(int arrayIndex = 0; arrayIndex < eventFilesArray.length(); arrayIndex++)
					{
						Services.Log.debug("Uploading blob : " + String.valueOf(arrayIndex + 1) + " of " + String.valueOf(eventFilesArray.length()));

						String fileToSendPath = eventFilesArray.getString(arrayIndex);
						// Upload file
						String fileExtenion = fileToSendPath.substring(fileToSendPath.lastIndexOf('.') + 1);

						File file = new File(fileToSendPath);
						long dataLength = file.length();
						InputStream data = new FileInputStream(file);
						String mimeType = FileUtils.getMimeType(file);

						IApplicationServer serverApp = MyApplication.getApplicationServer(Connectivity.Online);
						String binaryToken = serverApp.uploadBinary(fileExtenion, mimeType, data, dataLength, null);

						String valuePlaceHolder = String.format(SynchronizationHelper.SYNC_BLOB_PLACEHOLDER, String.valueOf(arrayIndex));

						if (binaryToken!=null)
							eventData = eventData.replace(valuePlaceHolder, binaryToken);
						else
							Services.Log.Error("Error uploading bynary file!"); //$NON-NLS-1$
					}

					pendingEventToSend.setProperty("EventData", eventData);

				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void deleteFilesIfNecessary(SdtGxPendingEvent sdtTrn,
			Short pendingEventStatusShort) 
	{
		// remove successfully upload files. if pendingEventStatusShort is 3 and has files.
		if (pendingEventStatusShort == 3)
		{
			// get the files
			String eventFiles = sdtTrn.getgxTv_SdtGxPendingEvent_Pendingeventfiles();
			
			// if has files to send.
			if (Services.Strings.hasValue(eventFiles))
			{
				try 
				{
					JSONArray eventFilesArray = new JSONArray(eventFiles);
					if (eventFilesArray.length()>0)
					{
						for(int arrayIndex = 0; arrayIndex < eventFilesArray.length(); arrayIndex++)
						{
							String fileToSendPath = eventFilesArray.getString(arrayIndex);
							// Delete file
							File file = new File(fileToSendPath);
							if (file.exists())
							{
								file.delete();
							}
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}	
		}
	}

	private static boolean applyMappings(EntityList listMappings, EntityList listMappingsWithErrors)
	{
		int mappingsCount = listMappings.size();
		EntityList listMappingsFail = new EntityList();
		
		Boolean continueApplyingMappings = true;
		
		while (continueApplyingMappings)
		{
			//each entity is a mapping.
			for (Entity mapping : listMappings)
			{
				String conditions = mapping.optStringProperty("Conditions");
				String table = mapping.optStringProperty("Table");
				String updates = mapping.optStringProperty("Updates");

				if (!Services.Strings.hasValue(table))
				{
					Services.Log.Error("table name must be of type string."); //$NON-NLS-1$
					break;
				}

				JSONArray conditionsArray;
				try {
					conditionsArray = new JSONArray(conditions);
				} catch (JSONException e) {
					Services.Log.Error("conditions must be of type array."); //$NON-NLS-1$
					break;
				}

				JSONArray updatesArray;
				try {
					updatesArray = new JSONArray(updates);
				} catch (JSONException e) {
					Services.Log.Error("updates must be of type array."); //$NON-NLS-1$
					break;
				}

				StringBuilder sqlSent = new StringBuilder();
				sqlSent.append("UPDATE ["); //$NON-NLS-1$

				sqlSent.append(table);
				sqlSent.append("] SET "); //$NON-NLS-1$

				ArrayList<String> stringParametersValues = new ArrayList<String>();
			
				if (!appendFromArray(sqlSent, updatesArray, ", ", stringParametersValues))
					break;

				sqlSent.append(" WHERE "); //$NON-NLS-1$

				if (!appendFromArray(sqlSent, conditionsArray, " AND ", stringParametersValues))
					break;

				String sqlSentToExecute = sqlSent.toString();

				//	execute mapping statements.
				Services.Log.debug("execute mapping:" + sqlSentToExecute );

				try {
					PreparedStatement statement = SQLDroidDriver.getCurrentConnection().prepareStatement(sqlSentToExecute);
					//statement parameters.
					int parIndex = 1;
					for(String parameterValue : stringParametersValues)
					{
						//Services.Log.debug("parameter : " + parIndex + " " + parameterValue);
						statement.setString(parIndex, parameterValue);
						parIndex++;
					}
					//statement execute.
					statement.execute();
				} catch (SQLException e) {
					Services.Log.Error("ProcedureReplicator" , "Error applyng mappings " + e.getMessage() + " " + sqlSentToExecute);
					e.printStackTrace();
					listMappingsFail.add(mapping);
				}

			}
		
			int mappingsFailCount = listMappingsFail.size();
			if (mappingsFailCount > 0)
			{
				if (mappingsFailCount < mappingsCount)
				{
					Services.Log.debug("ProcedureReplicator" , "Fail applyng mappings count: " + mappingsFailCount + " of " + mappingsCount + " retring");
					listMappings = listMappingsFail;
					mappingsCount = listMappings.size();
					listMappingsFail = new EntityList();
					// continue applying mappings
					continueApplyingMappings = true;
				}
				else
				{
					//return the mappings that fails to process later, could not be processed here.
					Services.Log.debug("ProcedureReplicator" , "Fail to process mappings : " + mappingsFailCount + " of " + mappingsCount + " ");
					continueApplyingMappings = false;
					for (Entity mapping : listMappingsFail)
					{
						listMappingsWithErrors.add(mapping);
					}
					return false;
				}
			}
			else
			{
				continueApplyingMappings = false;
			}
		}
		return true;
	}

	private static boolean appendFromArray(StringBuilder sqlSent, JSONArray attsArray, String separator, ArrayList<String> stringParametersValues)
	{
		for (int i = 0; i < attsArray.length(); i++) {
		    JSONObject row;
			try {
				row = attsArray.getJSONObject(i);
			    String sKey = row.getString("Key");
			    String sValue = row.getString("Value");

			    if (i>0)
			    	sqlSent.append(separator);

			    // value in '' 
			    sqlSent.append("[").append(sKey).append("] = ?");
			    stringParametersValues.add(sValue);
			} catch (JSONException e) {
				Services.Log.Error("items must have a Key-Value pair."); //$NON-NLS-1$
				return false;
			}
		}
		return true;
	}

}
