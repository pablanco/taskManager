package com.artech.activities;

import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;

import com.artech.actions.UIContext;
import com.artech.app.ComponentParameters;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.metadata.enums.DisplayModes;
import com.artech.base.services.Services;
import com.artech.common.IntentHelper;
import com.artech.controllers.DataViewModel;

public class ActivityModel
{
	private String mMainDataViewName;
	private DataViewModel mMain;

	private boolean mInSelectionMode;
	private UIContext mUIContext;

	ActivityModel()
	{
    	mMainDataViewName = "<None>"; //$NON-NLS-1$
	}

	/**
	 * Initializes a new ActivityModel using the activity intent bundle for parameters.
	 * @param bundle Data of the intent received by the activity.
	 */
	boolean initializeFrom(Activity activity, Bundle bundle)
	{
        if (bundle != null)
        {
        	mUIContext = UIContext.base(activity, Connectivity.fromBundle(bundle));

        	mMainDataViewName = bundle.getString(IntentParameters.DataView);
       		short mode = bundle.getShort(IntentParameters.Mode, DisplayModes.VIEW);
	       	List<String> mainParameters = IntentHelper.getList(bundle, IntentParameters.Parameters);
	       	Map<String, String> mainNamedParameters = IntentHelper.getMap(bundle, IntentParameters.BCFieldParameters);
	       	mInSelectionMode = bundle.getBoolean(IntentParameters.IsSelecting);

        	IDataViewDefinition mainDefinition = Services.Application.getDataView(mMainDataViewName);
	       	if (mainDefinition != null)
	       	{
		       	mUIContext = UIContext.base(activity, Connectivity.getConnectivitySupport(bundle, mainDefinition));
		       	ComponentParameters params = new ComponentParameters(mainDefinition, mode, mainParameters, mainNamedParameters);
	    		mMain = createDataView(mUIContext, params);
	    		return true;
	       	}
        }

        return false;
	}

	public UIContext getUIContext()
	{
		return mUIContext;
	}

	public DataViewModel getMain()
	{
		return mMain;
	}

	DataViewModel createDataView(UIContext context, ComponentParameters params)
	{
		if (mMain != null && mMain.getParams().Object == params.Object)
		{
			if (!mMain.getParams().Parameters.equals(params.Parameters))
			{
				mMain = null;
				mMain = createDataView(context, params);
			}

			return mMain;
		}

		return new DataViewModel(params, context.getConnectivitySupport());
	}

	@Override
	public String toString()
	{
		return getName();
	}

	public String getName()
	{
		return mMainDataViewName;
	}

	public boolean getInSelectionMode() { return mInSelectionMode; }
}