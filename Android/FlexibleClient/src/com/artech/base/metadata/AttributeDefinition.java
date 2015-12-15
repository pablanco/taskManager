package com.artech.base.metadata;

import java.io.Serializable;
import java.util.List;

import com.artech.base.serialization.INodeObject;

public class AttributeDefinition extends DataTypeDefinition implements Serializable
{
	private static final long serialVersionUID = 1L;

	public AttributeDefinition(INodeObject jsonData)
	{
		super(jsonData);
	}

	public String getSupertype()
	{
		return optStringProperty("AtributeSuperType"); //$NON-NLS-1$
	}

	@Override
	public List<EnumValuesDefinition> getEnumValues()
	{
		if (getBaseType() != null)
			return getBaseType().getEnumValues();

		return null;
	}
}
