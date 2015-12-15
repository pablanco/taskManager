package com.artech.controls;

import java.util.List;

import android.content.Context;

import com.artech.base.metadata.EnumValuesDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.utils.Strings;

public class GxEnumTextView extends GxTextView implements IGxComboEdit
{
	private List<? extends EnumValuesDefinition> m_EnumValues = null;

	public GxEnumTextView(Context context, LayoutItemDefinition definition)
	{
		super(context, definition);
	}

	@Override
	public String getGx_Value() {

		String description = getText().toString();
		if (description!=null)
		{
			return findValueFromDescription(description);
		}
		return Strings.EMPTY;
	}

	@Override
	public void setGx_Value(String value) {
		String description = findValueDescription(value);
		this.setText(description);
	}

	@Override
	public String getGx_Tag() {
		return (String)this.getTag();
	}

	@Override
	public void setComboValues(List<? extends EnumValuesDefinition> values)
	{
		//Do something with enumvalues.
		m_EnumValues = values;
	}

	private String findValueDescription(String value)
	{
		if (m_EnumValues!=null)
		{
			for (EnumValuesDefinition item : m_EnumValues) {
				if (value.equalsIgnoreCase(item.getValue()))
						return item.getDescription();
			}
		}
		return Strings.EMPTY;
	}

	private String findValueFromDescription(String description)
	{
		if (m_EnumValues!=null)
		{
			for (EnumValuesDefinition item : m_EnumValues) {
				if (description.equalsIgnoreCase(item.getDescription()))
					return item.getValue();
			}
		}
		return Strings.EMPTY;
	}

	@Override
	public void setGx_Tag(String data) {
		this.setTag(data);
	}

	@Override
	public LayoutItemDefinition getDefinition() {
		return mDefinition;
	}


}
