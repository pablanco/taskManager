package com.artech.android.api;

import java.util.List;

import android.support.annotation.NonNull;

import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;

public class CalendarAPI extends ExternalApi
{
	@Override
	public @NonNull ExternalApiResult execute(String method, List<Object> parameters)
	{
		if (method.equalsIgnoreCase("schedule")) //$NON-NLS-1$
		{
			if (SDActions.addAppointmentFromParameters(getActivity(), toString(parameters)))
				return ExternalApiResult.SUCCESS_CONTINUE;
			else
				return InteropAPI.getInteropActionFailureResult();
		}

		return ExternalApiResult.failureUnknownMethod(this, method);
	}
}
