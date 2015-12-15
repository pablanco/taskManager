package com.artech.activities;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.artech.R;
import com.artech.actions.UIContext;
import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.metadata.RelationDefinition;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.metadata.WorkWithDefinition;
import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.metadata.enums.ControlTypes;
import com.artech.base.metadata.enums.DisplayModes;
import com.artech.base.metadata.filter.FilterAttributeDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.FiltersHelper;
import com.artech.common.IntentHelper;
import com.artech.common.PromptHelper;
import com.artech.common.TrnHelper;
import com.artech.controls.GxButton;
import com.artech.controls.GxLinearLayout;
import com.artech.controls.IGxEdit;
import com.artech.utils.SparseArrayEx;

public class DetailFiltersActivity extends GxBaseActivity
{
	// Metadata - Filter attribute begin edited.
	private IDataSourceDefinition mFilteredDataSource;
	private FilterAttributeDefinition mFilterAttribute;
	private StructureDefinition mStructure;
	private boolean mIsRange;

	//UI Fixed Elements
	private LinearLayout mData;
	private GxLinearLayout mLinearLayout;
	private GxButton mOkButton;
	private GxButton mCancelButton;

	private final ArrayList<IGxEdit> mEditables = new ArrayList<IGxEdit>();

	private Entity mEntity;

	private String rangeBegin;
	private String rangeEnd;
	private String filterDefault;
	private String filterRangeFk;

	private final String prefixFrom = "From"; //$NON-NLS-1$
	private final String prefixTo = "To"; //$NON-NLS-1$
	private final String prefixCero = Strings.ZERO;

	private String [] filterRangeFkFrom;
	private String [] filterRangeFkTo;

	private LayoutItemDefinition mFormAttDef;
	private RelationDefinition mPickedRelation = null;

	private int mItemSelected = 0;
	private final Hashtable<String, String> mEditablesFK = new Hashtable<String, String>();
	private Connectivity mConnectivity;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
    	ActivityHelper.onBeforeCreate(this);
		super.onCreate(savedInstanceState);
        ActivityHelper.initialize(this, savedInstanceState);

		if (!Services.Application.isLoaded())
		{
	    	finish();
	    	return;
		}

		setContentView(R.layout.detailfilter);

		// set support toolbar
		Toolbar toolbar = (Toolbar)this.findViewById(R.id.toolbar);
		this.setSupportActionBar(toolbar);

		ActivityHelper.applyStyle(this, null);

		if (!initialize(getIntent()))
		{
			Services.Log.Error("Insufficient information to initialize DetailsFilterActivity."); //$NON-NLS-1$
			finish();
		}
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		ActivityHelper.onNewIntent(this, intent);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		ActivityHelper.onResume(this);
	}

	@Override
	protected void onPause()
	{
		ActivityHelper.onPause(this);
		super.onPause();
	}

	@Override
	protected void onDestroy()
	{
		ActivityHelper.onDestroy(this);
		super.onDestroy();
	}

	private boolean initialize(Intent intent)
	{
		mFilteredDataSource = IntentHelper.getObject(intent, IntentParameters.Filters.DataSource, IDataSourceDefinition.class);
		if (mFilteredDataSource == null)
			return false;

		mConnectivity = Connectivity.fromIntent(intent);

		mStructure = mFilteredDataSource.getStructure();

		String attName = intent.getStringExtra(IntentParameters.AttName);
		mFilterAttribute = mFilteredDataSource.getFilter().getAttribute(attName);
		if (mFilterAttribute == null)
			return false;

		mIsRange = mFilterAttribute.getType().equalsIgnoreCase(FilterAttributeDefinition.TYPE_RANGE);

		//Fixed Controls
		mData = (LinearLayout) findViewById(R.id.LinearLayoutData);

		mLinearLayout = (GxLinearLayout) findViewById(R.id.LinearLayoutDetailfilter);
		mOkButton = (GxButton) findViewById(R.id.formOkButtonDetailFilters);
		mCancelButton = (GxButton) findViewById(R.id.formCancelButtonDetailFilters);

		rangeBegin = intent.getStringExtra(IntentParameters.RangeBegin);
		rangeEnd = intent.getStringExtra(IntentParameters.RangeEnd);

		filterDefault = intent.getStringExtra(IntentParameters.FilterDefault);

		filterRangeFk = intent.getStringExtra(IntentParameters.FilterRangeFk);
		if (filterRangeFk != null) {
			String [] filterRangeFkSplit = Services.Strings.split(filterRangeFk, '&');
			filterRangeFkFrom = Services.Strings.split(filterRangeFkSplit[0], '=');
			filterRangeFkTo = Services.Strings.split(filterRangeFkSplit[1], '=');
		}

		setTitle(getText(R.string.GXM_Filter) + Strings.SPACE + mFilterAttribute.getDescription());
		DataItem attDef = FiltersHelper.getFilterDataItem(mFilteredDataSource, attName);

		mEntity = new Entity(mStructure);
		if (mIsRange)
			createAdapterRange(attDef);
		else
			createAdapter(attDef);

		TrnHelper.setEnumCombosData(mEditables);

		if (mIsRange)
		{
			if (rangeBegin.length() > 0 || rangeEnd.length() > 0)
			{
				setControlValue(attName + prefixFrom, rangeBegin);
				setControlValue(attName + prefixTo, rangeEnd);
			}
			else
			{
				if (filterDefault.length() > 1)
				{
					String [] strSplitFilterDefault = filterDefault.split(Strings.AND);
					setControlValue(attName + prefixFrom, strSplitFilterDefault[0]);
					setControlValue(attName + prefixTo, strSplitFilterDefault[1]);
				} else {
					LayoutItemDefinition formAttDef = FiltersHelper.getFormAttDef(attDef, mStructure);
					if (formAttDef.getControlType().equals(ControlTypes.NumericTextBox) && (mPickedRelation == null))
					{
						setControlValue(attName + prefixFrom, prefixCero);
						setControlValue(attName + prefixTo, prefixCero);
					}
				}
			}
		}
		else
		{
			if (rangeBegin.length() > 0)
				setControlValue(attName, rangeBegin);
			else
			{
				if (filterDefault.length() > 0)
					setControlValue(attName, filterDefault);
				else {
					LayoutItemDefinition formAttDef = FiltersHelper.getFormAttDef(attDef, mStructure);
					if (formAttDef.getControlType().equals(ControlTypes.NumericTextBox))
						setControlValue(attName, prefixCero);
				}
			}
		}

		// Update demo TextViews when the "OK" button is clicked
		findViewById(R.id.formOkButtonDetailFilters ).findViewById(R.id.formOkButtonDetailFilters).setOnClickListener(myOkClickListener);

		// Cancel the dialog when the "Cancel" button is clicked
		findViewById(R.id.formCancelButtonDetailFilters ).findViewById(R.id.formCancelButtonDetailFilters).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent resultIntent = new Intent();
				setResult(RESULT_CANCELED, resultIntent);
				finish();
			}
		});

		FiltersHelper.setButtonAttributes(mOkButton, mCancelButton, R.string.GXM_button_ok, R.string.GXM_cancel);
		FiltersHelper.setThemeFilters(mLinearLayout, null, null, null, null, mOkButton, mCancelButton);

		return true;
	}

	private UIContext getUIContext() {
		return UIContext.base(this, mConnectivity);
	}

	//Click event
	OnClickListener myOkClickListener = new OnClickListener() {
		@Override
		public void onClick(View v)
		{
			String attName = mFilterAttribute.getName();
			String rangeFk = Strings.EMPTY;

			if (mIsRange)
			{
				rangeBegin = getControlValue(attName + prefixFrom);
				rangeEnd = getControlValue(attName + prefixTo);
				if (mPickedRelation != null)
				{
					Vector<String> filterAttRange = new Vector<String>();
					Vector<String> filterAttRangeValue = new Vector<String>();
					filterAttRange.add(attName + prefixFrom);
					filterAttRange.add(attName + prefixTo);
					filterAttRangeValue.add(getControlFkId(attName + prefixFrom));
					filterAttRangeValue.add(getControlFkId(attName + prefixTo));

					rangeFk = FiltersHelper.MakeGetFilterWithValue(filterAttRange, filterAttRangeValue);
				}
			}
			else
				rangeBegin = getControlValue(attName);

			Intent resultIntent = new Intent();
			resultIntent.putExtra(IntentParameters.AttName, attName);
			resultIntent.putExtra(IntentParameters.RangeBegin, rangeBegin);
			resultIntent.putExtra(IntentParameters.RangeEnd, rangeEnd);
			resultIntent.putExtra(IntentParameters.FilterRangeFk, rangeFk);
			setResult(RESULT_OK, resultIntent);
			finish();
		}
	};

	private String getControlFkId(String name)
	{
		for (int index = 0; index< mEditables.size(); index++)
		{
			IGxEdit text = mEditables.get(index);
			if (text.getGx_Tag() != null && mEntity.getProperty(text.getGx_Tag())!=null)
			{
				if (mEntity.getProperty(name) != null) {
					return mEntity.getProperty(name).toString();
				} else {
					if (filterRangeFkFrom != null) {
						if (filterRangeFkFrom[0].equalsIgnoreCase(name))
							return filterRangeFkFrom[1];
						else if (filterRangeFkTo[0].equalsIgnoreCase(name))
							return filterRangeFkTo[1];
					} else {
						return Strings.EMPTY;
					}
				}
			}
		}
		return null;
	}

	private String getControlValue(String name)
	{
		for (int index = 0; index< mEditables.size(); index++)
		{
			IGxEdit text = mEditables.get(index);
			String nameControl = text.getGx_Tag();
			if (name.equalsIgnoreCase(nameControl))
			{
				return  text.getGx_Value();
			}
		}
		return null;
	}

	private void setControlValue(String name, String value)
	{
		//mEntity.setProperty(name, value);
		for (int index = 0; index< mEditables.size(); index++)
		{
			IGxEdit text = mEditables.get(index);
			String nameControl = text.getGx_Tag();
			if (name.equalsIgnoreCase(nameControl))
			{
				text.setGx_Value(value);
			}
		}
	}

	private void createAdapter(DataItem attDef) {
		LayoutItemDefinition formAttDef = FiltersHelper.getFormAttDef(attDef, mStructure);
		boolean readOnly = formAttDef.getDataItem().getReadOnly();
		formAttDef.getDataItem().setProperty("ReadOnly", String.valueOf(false)); //$NON-NLS-1$
		LinearLayout layout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.itemeditform , null);
		TrnHelper.createEditRow(this, null, layout, mEntity, getImageLoader(), formAttDef, mEditables, null);
		setLabelText(layout);
		formAttDef.getDataItem().setProperty("ReadOnly", String.valueOf(readOnly)); //$NON-NLS-1$
		mItems.put(0, layout);
		addViews(mData);
	}

	private void createAdapterRange(DataItem attDef) {
		//mFormAttDef = FiltersHelper.getFormAttDef(attDef, mStructure);
		mFormAttDef = FiltersHelper.getFormAttDef(attDef, mFilteredDataSource.getPattern().getBusinessComponent());

		LinearLayout layout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.itemeditform , null);
		String formAttDefName = mFormAttDef.getDataId();

		boolean readOnly = mFormAttDef.getDataItem().getReadOnly();
		boolean isKey = mFormAttDef.getDataItem().isKey();
		mPickedRelation = mFormAttDef.getFK();
		if (mPickedRelation == null)
		{
			mFormAttDef.getDataItem().setProperty("ReadOnly", String.valueOf(false)); //$NON-NLS-1$
			mFormAttDef.getDataItem().setProperty("IsKey", String.valueOf(false)); //$NON-NLS-1$

			createEditRowRange(layout, formAttDefName, null, null);
		} else {
			mFormAttDef.getDataItem().setProperty("ReadOnly", String.valueOf(true)); //$NON-NLS-1$
			mFormAttDef.getDataItem().setProperty("IsKey", String.valueOf(true)); //$NON-NLS-1$

			createEditRowRange(layout, formAttDefName, mRelationActionHandlerFrom, mRelationActionHandlerTo);
		}
		mFormAttDef.getDataItem().setProperty("ReadOnly", String.valueOf(readOnly)); //$NON-NLS-1$
		mFormAttDef.getDataItem().setProperty("IsKey", String.valueOf(isKey)); //$NON-NLS-1$

		mItems.put(0, layout);
		addViews(mData);
	}

	private void createEditRowRange(LinearLayout layout, String formAttDefName, OnClickListener relationActionHandlerFrom, OnClickListener relationActionHandlerTo)
	{
		String fromCaption = Services.Strings.getResource(R.string.GXM_FilterRangeFrom, Strings.EMPTY);
		TrnHelper.createEditRowRange(this, null, layout,DisplayModes.INSERT, mEntity, getImageLoader(), mFormAttDef, mEditables, relationActionHandlerFrom, fromCaption);
		setNameLayoutItem(0, formAttDefName + prefixFrom);

		String toCaption = Services.Strings.getResource(R.string.GXM_FilterRangeTo, Strings.EMPTY);
		TrnHelper.createEditRowRange(this, null, layout,DisplayModes.INSERT, mEntity, getImageLoader(), mFormAttDef, mEditables, relationActionHandlerTo, toCaption);
		setNameLayoutItem(1, formAttDefName + prefixTo);
	}

	private final OnClickListener mRelationActionHandlerFrom = new OnClickListener()
	{
		@Override
		public void onClick(View view)
		{
			mItemSelected = 0;
			callFKforView(view);
		}
	};

	private final OnClickListener mRelationActionHandlerTo = new OnClickListener()
	{
		@Override
		public void onClick(View view)
		{
			mItemSelected = 1;
			callFKforView(view);
		}
	};

	private void callFKforView(View v)
	{
		if (mPickedRelation != null)
			PromptHelper.callPrompt(getUIContext(), mPickedRelation);
	}

	private void setNameLayoutItem(int index, String name)
	{
		IGxEdit text = mEditables.get(index);
		text.setGx_Tag(name);
	}

	private final SparseArrayEx<View> mItems = new SparseArrayEx<View>();

	private void addViews(LinearLayout parent)
	{
		for (View v : mItems.values())
			parent.addView(v);
	}

	private void setLabelText(LinearLayout layout) {
		int count = layout.getChildCount();
		for (int i = 0; i < count; i++) {
			View child = layout.getChildAt(i);
			if (child instanceof TextView) {
				try {
					((TextView) child).setText(mFilterAttribute.getDescription());
				break;
				} catch (Exception e) {}
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK && requestCode == FiltersHelper.SELECT_FK) {
			if (data!=null) {
				String metaBCString = data.getStringExtra("MetaBCName"); //$NON-NLS-1$
				WorkWithDefinition meta = Services.Application.getWorkWithForBC(metaBCString);
				if (meta != null) {
					List<DataItem> keysList = meta.getBusinessComponent().Root.GetKeys();
					String attName = Strings.EMPTY;

					for(int i = 0; i< keysList.size(); i++)
					{
						DataItem att = keysList.get(i);
						attName = FiltersHelper.calculateAttName(att.getName(), mPickedRelation);
						if (mItemSelected == 0)
							SetValue(attName + prefixFrom, data.getStringExtra(att.getName()));
						else {
							if (mItemSelected == 1)
								SetValue(attName + prefixTo, data.getStringExtra(att.getName()));
						}
					}

					if (meta.getBusinessComponent().Root.getDescriptionAttribute()!=null)
					{
						DataItem att = meta.getBusinessComponent().Root.getDescriptionAttribute();
						if (mItemSelected == 0)
							SetFKValue(attName + prefixFrom, data.getStringExtra(att.getName()));
						else {
							if (mItemSelected == 1)
								SetFKValue(attName + prefixTo, data.getStringExtra(att.getName()));
						}
					}
					DataToControls(mEntity);
				}
			}
		}
	}

	private void SetValue(String name, String value) {
		mEntity.setProperty(name, value);
	}

	private void SetFKValue(String name, String id) {
		mEditablesFK.put(name, id);
	}

	private void DataToControls(Entity entity)
	{
		if (entity == null) return;
		for (int index = 0; index< mEditables.size(); index++)
		{
			IGxEdit text = mEditables.get(index);
			if (text.getGx_Tag() != null && entity.getProperty(text.getGx_Tag())!=null)
			{
				text.setGx_Value(mEditablesFK.get(text.getGx_Tag()));
			}
		}
	}
}
