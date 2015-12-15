package com.artech.controls;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.AppCompatSpinner;
import android.text.Html;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.IValuesFormatter;
import com.artech.controls.common.SpinnerItemsAdapter;
import com.artech.controls.common.StaticValueItems;
import com.artech.ui.Coordinator;

public class SpinnerControl extends AppCompatSpinner implements IGxEdit, OnItemSelectedListener, IGxThemeable
{
	private final Coordinator mCoordinator;
	private final LayoutItemDefinition mDefinition;

	private StaticValueItems mItems;
	private SpinnerItemsAdapter mAdapter;
	private ThemeClassDefinition mThemeClass;

	private boolean mFireControlValueChanged;

	private SpinnerControl(Context context)
	{
		super(context);
		throw new UnsupportedOperationException("Unsupported constructor."); //$NON-NLS-1$
	}

	public SpinnerControl(Context context, Coordinator coordinator, LayoutItemDefinition definition)
	{
		super(context);
		mDefinition = definition;
		mCoordinator = coordinator;
		mThemeClass = mDefinition.getThemeClass();

		// Needed to make sure ListView.setOnClickListener works.
		setFocusable(false);
		setFocusableInTouchMode(false);

		setOnItemSelectedListener(this);
		initValues();
	}

	private void initValues()
	{
		mItems = new StaticValueItems(mDefinition.getControlInfo());
		mAdapter = new SpinnerItemsAdapter(getContext(), mItems, mThemeClass, mDefinition);
		setAdapter(mAdapter);
	}

	@Override
	public String getGx_Value()
	{
		return mItems.getValue(getSelectedItemPosition());
	}

	@Override
	public void setGx_Value(String value)
	{
		// Only make a selection if there are items in the adapter.
		if (getCount() > 0)
		{
			int currentPosition = getSelectedItemPosition();
			int newPosition = mItems.indexOf(value);

			// Reset to the first item if the value was invalid.
			if (newPosition == -1)
				newPosition = 0;

			// Do nothing if there's no change of item position.
			if (newPosition == currentPosition)
				return;

			mFireControlValueChanged = false;
			setSelection(newPosition);
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
	{
		if (mCoordinator != null)
			mCoordinator.onValueChanged(this, mFireControlValueChanged);

		mFireControlValueChanged = true;
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) { }

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

	@Override
	public IGxEdit getViewControl()
	{
		return new GxTextView(getContext(), mCoordinator, mDefinition, new Formatter());
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

	@Override
	public void applyClass(ThemeClassDefinition themeClass)
	{
		if (mAdapter != null)
			mAdapter.applyClass(themeClass);
	}

	private class Formatter implements IValuesFormatter
	{
		@Override
		public CharSequence format(String value)
		{
			if (mDefinition.isHtml())
				return Html.fromHtml(mItems.getDescription(value));
			else
				return mItems.getDescription(value);
		}

		@Override
		public boolean needsAsync()
		{
			return false;
		}
	}

	@Override
	public boolean isEditable()
	{
		return isEnabled(); // Editable when enabled.
	}
}
