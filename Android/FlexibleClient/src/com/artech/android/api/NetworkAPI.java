package com.artech.android.api;

import java.util.List;

import android.support.annotation.NonNull;

import com.artech.actions.ActionResult;
import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;

/**
 * This class allow access to network information from API.
 * @author FPanizza
 *
 */
public class NetworkAPI extends ExternalApi
{
	@Override
	public @NonNull ExternalApiResult execute(String method, List<Object> parameterValues)
	{
		Object result = null;
		if (method.equalsIgnoreCase("ApplicationServerURL")) //$NON-NLS-1$
		{
			// Get device Id
			result = Network.applicationServerUrl();
		}
		else if (method.equalsIgnoreCase("IsServerAvailable")) //$NON-NLS-1$
		{
			// Get device os name
			String serverAddress = (parameterValues.size() != 0 ? parameterValues.get(0).toString() : null); 
			result = Network.isServerAvailable(serverAddress);
		}
		else if (method.equalsIgnoreCase("Type")) //$NON-NLS-1$
		{
			// Get device network id
			result = Network.type();
			
			//TODO with parameter
		}
		else if (method.equalsIgnoreCase("TrafficBasedCost")) //$NON-NLS-1$
		{
			// Get device language.
			result = Network.trafficBasedCost();
			
			//TODO with parameter
		}

		return new ExternalApiResult(ActionResult.SUCCESS_CONTINUE, result);
	}
}
