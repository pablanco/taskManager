package com.artech.base.metadata;

import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;

public class DataSourceAttributeDefinition extends DataItem
{
	private static final long serialVersionUID = 1L;

	private final boolean mIsKey;

	public DataSourceAttributeDefinition(INodeObject json)
	{
		super(getDefinition(json));

		// Although name is in definition, getDefinition() may fail if the
		// attributes file was not loaded, so set it from here.
		setProperty("Name", getAttributeName(json)); //$NON-NLS-1$

		// Read properties from the attribute node proper.
		mIsKey = json.optBoolean("@isKey"); //$NON-NLS-1$
		setStorageType(json.optInt("@internalType")); //$NON-NLS-1$
	}

	@Override
	public boolean isKey()
	{
		return mIsKey;
	}

	private static ITypeDefinition getDefinition(INodeObject json)
	{
		return Services.Application.getAttribute(getAttributeName(json));
	}

	private static String getAttributeName(INodeObject json)
	{
		 return MetadataLoader.getAttributeName(json.optString("@attribute"));		 //$NON-NLS-1$
	}
}
