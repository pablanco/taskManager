package com.artech.base.metadata.settings;

import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.IContext;

public class WorkWithSettingsLoader
{
	private static final String FILE_NAME = "settings"; //$NON-NLS-1$

	public static WorkWithSettings load(IContext context)
	{
		INodeObject jsonData = MetadataLoader.getDefinition(context, FILE_NAME);
		if (jsonData == null)
			return null;

		return loadFromJson(jsonData);
	}

	private static WorkWithSettings loadFromJson(INodeObject jsonData)
	{
		return new WorkWithSettings(jsonData);
	}
}
