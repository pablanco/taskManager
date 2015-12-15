package com.artech.controls;

import java.math.BigDecimal;

import android.content.Context;
import android.graphics.Rect;
import android.text.InputType;
import android.view.Gravity;

import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.FormatHelper;
import com.artech.ui.Coordinator;

public class GxEditTextNumeric extends GxEditText
{
	private String mDisplayValue = Strings.ZERO;
	private String mEditableValue = Strings.ZERO;
	private boolean mHasFocus;

	public GxEditTextNumeric(Context context, Coordinator coordinator, LayoutItemDefinition def)
	{
		super(context, coordinator, def);
		setInputType(InputType.TYPE_CLASS_NUMBER);

		DataItem dataItem = def.getDataItem();
		int filterLength = dataItem.getLength();
		if (dataItem.getLength() > 0 && dataItem.getDecimals() > 0)
		{
			setInputType(getInputType() | InputType.TYPE_NUMBER_FLAG_DECIMAL);
			filterLength++;
			if (dataItem.getSigned())
			{
				setInputType(getInputType() | InputType.TYPE_NUMBER_FLAG_SIGNED);
				filterLength++;
			}
		}
		else
		{
			if (dataItem.getLength() > 0)
			{
				if (dataItem.getSigned())
				{
					setInputType(getInputType() | InputType.TYPE_NUMBER_FLAG_SIGNED);
					filterLength++;
				}
			}
		}

		if (Strings.hasValue(getPicture()) && getPicture().length() > filterLength)
			filterLength = getPicture().length();

		setMaximumLength(filterLength);
		setGravity(Gravity.RIGHT);
		setMaxEms(10);

		setUpPasswordInput(getInputType());
	}

	@Override
	public void setGx_Value(String value)
	{
		super.setGx_Value(value); // Needed to track the last value for ControlValueChanged.
		internalSetValue(value);
	}

	@Override
	public String getGx_Value()
	{
		// In case the control hasn't lost focus yet...
		// IMPORTANT: Use a custom flag (instead of isFocused()) because this code depends on
		// onFocusChanged() having ALREADY executed, which may not be the case. So if it hasn't executed
		// treat as if it didn't actually have focus yet.
		if (mHasFocus)
		{
			String editedValue = getText().toString();
			if (!mEditableValue.equals(editedValue))
				internalSetValue(editedValue);
		}

		// As a special case, if the display value is blank (i.e. a picture like "ZZZ.ZZ") then the editable value
		// is also shown as blank. However, it is returned as zero when asked by getValue().
		return (Services.Strings.hasValue(mEditableValue) ? mEditableValue : Strings.ZERO);
	}

	private void internalSetValue(String editableValue)
	{
		if (!Services.Strings.hasValue(editableValue))
			editableValue = Strings.ZERO;

		if (Services.Strings.hasValue(getPicture()))
		{
			BigDecimal value;
			try
			{
				value = new BigDecimal(editableValue);
			}
			catch (NumberFormatException ex)
			{
				value = BigDecimal.ZERO;
			}

			mEditableValue = value.toPlainString();
			mDisplayValue = FormatHelper.formatNumber(value, getPicture());
		}
		else
		{
			mEditableValue = editableValue;
			mDisplayValue = editableValue;
		}

		// As a special case, if the display value is blank (i.e. a picture like "ZZZ.ZZ") then the
		// editable value is also shown as blank. However, it is returned as zero when asked by getValue().
		if (mDisplayValue.length() == 0)
			mEditableValue = Strings.EMPTY;

		// IMPORTANT: Do not use isFocused(). See comment in getGx_Value().
		String text = (mHasFocus ? mEditableValue : mDisplayValue);
		setText(text);
	}

	private String getPicture()
	{
		if (getDefinition() != null && getDefinition().getDataItem() != null)
			return getDefinition().getDataItem().getInputPicture();
		else
			return null;
	}

	@Override
	protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect)
	{
		if (focused)
		{
			// Focused, change display_string to editable_string (calculated from previously set value).
			mHasFocus = true;
			setText(mEditableValue);
			selectAll();
		}
		else
		{
			// Lost focus, read value from editable_string and show display_string.
			mHasFocus = false;
			String editedValue = getText().toString();
			internalSetValue(editedValue);
		}

		super.onFocusChanged(focused, direction, previouslyFocusedRect);
	}
}
