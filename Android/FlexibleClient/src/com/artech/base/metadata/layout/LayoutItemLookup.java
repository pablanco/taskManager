package com.artech.base.metadata.layout;

import java.io.Serializable;

import com.artech.base.metadata.DataItemHelper;
import com.artech.base.metadata.enums.LayoutItemsTypes;
import com.artech.base.utils.NameMap;
import com.artech.base.utils.Strings;

/**
 * Helper class to efficiently look up controls in a layout definition.
 * @author matiash
 */
class LayoutItemLookup implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private final LayoutDefinition mLayout;
	private final NameMap<LayoutItemDefinition> mByControlName;
	private final NameMap<LayoutItemDefinition> mByBoundData;

	public LayoutItemLookup(LayoutDefinition layout)
	{
		mLayout = layout;
		mByControlName = new NameMap<LayoutItemDefinition>();
		mByBoundData = new NameMap<LayoutItemDefinition>();

		mLayout.accept(new Visitor());
	}

	public LayoutItemDefinition getControl(String name)
	{
		return mByControlName.get(name);
	}

	public LayoutItemDefinition getDataControl(String dataId)
	{
		return mByBoundData.get(DataItemHelper.getNormalizedName(dataId));
	}

	private class Visitor implements ILayoutVisitor
	{
		@Override
		public void enterVisitor(LayoutItemDefinition visitable) { }

		@Override
		public void visit(LayoutItemDefinition item)
		{
			String controlName = item.getName();
			if (Strings.hasValue(controlName))
				mByControlName.put(controlName, item);

			if (item.getType().equalsIgnoreCase(LayoutItemsTypes.Data))
			{
				String dataId = DataItemHelper.getNormalizedName(item.getDataId());
				if (Strings.hasValue(dataId))
					mByBoundData.put(dataId, item);
			}
		}

		@Override
		public void leaveVisitor(LayoutItemDefinition visitable) { }
	}
}
