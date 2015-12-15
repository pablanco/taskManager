package com.artech.base.metadata;

import com.artech.base.serialization.INodeObject;

class DataSourceVariableDefinition extends VariableDefinition
{
	private static final long serialVersionUID = 1L;

	public DataSourceVariableDefinition(INodeObject varNode)
	{
		super(varNode);
		setStorageType(varNode.optInt("@internalType")); //$NON-NLS-1$
	}

	@Override
	public boolean isKey()
	{
		// Variables can never be part of the primary key.
		return false;
	}
}
