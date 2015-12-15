package com.artech.controls.achartengine;

import android.content.Context;
import android.content.Intent;

import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.controls.IGridView;
import com.artech.controls.IGxEdit;
import com.artech.controls.IGxEditControl;

public class GxChartView extends GxChartEngine implements IGridView, IGxEdit, IGxEditControl
{
	private String mTag;

	public GxChartView(Context context, LayoutItemDefinition definition)
	{
		super(context, definition);
		init();
	}

	@Override
	protected void init()
	{
		super.init();
		adapterView();
	}

	@Override
	public void addListener(GridEventsListener listener)
	{
		// Control does not support incremental loading.
		// TODO: Support item click?
	}

	@Override
	public String getGx_Value() {
		return null;
	}

	@Override
	public String getGx_Tag() {
		return mTag;
	}

	@Override
	public void setGx_Tag(String tag) {
		mTag = tag;
	}

	@Override
	public void setValueFromIntent(Intent data) {
	}

	@Override
	public IGxEdit getViewControl() {
		return this;
	}

	@Override
	public IGxEdit getEditControl() {
		return this;
	}

	@Override
	public void setValue(Object value) {
		super.setValue(value);
	}

	@Override
	public Object getValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isEditable()
	{
		return false; // Never editable.
	}
}
