package com.artech.controls;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.ui.Coordinator;

public class GxSwitch extends SwitchCompat implements IGxEdit, OnCheckedChangeListener
{
	private String mCheckedValue = "";
	private String mUncheckedValue = "";
	private Coordinator mCoordinator = null;
	private boolean mFireControlValueChanged;

	public GxSwitch(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public GxSwitch(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public GxSwitch(Context context, Coordinator coordinator, LayoutItemDefinition item)
	{
		super(context);

		mCheckedValue = item.getControlInfo().optStringProperty("@SDSwitchCheckedValue");
		mUncheckedValue = item.getControlInfo().optStringProperty("@SDSwitchUncheckedValue");
		mCoordinator = coordinator;
		mFireControlValueChanged = true;

		setOnCheckedChangeListener(this);
	}

	@Override
	public String getGx_Value()
	{
		return isChecked() ? mCheckedValue : mUncheckedValue;
	}

	@Override
	public void setGx_Value(String value)
	{
		boolean currentState = isChecked();
		boolean newState = value != null && value.equalsIgnoreCase(mCheckedValue);

		if (newState != currentState)
		{
			mFireControlValueChanged = false;
			setChecked(newState);
			mFireControlValueChanged = true;
		}
	}

	@Override
	public String getGx_Tag()
	{
		return (String)this.getTag();
	}

	@Override
	public void setGx_Tag(String data)
	{
		this.setTag(data);
	}

	@Override
	public void setValueFromIntent(Intent data) {}

	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		super.setFocusable(enabled);
	}

	@Override
	public IGxEdit getViewControl()
	{
		setEnabled(false);
		return this;
	}

	@Override
	public IGxEdit getEditControl()
	{
		return this;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		if (mCoordinator != null)
			mCoordinator.onValueChanged(this, mFireControlValueChanged);
	}

	@Override
	public boolean isEditable()
	{
		return isEnabled(); // Editable when enabled.
	}
}
