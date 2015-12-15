package com.artech.activities;

import android.app.Activity;
import android.content.Intent;

import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.services.Services;
import com.artech.fragments.IDataView;

public class DataViewHelper
{
	private final IDataViewDefinition mView;

	public static DataViewHelper fromIntent(Intent intent)
	{
		String dataViewId = intent.getStringExtra(IntentParameters.DataView);

		if (!Services.Strings.hasValue(dataViewId))
			throw new IllegalArgumentException("Detail Intent was not properly set up -- DataView name is missing."); //$NON-NLS-1$

		IDataViewDefinition dataView = Services.Application.getDataView(dataViewId);
		if (dataView == null)
        	throw new IllegalArgumentException(String.format("Data View with name '%s' does not exist.", dataViewId)); //$NON-NLS-1$

        return new DataViewHelper(dataView);
	}

	public DataViewHelper(IDataViewDefinition view)
	{
		mView = view;
	}

	public IDataViewDefinition getDefinition()
	{
		return mView;
	}
	
	public static void setTitle(Activity activity, IDataView fromDataView, CharSequence title)
	{
		if (activity == null)
			return;

		if (activity instanceof GenexusActivity)
		{
			((GenexusActivity)activity).setTitle(title, fromDataView);
		}
		else
			activity.setTitle(title);
	}
}
