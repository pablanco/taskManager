package com.artech.externalapi;

import java.util.List;

import android.support.annotation.NonNull;

import com.artech.base.services.Services;

public class DummyExternalApi extends ExternalApi
{
	private final String mName;
	
	public DummyExternalApi(String name)
	{
		mName = name;
	}
	
	@Override
	public @NonNull ExternalApiResult execute(String method, List<Object> parameters)
	{
		Services.Log.warning(String.format("Dummy implementation for %s.%s()", mName, method));
		return ExternalApiResult.SUCCESS_CONTINUE;
	}
}
