package com.artech.base.metadata;

import java.util.ArrayList;

public class DataSourceDefinitionList extends ArrayList<IDataSourceDefinition>
{
	private static final long serialVersionUID = 1L;

	public IDataSourceDefinition get(String name)
	{
		for (IDataSourceDefinition ds : this)
			if (ds.getName().equalsIgnoreCase(name))
				return ds;
		
		return null;
	}
}
