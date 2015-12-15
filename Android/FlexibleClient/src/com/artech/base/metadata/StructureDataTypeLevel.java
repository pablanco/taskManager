package com.artech.base.metadata;

import com.artech.base.serialization.INodeCollection;
import com.artech.base.serialization.INodeObject;

public class StructureDataTypeLevel extends StructureDataTypeItem
{
	private static final long serialVersionUID = 1L;

	public StructureDataTypeLevel(INodeObject obj, StructureDataTypeItem parent)
	{
		super(obj, parent);
	}

	@Override
	public void deserialize(INodeObject obj)
	{
		String isColl = obj.optString("IsCollection"); //$NON-NLS-1$
		if (isColl.length() > 0)
			Boolean.parseBoolean(isColl);

		String name = obj.getString("Name"); //$NON-NLS-1$
		setName(name);

		INodeCollection items = obj.optCollection("Items"); //$NON-NLS-1$
		if (items != null)
		{
			for (int i = 0; i < items.length() ; i++)
			{
				INodeObject item = items.getNode(i);
				INodeCollection subItems = item.getCollection("Items"); //$NON-NLS-1$

				StructureDataTypeItem subItem;
				if (subItems != null)
					subItem = new StructureDataTypeLevel(item, this);
				else
					subItem = new StructureDataTypeItem(item, this);

				subItem.deserialize(item);
				getItems().add(subItem);
			}
		}
	}
}
