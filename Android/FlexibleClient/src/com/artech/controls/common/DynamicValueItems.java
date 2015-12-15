package com.artech.controls.common;

import java.util.Map;

public class DynamicValueItems extends ValueItems<ValueItem>
{
	public DynamicValueItems(Map<String, String> values)
	{
		super();

		for (Map.Entry<String, String> value : values.entrySet())
			add(new ValueItem(value.getKey(), value.getValue()));
	}
}
