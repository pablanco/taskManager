package com.artech.actions;

import java.util.List;

import com.artech.application.MyApplication;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;

public class ExternalObjectEvent
{
//	private final String mExternalObjectName;
//	private final String mEventName;
	private final String mFullEventName;
	
	public ExternalObjectEvent(String externalObject, String event)
	{
//		mExternalObjectName = externalObject;
//		mEventName = event;
		mFullEventName = String.format("%s.%s", externalObject, event);
	}
	
	public void fire(List<Object> parameters)
	{
		// This event is fired in two places:
		// 1) In the main object (even if the app is in background).
		ExternalObjectEvent.fireApplicationEvent(mFullEventName, parameters);
		
		// 2) In the current object (if any).
	}

	private static void fireApplicationEvent(final String eventName, List<Object> parameters)
	{
		if (!Services.Application.isLoaded())
			return;

		final IViewDefinition mainObject = MyApplication.getApp().getMain();
		if (mainObject == null)
			return;
			
		final ActionDefinition eventHandler = mainObject.getEvent(eventName);
		if (eventHandler == null)
			return; // No event to fire.
		
		// Assign the event parameters to the event handler input variables, by position.
		final Entity data = new Entity(StructureDefinition.EMPTY);
		data.setExtraMembers(mainObject.getVariables());
		for (int i = 0; i < eventHandler.getEventParameters().size() && i < parameters.size(); i++)
			data.setProperty(eventHandler.getEventParameters().get(i).getValue(), parameters.get(i));

		// Fire the event.
		Services.Device.invokeOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				UIContext context = new UIContext(MyApplication.getAppContext(), mainObject.getConnectivitySupport());

				Action eventHandlerImpl = ActionFactory.getAction(context, eventHandler, new ActionParameters(data));
				ActionExecution eventHandlerExec = new ActionExecution(eventHandlerImpl);
				eventHandlerExec.executeAction();
			}
		});
	}
}
