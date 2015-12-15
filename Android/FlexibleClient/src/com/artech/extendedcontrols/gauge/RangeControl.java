package com.artech.extendedcontrols.gauge;

import android.content.Context;
import android.content.Intent;

import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.controls.IGxEdit;
import com.artech.controls.IGxThemeable;

public class RangeControl extends ChartSurface implements IGxEdit, IGxThemeable 
{

	private String mTag;

	public RangeControl(Context context, LayoutItemDefinition def) 
	{
		super(context);
	}


	@Override
	public String getGx_Value() 
	{
		return null;
	}

	@Override
	public void setGx_Value(String value) 
	{
		GaugeSpecification spec = new GaugeSpecification();
		spec.deserialize(value);
		setSpec(spec);
		this.postInvalidate();

	}

	@Override
	public String getGx_Tag() 
	{
		return mTag;
	}

	@Override
	public void setGx_Tag(String tag) 
	{
		mTag = tag;
	}

	@Override
	public void setValueFromIntent(Intent data) { }

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
	public boolean isEditable()
	{
		return false; // Never editable.
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


	@Override
	public void applyClass(ThemeClassDefinition themeClass) 
	{
		///force draw of ChartSurface
		this.postInvalidate();
		
	}
}