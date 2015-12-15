package com.artech.base.metadata;

import java.io.Serializable;

/**
 * Abstract class for members of an IDataSourceDefinition.
 * @author matiash
 *
 */
public abstract class DataSourceMemberDefinition implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final IDataSourceDefinition mParent;

	public DataSourceMemberDefinition(IDataSourceDefinition parent)
	{
		mParent = parent;
	}

	public IDataSourceDefinition getParent() { return mParent; }
	public abstract String getName();
}
