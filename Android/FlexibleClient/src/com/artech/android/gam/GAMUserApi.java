package com.artech.android.gam;

import java.util.List;

import com.artech.externalapi.ExternalApi;

public class GAMUserApi extends ExternalApi
{
	public GAMUserApi()
	{
		addSimpleMethodHandler("GetId", 0, new ISimpleMethodInvoker()
		{
			@Override
			public Object invoke(List<Object> parameters)
			{
				return GAMUser.getCurrentUserId();
			}
		});

		addSimpleMethodHandler("GetLogin", 0, new ISimpleMethodInvoker()
		{
			@Override
			public Object invoke(List<Object> parameters)
			{
				return GAMUser.getCurrentUserLogin();
			}
		});

		addSimpleMethodHandler("GetName", 0, new ISimpleMethodInvoker()
		{
			@Override
			public Object invoke(List<Object> parameters)
			{
				return GAMUser.getCurrentUserName();
			}
		});

		addSimpleMethodHandler("GetExternalId", 0, new ISimpleMethodInvoker()
		{
			@Override
			public Object invoke(List<Object> parameters)
			{
				return GAMUser.getCurrentUserExternalId();
			}
		});

		addSimpleMethodHandler("GetEmail", 0, new ISimpleMethodInvoker()
		{
			@Override
			public Object invoke(List<Object> parameters)
			{
				return GAMUser.getCurrentUserEMail();
			}
		});

		addSimpleMethodHandler("IsAnonymous", 0, new ISimpleMethodInvoker()
		{
			@Override
			public Object invoke(List<Object> parameters)
			{
				return GAMUser.getCurrentUserIsAnonymous();
			}
		});
	}
}
