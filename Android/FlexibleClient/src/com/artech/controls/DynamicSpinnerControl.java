package com.artech.controls;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.widget.AppCompatSpinner;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

import com.artech.application.MyApplication;
import com.artech.base.controls.IGxControlNotifyEvents;
import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.IValuesFormatter;
import com.artech.base.utils.Strings;
import com.artech.controls.common.ControlServiceDefinition;
import com.artech.controls.common.DynamicValueItems;
import com.artech.controls.common.SpinnerItemsAdapter;
import com.artech.ui.Coordinator;

public class DynamicSpinnerControl extends AppCompatSpinner implements IGxEditWithDependencies, OnItemSelectedListener, IGxThemeable, IGxControlNotifyEvents
{
	private final Coordinator mCoordinator;
	private final DynamicComboDefinition mDefinition;
	private DynamicValueItems mItems;
	private SpinnerItemsAdapter mAdapter;
	private ThemeClassDefinition mThemeClass;

	// Status flags
	private boolean mIsEditControl;
	private boolean mFireControlValueChanged;
	private String mPreviousValue;

	private DynamicSpinnerControl(Context context)
	{
		super(context);
		throw new UnsupportedOperationException("Unsupported constructor."); //$NON-NLS-1$
	}

	public DynamicSpinnerControl(Context context, Coordinator coordinator, LayoutItemDefinition definition)
	{
		super(context);
		mCoordinator = coordinator;
		mDefinition = new DynamicComboDefinition(definition);
		mThemeClass = mDefinition.LayoutItem.getThemeClass();

		// Needed to make sure ListView.setOnClickListener works.
		setFocusable(false);
		setFocusableInTouchMode(false);

		mFireControlValueChanged = false;
		setOnItemSelectedListener(this);
		init();
	}

	@Override
	public String getGx_Value()
	{
		return (mItems != null) ? mItems.getValue(getSelectedItemPosition()) : null;
	}

	@Override
	public void setGx_Value(String value)
	{
		if (mItems == null)
		{
			// Calls setGx_Value(value) again after loading the items.
			loadComboItems(value);
			return;
		}

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
		String value = getGx_Value();
		if (!Strings.areEqual(value, mPreviousValue))
		{
			mPreviousValue = value;

			if (mCoordinator != null)
				mCoordinator.onValueChanged(this, mFireControlValueChanged);
		}

		mFireControlValueChanged = true;
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) { }

	@Override
	public String getGx_Tag() { return getTag().toString(); }

	@Override
	public void setGx_Tag(String tag) { setTag(tag); }

	private void init()
	{
		LayoutParams param = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		setLayoutParams(param);
	}

	@Override
	public void setValueFromIntent(Intent data) { }

	@Override
	public IGxEdit getViewControl()
	{
		return new GxTextView(getContext(), mCoordinator, mDefinition.LayoutItem, new Formatter());
	}

	@Override
	public IGxEdit getEditControl()
	{
		mIsEditControl = true;
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

	@Override
	public List<String> getDependencies()
	{
		return mDefinition.ServiceInput;
	}

	@Override
	public void onDependencyValueChanged(String name, Object value)
	{
		String currentValue = null;
		if (mItems != null)
			currentValue = mItems.getValue(getSelectedItemPosition());

		loadComboItems(currentValue);
	}

	private LoadComboTask taskLoad ;

	private void loadComboItems(String setValueAfter)
	{
		if (mIsEditControl)
		{
			//if (taskLoad!=null && taskLoad.getStatus()!=Status.FINISHED)
			//{
			//	taskLoad.cancel(true);
			//}
			taskLoad = new LoadComboTask(setValueAfter);
			taskLoad.execute();
		}
	}

	private class LoadComboTask extends AsyncTask<Void, Void, DynamicValueItems>
	{
		private Map<String, String> mConditionValues;
		private String mSetValueAfterLoadItems;

		public LoadComboTask(String setValueAfterLoadItems)
		{
			if (setValueAfterLoadItems != null)
				mSetValueAfterLoadItems = setValueAfterLoadItems;
		}

		@Override
		protected void onPreExecute()
		{
			mConditionValues = getConditionValues();
		}

		private LinkedHashMap<String, String> getConditionValues()
		{
			// Get the input values for the service call (for dependent combos).
			LinkedHashMap<String, String> conditionValues = new LinkedHashMap<String, String>();
			for (String inputMember : mDefinition.ServiceInput)
				conditionValues.put(inputMember, mCoordinator.getStringValue(inputMember));

			return conditionValues;
		}

		@Override
		protected DynamicValueItems doInBackground(Void... params)
		{
			// Call remote service to obtain items.
			Connectivity connectivity = mCoordinator.getUIContext().getConnectivitySupport();
			Map<String, String> items = MyApplication.getApplicationServer(connectivity).getDynamicComboValues(mDefinition.Service, mConditionValues);
			return new DynamicValueItems(items);
		}

		@Override
		protected void onPostExecute(DynamicValueItems result)
		{
			mItems = result;
			mAdapter = new SpinnerItemsAdapter(getContext(), result, mThemeClass, mDefinition.LayoutItem);
			setAdapter(mAdapter);

			// If setValue executed before values were available...
			String valueToSet = mSetValueAfterLoadItems;
			if (valueToSet != null)
			{
				mSetValueAfterLoadItems = null;
				setGx_Value(valueToSet);
			}
		}
	}

	private class Formatter implements IValuesFormatter
	{
		private DynamicValueItems mValues;
		private String mCondition;

		@Override
		public boolean needsAsync() { return true; }

		@Override
		public CharSequence format(String value)
		{
			LoadComboTask task = new LoadComboTask(null);
			String condition = task.getConditionValues().toString();

			if (mValues == null || !mCondition.equalsIgnoreCase(condition))
			{
				// Load combo values (this is executed in a background thread so directly calling the
				// AsyncTask implementation is reasonable).
				task.onPreExecute();
				mValues = task.doInBackground();
				mCondition = condition;
			}

			return mValues.getDescription(value);
		}
	}

	private static class DynamicComboDefinition extends ControlServiceDefinition
	{
		public DynamicComboDefinition(LayoutItemDefinition itemDefinition)
		{
			super(itemDefinition, "");
		}
	}

	@Override
	public void notifyEvent(EventType type)
	{
		if (type == EventType.REFRESH && mItems != null)
		{
			String currentValue = mItems.getValue(getSelectedItemPosition());
			loadComboItems(currentValue);
		}
	}

	@Override
	public boolean isEditable()
	{
		return isEnabled(); // Editable when enabled.
	}
}
