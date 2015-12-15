package com.artech.controls.common;

import java.util.List;

import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class StaticValueItems extends ValueItems<ValueItem>
{
	public StaticValueItems(ControlInfo controlInfo)
	{
		super();
		initializeItems(controlInfo);
	}

	private void initializeItems(ControlInfo controlInfo)
	{
		if (controlInfo.optBooleanProperty("@AddEmptyItem")) //$NON-NLS-1$
		{
			ValueItem emptyItem = new ValueItem(Strings.EMPTY, controlInfo.getTranslatedProperty("@EmptyItemText"));
			setEmptyItem(emptyItem);
		}

		String strControlValues = controlInfo.optStringProperty("@ControlValues"); //$NON-NLS-1$
		if (Services.Strings.hasValue(strControlValues))
		{
			List<String> controlValues = Services.Strings.decodeStringList(strControlValues, ',');
			for (String strItem : controlValues)
			{
				List<String> itemParts = Services.Strings.decodeStringList(strItem, ':');
				if (itemParts.size() == 2)
				{
					String itemValue = itemParts.get(1);
					String itemDescription = Services.Resources.getTranslation(itemParts.get(0));

					add(new ValueItem(itemValue, itemDescription));
				}
			}
		}
	}
}
