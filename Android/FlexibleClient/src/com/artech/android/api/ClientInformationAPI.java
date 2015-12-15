package com.artech.android.api;

import java.util.List;
import java.util.Locale;

import android.support.annotation.NonNull;

import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;

/**
 * This class allow access to device information from API.
 * @author FPanizza
 *
 */
public class ClientInformationAPI extends ExternalApi
{
	@Override
	public @NonNull ExternalApiResult execute(String method, List<Object> parameterValues)
	{
		Object result = null;
		if (method.equalsIgnoreCase("id")) //$NON-NLS-1$
		{
			// Get device Id
			result = ClientInformation.id();
		}
		else if (method.equalsIgnoreCase("osname")) //$NON-NLS-1$
		{
			// Get device os name
			result = ClientInformation.osName();
		}
		else if (method.equalsIgnoreCase("osversion")) //$NON-NLS-1$
		{
			// Get device os version
			result = ClientInformation.osVersion();
		}
		else if (method.equalsIgnoreCase("networkid")) //$NON-NLS-1$
		{
			// Get device network id
			result = ClientInformation.networkId();
		}
		else if (method.equalsIgnoreCase("language")) //$NON-NLS-1$
		{
			// Get device language.
			result = ClientInformation.getLocaleString(Locale.getDefault());
		}
		else
			return ExternalApiResult.failureUnknownMethod(this, method);

		return ExternalApiResult.success(result);
	}
}
