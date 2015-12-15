package com.artech.controls;

import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class ControlPropertiesDefinition
{
	private final LayoutItemDefinition mItem;

	public ControlPropertiesDefinition(LayoutItemDefinition item)
	{
		if (item == null)
			throw new IllegalArgumentException("Null item definition."); //$NON-NLS-1$

		mItem = item;
	}

	public LayoutItemDefinition getItem() { return mItem; }

	@Override
	public String toString()
	{
		return mItem.getName();
	}

	/**
	 * Helper method to read a "data item" property from a control. Normally, this can
	 * be an attribute or an SDT variable/SDT field combination; this method unifies them
	 * and returns an expression that can be used to get the value.
	 * @param dataProperty Name of the property with the "Data Attribute".
	 * @param selectorProperty Name of the property with the "Data Field Selector"
	 * @return The expression used to get the value from the data entity.
	 */
	public String readDataExpression(String dataProperty, String selectorProperty)
	{
		String data;
		String selector;

		// Try to read from layout item first, in case they are "common" to different control definitions.
		data = mItem.optStringProperty(dataProperty);
		selector = mItem.optStringProperty(selectorProperty);

		if (mItem.getControlInfo() != null)
		{
			if (!Services.Strings.hasValue(data))
				data = mItem.getControlInfo().optStringProperty(dataProperty);

			if (!Services.Strings.hasValue(selector))
				selector = mItem.getControlInfo().optStringProperty(selectorProperty);
		}

		return getDataExpression(data, selector);
	}

	protected String getDataSourceMember()
	{
		if (mItem instanceof GridDefinition)
			return ((GridDefinition)mItem).getDataSourceMember();
		else
			return null;
	}

	protected String getDataExpression(String dataItem, String fieldSelector)
	{
		// Possible cases:
		// 1) No data item -> do nothing.
		if (!Services.Strings.hasValue(dataItem) || dataItem.equalsIgnoreCase("(none)"))
			return null;

		// 2) DataItem is an attribute -> return it.
		// 3) DataItem is a "simple" variable in the DS (no field selector) -> return it.
		if (!Services.Strings.hasValue(fieldSelector))
			return dataItem;

		// 4) Field selector is present, which means that the data item is a structured variable.
		if (dataItem.equalsIgnoreCase(getDataSourceMember()))
		{
			// 4.1) DataItem is the same variable as the SDT collection of the grid -> strip the "Item(x)" part from the field selector and return that.
			final String INDEXER = "Item(0)."; //$NON-NLS-1$
			if (Strings.starsWithIgnoreCase(fieldSelector, INDEXER))
				fieldSelector = fieldSelector.substring(INDEXER.length());

			return fieldSelector;
		}
		else
		{
			// 4.2) DataItem is a "complex" variable in the DS -> concatenate with field selector and return it.
			return dataItem + Strings.DOT + fieldSelector;
		}
	}
}
