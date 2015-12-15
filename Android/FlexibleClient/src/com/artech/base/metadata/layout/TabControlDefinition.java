package com.artech.base.metadata.layout;

import java.util.ArrayList;
import java.util.List;

import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class TabControlDefinition extends LayoutItemDefinition
{
	private static final long serialVersionUID = 1L;

	public enum TabStripKind { Fixed, Scrollable }

    // Constants of size
	private static final int TAB_HEIGHT = 48; // Ugly hack; this changes by theme. Value from Holo.

	private ArrayList<TabItemDefinition> mTabItems;
	private TabStripKind mTabStripKind;

	public TabControlDefinition(LayoutDefinition layout, LayoutItemDefinition parent)
	{
		super(layout, parent);
	}

	//calculate bound of it childs tabs pages.
	public void calculateBounds(float absoluteWidth, float absoluteHeight)
	{
		// remove tab header to size.
		float absoluteHeightMinusTab = absoluteHeight - getTabWidgetHeight();
		if (absoluteHeightMinusTab < 0)
			absoluteHeightMinusTab = 0;

		//rest tab padding. tab not have padding anymore.
		float absoluteWidthMinusTab = absoluteWidth;
		if (absoluteWidthMinusTab < 0)
			absoluteWidthMinusTab = 0;

		for (TabItemDefinition item : getTabItems())
		{
			TableDefinition itemTable = item.getTable();
			itemTable.calculateBounds(absoluteWidthMinusTab, absoluteHeightMinusTab);
		}
	}

	public List<TabItemDefinition> getTabItems()
	{
		if (mTabItems == null)
		{
			ArrayList<TabItemDefinition> list = new ArrayList<TabItemDefinition>();
			for (LayoutItemDefinition item : getChildItems())
			{
				if (item instanceof TabItemDefinition)
					list.add((TabItemDefinition)item);
			}

			mTabItems = list;
		}

		return mTabItems;
	}

	public TabStripKind getTabStripKind()
	{
		if (mTabStripKind == null)
		{
			// Platform Default: Fixed if Tabs <= 3, scrollable otherwise.
			String strValue = optStringProperty("@TabsBehavior");
			if (Strings.hasValue(strValue) && strValue.equalsIgnoreCase("Show More Button"))
				mTabStripKind = TabStripKind.Fixed;
			else if (Strings.hasValue(strValue) && strValue.equalsIgnoreCase("Scroll"))
				mTabStripKind = TabStripKind.Scrollable;
			else // No value, unknown, or "Platform Default".
				mTabStripKind = (getTabItems().size() <= 3 ? TabStripKind.Fixed : TabStripKind.Scrollable);
		}

		return mTabStripKind;
	}

	/**
	 * Returns the height (in dips) of a tab control.
	 */
	public static int getTabWidgetHeight()
	{
		return Services.Device.dipsToPixels(TAB_HEIGHT);
	}

}
