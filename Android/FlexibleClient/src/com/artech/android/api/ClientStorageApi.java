package com.artech.android.api;

import java.util.List;

import android.support.annotation.NonNull;

import com.artech.actions.ActionResult;
import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;

@SuppressWarnings("unused")
public class ClientStorageApi extends ExternalApi
{
	private static final String METHOD_SET = "Set";
	private static final String METHOD_GET = "Get";
	private static final String METHOD_REMOVE = "Remove";
	private static final String METHOD_CLEAR = "Clear";

	@Override
	public @NonNull ExternalApiResult execute(String method, List<Object> parameters)
	{
		List<String> parameterValues = toString(parameters);
		if (METHOD_GET.equalsIgnoreCase(method) && parameters.size() >= 1)
		{
			String key = parameterValues.get(0);
			Object value = ClientStorage.get(key);
			return new ExternalApiResult(ActionResult.SUCCESS_CONTINUE, value);
		}
		else if (METHOD_SET.equalsIgnoreCase(method) && parameters.size() >= 2)
		{
			String key = parameterValues.get(0);
			String value = parameterValues.get(1);
			ClientStorage.set(key, value);
			return ExternalApiResult.SUCCESS_CONTINUE;
		}
		else if (METHOD_REMOVE.equalsIgnoreCase(method) && parameters.size() >= 1)
		{
			String key = parameterValues.get(0);
			ClientStorage.remove(key);
			return ExternalApiResult.SUCCESS_CONTINUE;
		}
		else if (METHOD_CLEAR.equalsIgnoreCase(method))
		{
			ClientStorage.clear();
			return ExternalApiResult.SUCCESS_CONTINUE;
		}
		else
			return ExternalApiResult.failureUnknownMethod(this, method);
	}
}
