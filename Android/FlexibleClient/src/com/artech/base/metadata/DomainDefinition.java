package com.artech.base.metadata;

import java.io.Serializable;
import java.util.Vector;

import com.artech.base.metadata.enums.ControlTypes;
import com.artech.base.serialization.INodeCollection;
import com.artech.base.serialization.INodeObject;

public class DomainDefinition extends DataTypeDefinition implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final Vector<EnumValuesDefinition> EnumValues = new Vector<EnumValuesDefinition>();

	public DomainDefinition(INodeObject jsonData)
	{
		super(jsonData);

		INodeCollection enumValues = jsonData.optCollection("EnumValues"); //$NON-NLS-1$
		for (int j = 0; j < enumValues.length() ; j++) {
			INodeObject objEnumValue = enumValues.getNode(j);

			String enumValueName = objEnumValue.getString("Name"); //$NON-NLS-1$
			String enumValueValue = objEnumValue.getString("Value"); //$NON-NLS-1$
			String enumValueDescription = objEnumValue.getString("Description");	 //$NON-NLS-1$

			EnumValuesDefinition valuesDef = new EnumValuesDefinition();
			valuesDef.setName(enumValueName);
			valuesDef.setValue(enumValueValue);
			valuesDef.setDescription(enumValueDescription);

			EnumValues.addElement(valuesDef);
		}

		if (enumValues.length() > 0)
			setProperty("IsEnumeration", "true"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public Vector<EnumValuesDefinition> getEnumValues()
	{
		return EnumValues;
	}

	public EnumValuesDefinition getEnumValueByName(String name)
	{
		for(int i = 0; i < EnumValues.size(); i++)
		{
			EnumValuesDefinition rel = EnumValues.elementAt(i);
			if (rel.getName().equalsIgnoreCase(name))
				return rel;
		}

		return null;
	}

	public EnumValuesDefinition getEnumValueByValue(String value)
	{
		for(int i = 0; i < EnumValues.size(); i++)
		{
			EnumValuesDefinition rel = EnumValues.elementAt(i);
			if(rel.getValue().equalsIgnoreCase(value))
				return rel;
		}
		return null;
	}


	@Override
	public boolean getIsEnumeration()
	{
		return EnumValues != null && EnumValues.size() > 0;
	}

	static boolean isSpecialDomain(ITypeDefinition domain)
	{
		//TODO data
		DataTypeName name = new DataTypeName(domain.getName());
		return (!name.GetControlType().equalsIgnoreCase(ControlTypes.TextBox)) || (name.GetActions().size()!=0) || (domain.getIsEnumeration());
	}
}
