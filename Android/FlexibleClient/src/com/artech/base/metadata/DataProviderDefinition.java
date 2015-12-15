package com.artech.base.metadata;

import com.artech.base.metadata.enums.GxObjectTypes;

public class DataProviderDefinition extends GxObjectDefinition
{
	public DataProviderDefinition(String name)
	{
		super(GxObjectTypes.DATAPROVIDER, name);
	}
}
