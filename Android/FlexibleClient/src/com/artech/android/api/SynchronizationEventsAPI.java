package com.artech.android.api;

import java.util.List;

import android.support.annotation.NonNull;

import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;

/**
 * This class allow access to synchronization events from API.
 * @author FPanizza
 *
 */
public class SynchronizationEventsAPI extends ExternalApi
{
	@Override
	public @NonNull	ExternalApiResult execute(String method, List<Object> parameters)
	{
		List<String> parameterValues = toString(parameters);

		if (method.equalsIgnoreCase("hasevents")) //$NON-NLS-1$
		{
			Integer status = readInteger(parameterValues, 0);
			boolean result = SynchronizationEvents.hasEvents(status);
			return ExternalApiResult.success(result);
		}
		else if (method.equalsIgnoreCase("getevents")) //$NON-NLS-1$
		{
			Integer status = readInteger(parameterValues, 0);
			Object result = SynchronizationEvents.getEventsLocal(status);
			return ExternalApiResult.success(result);
		}
		else if (method.equalsIgnoreCase("markeventaspending")) //$NON-NLS-1$
		{
			String guid = parameterValues.get(0);
			java.util.UUID guidVal = java.util.UUID.fromString(guid);
			
			// Mark event as pending
			SynchronizationEvents.markEventAsPending(guidVal, true);
			return ExternalApiResult.SUCCESS_CONTINUE;
		}
		else if (method.equalsIgnoreCase("removeevent")) //$NON-NLS-1$
		{
			String guid = parameterValues.get(0);
			java.util.UUID guidVal = java.util.UUID.fromString(guid);
			
			// Remove event
			SynchronizationEvents.removeEvent(guidVal, true);
			return ExternalApiResult.SUCCESS_CONTINUE;
		}
		else
			return ExternalApiResult.failureUnknownMethod(this, method);
	}

	private static Integer readInteger(List<String> values, int arrayIndex)
	{
		Integer timeout = 0;
		if (values.size() > arrayIndex)
		{
			try{
				timeout = Integer.valueOf(values.get(arrayIndex)); }
			catch (NumberFormatException ex)
			{ /* return 0 as default */}
		}
		return timeout;
	}
}
