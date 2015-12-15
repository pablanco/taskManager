package com.artech.base.metadata.layout;

import com.artech.base.metadata.enums.LayoutItemsTypes;

public class LayoutItemDefinitionFactory
{
	public static LayoutItemDefinition createDefinition(LayoutDefinition layout, LayoutItemDefinition parent, String attName)
	{
		if (attName.equalsIgnoreCase(LayoutItemsTypes.Data)	|| 
			attName.equalsIgnoreCase(LayoutItemsTypes.Image) ||
			attName.equalsIgnoreCase(LayoutItemsTypes.TextBlock) ||
			attName.equalsIgnoreCase(LayoutItemsTypes.Content) ||
			attName.equalsIgnoreCase(LayoutItemsTypes.AllContent))
			return new LayoutItemDefinition(layout, parent);

		if (attName.equalsIgnoreCase(LayoutItemsTypes.Action))
			return new LayoutActionDefinition(layout, parent);

		if (attName.equalsIgnoreCase((LayoutItemsTypes.UserControl)))
			return new LayoutUserControlDefinition(layout, parent);
		
		if (attName.equalsIgnoreCase(LayoutItemsTypes.Group))
			return new GroupDefinition(layout, parent);
		
		if (attName.equalsIgnoreCase(LayoutItemsTypes.Row)) 
			return new RowDefinition(layout, parent);
		
		if (attName.equalsIgnoreCase(LayoutItemsTypes.Cell))
			return new CellDefinition(layout, parent);
		
		if (attName.equalsIgnoreCase(LayoutItemsTypes.Grid))
			return new GridDefinition(layout, parent); 
			
		if (attName.equalsIgnoreCase(LayoutItemsTypes.Table))
			return new TableDefinition(layout, parent);

		if (attName.equalsIgnoreCase(LayoutItemsTypes.Tab))
			return new TabControlDefinition(layout, parent);
		
		if (attName.equalsIgnoreCase(LayoutItemsTypes.TabPage))
			return new TabItemDefinition(layout, parent);

		if (attName.equalsIgnoreCase(LayoutItemsTypes.OneContent))
			return new ContentDefinition(layout, parent);

		if (attName.equalsIgnoreCase(LayoutItemsTypes.Component))
			return new ComponentDefinition(layout, parent);

		return null;
	}
}
