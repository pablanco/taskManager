package com.artech.base.metadata.layout;

import com.artech.base.metadata.enums.LayoutItemsTypes;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.serialization.INodeObject;

public class ActionGroupDataItemDefinition extends ActionGroupItemDefinition
{
	private static final long serialVersionUID = 1L;

	private final LayoutItemDefinition mDataItem;

	public ActionGroupDataItemDefinition(ActionGroupDefinition parent, INodeObject json)
	{
		super(parent, json);
		mDataItem = new LayoutItemDefinition(parent.getLayout(), null);
		mDataItem.setType(LayoutItemsTypes.Data);
		mDataItem.readData(json);
	}

	public LayoutItemDefinition getDataItem()
	{
		return mDataItem;
	}

	@Override
	public int getType()
	{
		return ActionGroupItem.TYPE_DATA;
	}

	@Override
	public String getCaption()
	{
		return mDataItem.getCaption();
	}

	@Override
	public ThemeClassDefinition getThemeClass()
	{
		return mDataItem.getThemeClass();
	}
}
