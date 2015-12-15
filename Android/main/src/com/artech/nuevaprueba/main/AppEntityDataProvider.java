package com.artech.nuevaprueba.main;

import com.artech.providers.EntityDataProvider;

public class AppEntityDataProvider extends EntityDataProvider
{
	public AppEntityDataProvider()
	{
		EntityDataProvider.AUTHORITY = "com.artech.nuevaprueba.main.appentityprovider";
		EntityDataProvider.sURIMatcher = buildUriMatcher();
	}
}
