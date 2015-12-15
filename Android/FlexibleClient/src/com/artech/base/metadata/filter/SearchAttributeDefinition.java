package com.artech.base.metadata.filter;

import com.artech.base.metadata.DataItemHelper;
import com.artech.base.metadata.DataSourceMemberDefinition;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;

public class SearchAttributeDefinition extends DataSourceMemberDefinition
{
	private static final long serialVersionUID = 1L;

	private final String mName;
	private final String mDescription;

	SearchAttributeDefinition(IDataSourceDefinition parent, INodeObject jsonData)
	{
		super(parent);
		String name = MetadataLoader.getAttributeName(jsonData.getString("@attribute")); //$NON-NLS-1$

		mName = DataItemHelper.getNormalizedName(name);
		mDescription = jsonData.getString("@description"); //$NON-NLS-1$
	}

	@Override
	public String getName() { return mName; }

	public String getDescription() { return Services.Resources.getTranslation(mDescription); }
}
