package com.artech.base.metadata.types;

import java.io.Serializable;
import java.util.List;

import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.DataTypeDefinition;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.metadata.enums.DataTypes;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;

public class BusinessComponentDataType extends DataTypeDefinition implements Serializable, IStructuredDataType
{
	private static final long serialVersionUID = 1L;

	private final StructureDefinition mStructure;

	public BusinessComponentDataType(StructureDefinition structure)
	{
		super(null);
		mStructure = structure;
	}

	@Override
	public String getName() { return mStructure.getName(); }

	@Override
	public String getType() { return DataTypes.businesscomponent; }

	@Override
	public StructureDefinition getStructure() { return mStructure; }

	@Override
	public boolean isCollection() { return false; }

	@Override
	public List<DataItem> getItems()
	{
		return mStructure.getItems();
	}

	@Override
	public DataItem getItem(String name)
	{
		return mStructure.getAttribute(name);
	}

	@Override
	public int getLength() { return 0; }

	@Override
	public int getDecimals() { return 0; }

	@Override
	public boolean getSigned() { return false; }

	@Override
	public boolean getIsEnumeration() { return false; }

	@Override
	public Object getEmptyValue(boolean isCollection)
	{
		if (isCollection)
		{
			return new EntityList();
		}
		else
		{
			Entity entity = new Entity(getStructure());
			entity.initialize();
			return entity;
		}
	}
}
