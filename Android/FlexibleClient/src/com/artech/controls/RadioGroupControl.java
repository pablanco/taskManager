package com.artech.controls;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.AppCompatRadioButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.controls.common.StaticValueItems;
import com.artech.controls.common.ValueItem;
import com.artech.controls.utils.TextViewUtils;
import com.artech.ui.Coordinator;
import com.artech.utils.ThemeUtils;

import java.util.Vector;

public class RadioGroupControl extends android.widget.RadioGroup implements IGxEdit, IGxThemeable, OnCheckedChangeListener
{
	private StaticValueItems mItems;
	private Vector<RadioButton> mVectorRadioButton;
	private ThemeClassDefinition mThemeClass;
	private Coordinator mCoordinator;
	private boolean mFireControlValueChanged;

	public RadioGroupControl(Context context, Coordinator coordinator, LayoutItemDefinition def)
	{
		super(context);
		mVectorRadioButton = new Vector<RadioButton>();
		mCoordinator = coordinator;
		mFireControlValueChanged = true;

		setOnCheckedChangeListener(this);

		setLayoutDefinition(def);
		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
	}

	public RadioGroupControl(Context context)
	{
		super(context);
		throw new UnsupportedOperationException("Unsupported constructor."); //$NON-NLS-1$
	}

	@Override
	public String getGx_Value()
	{
		int index = getCheckedRadioButtonId();
		if (index >= 0 && index < mVectorRadioButton.size())
		{
			RadioButton button = mVectorRadioButton.get(index);
			return (String) button.getTag();
		}
		else
			return Strings.EMPTY;
	}

	@Override
	public void setGx_Value(String value) {
		RadioButton button = (RadioButton) findViewWithTag(value);
		if (button != null) {
			mFireControlValueChanged = false;
			super.check(button.getId());
			mFireControlValueChanged = true;
		}
	}

	@Override
	public String getGx_Tag()
	{
		return getTag().toString();
	}

	@Override
	public void setGx_Tag(String tag)
	{
		setTag(tag);
	}

	@Override
	public void setValueFromIntent(Intent data) { }

	private void setOrientationFrom(ControlInfo info)
	{
		setOrientation(HORIZONTAL); // Default value.

		if (info != null)
		{
			String orientation = info.optStringProperty("@ControlDirection"); //$NON-NLS-1$
			if (Services.Strings.hasValue(orientation))
			{
				if (orientation.equalsIgnoreCase("Vertical")) //$NON-NLS-1$
					setOrientation(VERTICAL);
				else if (orientation.equalsIgnoreCase("Horizontal")) //$NON-NLS-1$
					setOrientation(HORIZONTAL);
			}
		}
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		for (RadioButton radItem : mVectorRadioButton)
		{
			removeView(radItem); // Why this?
			radItem.setEnabled(enabled);
			addView(radItem);
		}
	}

	@Override
	public IGxEdit getViewControl()
	{
		setFocusable(false);
		setEnabled(false);
		return this;
	}

	@Override
	public IGxEdit getEditControl()
	{
		return this;
	}

	@Override
	public void setThemeClass(ThemeClassDefinition themeClass)
	{
		mThemeClass = themeClass;
		applyClass(themeClass);
	}

	@Override
	public ThemeClassDefinition getThemeClass()
	{
		return mThemeClass;
	}

	private void setLayoutDefinition(LayoutItemDefinition definition)
	{
		mItems = new StaticValueItems(definition.getControlInfo());

		super.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		setOrientationFrom(definition.getControlInfo());

		for (int i = 0; i < mItems.size(); i++)
		{
			ValueItem item = mItems.get(i);

			RadioButton radioItem = new AppCompatRadioButton(getContext());
			radioItem.setLayoutParams(new RadioGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			ThemeUtils.setFontProperties(radioItem, mThemeClass);

			radioItem.setId(i);
			radioItem.setTag(item.Value);
			TextViewUtils.setText(radioItem, item.Description, definition);

			mVectorRadioButton.add(radioItem);
			addView(radioItem);
		}
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId)
	{
		if (mCoordinator != null)
			mCoordinator.onValueChanged(this, mFireControlValueChanged);
	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass)
	{
		for (RadioButton item : mVectorRadioButton)
			ThemeUtils.setFontProperties(item, themeClass);
	}

	@Override
	public boolean isEditable()
	{
		return isEnabled(); // Editable when enabled.
	}
}
