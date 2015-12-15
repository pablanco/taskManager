package com.artech.android.api;

import java.lang.reflect.Method;

import com.artech.application.MyApplication;
import com.artech.base.model.EntityList;
import com.artech.base.model.PropertiesObject;
import com.artech.base.services.AndroidContext;
import com.artech.base.services.IEntity;
import com.artech.base.synchronization.SynchronizationHelper;
import com.artech.base.synchronization.dps.SdtGxSynchroEventSDT_GxSynchroEventSDTItem;
import com.artech.base.synchronization.dps.deletependingeventsbyid;
import com.artech.base.synchronization.dps.getpendingeventbytimestamp;
import com.artech.base.synchronization.dps.markpendingeventsbyid;
import com.artech.base.utils.ReflectionHelper;
import com.artech.layers.LocalUtils;
import com.genexus.GXObjectCollectionBase;


/**
 * This class allow access to SynchronizationEvents.
 * @author fpanizza
 *
 */
public class SynchronizationEvents
{

	//private static final String OUTPUT_PARAMETER = "ReturnValue"; //$NON-NLS-1$

	// When call from inside proc shold not begintranscation , either commit it.
	public static boolean hasEvents(Integer status)
	{
		EntityList pendings = SynchronizationHelper.getPendingEventsList(status.toString());
		return pendings.size()>0;
	}

	
	// From client events
	public static EntityList getEventsLocal(Integer status)
	{
		return SynchronizationHelper.getPendingEventsList(status.toString());
	}
	
	// When call from inside procedure should not beginTranscation , either commit it.
	public static Object getEvents(Integer status)
	{
		// must return a GxObjectCollection
		String className = "GxObjectCollection";
		Class<?> clazz = ReflectionHelper.getClass(Object.class, className);
		Object gxObjectCollection = ReflectionHelper.createDefaultInstance(clazz, true);
	
		// populate object colection from proc.
		//Only with this status
		
		//call DP to get pending events
		getpendingeventbytimestamp pendingEvent = new getpendingeventbytimestamp(MyApplication.getApp().getRemoteHandle());
		
		try
		{
			pendingEvent.execute(status.shortValue(), new GXObjectCollectionBase[] { (GXObjectCollectionBase)gxObjectCollection} );
		
		} finally
		{
			
		}

		className = "SdtGxSynchroEventSDT_GxSynchroEventSDTItem";
		Class<?> clazzGen = ReflectionHelper.getClass(Object.class, className);
		
		// Convert Object collection items from  to SdtGxSynchroEventSDT_GxSynchroEventSDTItem
		if (clazzGen!=null)
		{
			Object sdttypedGen = ReflectionHelper.createDefaultInstance(clazzGen, true);
			if (sdttypedGen!= null)
			{
				Object gxObjectCollectionGen = ReflectionHelper.createDefaultInstance(clazz, true);
				GXObjectCollectionBase collectionBaseGen = (GXObjectCollectionBase)gxObjectCollectionGen;
				
				GXObjectCollectionBase collectionBase = (GXObjectCollectionBase)gxObjectCollection;
				for (int i = 0; i < collectionBase.size(); i++)
				{
					SdtGxSynchroEventSDT_GxSynchroEventSDTItem sdttyped = (SdtGxSynchroEventSDT_GxSynchroEventSDTItem) collectionBase.elementAt(i);
					// in root module
					IEntity objOutElement = AndroidContext.ApplicationContext.createEntity("", "GxSynchroEventSDT.GxSynchroEventSDTItem", null);
					sdttyped.sdttoentity(objOutElement);
			          
					Method methodGen = ReflectionHelper.getMethodEntity(clazzGen, "entitytosdt");
			        try {
			        	methodGen.invoke(sdttypedGen, objOutElement);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			        collectionBaseGen.add(sdttypedGen);
				}
		
				// return collection converted if posible.
				return gxObjectCollectionGen;
			}
			
		}
		return gxObjectCollection;
	}

	/*
	public static void markEventAsPendingLocal(String guid)
	{
		java.util.UUID guidVal = java.util.UUID.fromString(guid);
		markEventAsPending(guidVal, true);
	}
	*/
	
	// java.util.UUID
	public static void markEventAsPending(java.util.UUID guidVal)
	{
		markEventAsPending(guidVal, false);
	}
	 
	public static void markEventAsPending(java.util.UUID guid, boolean applyTransaction)
	{
		PropertiesObject parameters = new PropertiesObject();

		//Only this event
		parameters.setProperty("PendingEventId", guid.toString());
		parameters.setProperty("PendingEventStatus", "1"); //$NON-NLS-1$ // Pending

		//EntityList resultData = new EntityList();

		//call Proc to mark pending events
		markpendingeventsbyid markPendingEvent = new markpendingeventsbyid(MyApplication.getApp().getRemoteHandle());

		if (applyTransaction)
			LocalUtils.beginTransaction();

		try
		{
			markPendingEvent.execute(parameters);

			// Commit?
			if (applyTransaction)
				LocalUtils.commit();
			// not read output
		} finally
		{
			if (applyTransaction)
				LocalUtils.endTransaction();
		}
	}
	
	
	// java.util.UUID
	public static void removeEvent(java.util.UUID guidVal)
	{
		markEventAsPending(guidVal, false);
	}
		
	// java.util.UUID
	public static void removeEvent(java.util.UUID guid, boolean applyTransaction)
	{
		PropertiesObject parameters = new PropertiesObject();

		//Only this event
		parameters.setProperty("PendingEventId", guid.toString());
		
		//EntityList resultData = new EntityList();

		//call Proc to mark pending events
		deletependingeventsbyid removePendingEvent = new deletependingeventsbyid(MyApplication.getApp().getRemoteHandle());

		if (applyTransaction)
			LocalUtils.beginTransaction();

		try
		{
			removePendingEvent.execute(parameters);

			// Commit?
			if (applyTransaction)
				LocalUtils.commit();
			// not read output
		} finally
		{
			if (applyTransaction)
				LocalUtils.endTransaction();
		}
		
	}
	
		

}
