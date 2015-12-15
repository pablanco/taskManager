package com.artech.nuevaprueba.mainthumb;

import com.artech.providers.EntityDataProvider;

public class AppEntityDataProvider extends EntityDataProvider
{
	public AppEntityDataProvider()
	{
		EntityDataProvider.AUTHORITY = "com.artech.nuevaprueba.mainthumb.appentityprovider";
		EntityDataProvider.sURIMatcher = buildUriMatcher();
	}
}
