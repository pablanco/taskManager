package com.artech.android.api;

import java.util.List;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;
import com.artech.fragments.LayoutFragment;

public class EventDispatcher extends ExternalApi
{
	public static final String ACTION_NAME = "__GxAction";

	@Override
	public @NonNull ExternalApiResult execute(String method, List<Object> parameters)
	{
		Intent intent = new Intent(LayoutFragment.GENEXUS_EVENTS);
		intent.putExtra(ACTION_NAME, getDefinition().getGxObject() + "." + method);

		for (int i = 0; i < parameters.size() ; i++)
			intent.putExtra(String.valueOf(i), String.valueOf(parameters.get(i)));

		LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
		return ExternalApiResult.SUCCESS_CONTINUE;
	}
}
