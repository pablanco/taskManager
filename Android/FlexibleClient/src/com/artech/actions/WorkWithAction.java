package com.artech.actions;

import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;

import com.artech.activities.ActivityLauncher;
import com.artech.activities.IntentParameters;
import com.artech.app.ComponentParameters;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.ActionParameter;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.ObjectParameterDefinition;
import com.artech.base.metadata.WorkWithDefinition;
import com.artech.base.metadata.enums.DisplayModes;
import com.artech.base.metadata.enums.RequestCodes;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.IntentHelper;
import com.artech.ui.navigation.CallOptionsHelper;
import com.artech.ui.navigation.CallType;
import com.artech.ui.navigation.Navigation;
import com.artech.ui.navigation.NavigationHandled;
import com.artech.ui.navigation.UIObjectCall;

public class WorkWithAction extends Action
{
	private final String mPattern;
	private final String mComponent;
	private final String mBCVariableName;

	private boolean mWaitForResult;
	private boolean mIsReplace;

	public WorkWithAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		super(context, definition, parameters);

		mPattern = getDefinition().getGxObject();
		mComponent = getDefinition().optStringProperty("@instanceComponent"); //$NON-NLS-1$
		mBCVariableName = getDefinition().optStringProperty("@bcVariable"); //$NON-NLS-1$

		mWaitForResult = true;
	}

	@Override
	public boolean Do()
	{
		mWaitForResult = true;
		mIsReplace = false;

		Intent intent = getIntentForAction();
		ComponentParameters params = new ComponentParameters(getObject(), getMode(), getObjectParameters(), getFieldParameters());
		UIObjectCall call = new UIObjectCall(getContext(), params);

		NavigationHandled handled = Navigation.handle(call, intent);
		if (handled != NavigationHandled.NOT_HANDLED)
		{
			mWaitForResult = (handled == NavigationHandled.HANDLED_WAIT_FOR_RESULT);
			return true;
		}

		if (intent == null)
		{
			Services.Log.Error("WW Action Intent null"); //$NON-NLS-1$
			return false;
		}

		mIsReplace = (CallOptionsHelper.getCallOptions(call.getObject(), call.getMode()).getCallType() == CallType.REPLACE);
		ActivityLauncher.startActivityForResult(getActivity(), intent, RequestCodes.ACTION);
		return true;
	}

	private Intent getIntentForAction()
	{
		IViewDefinition dataView = getObject();
		if (dataView == null)
		{
			Services.Log.Error("WW Action Intent null: " + "DataView not found " + mPattern); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}

		short mode = getMode();
		Map<String, String> fieldParameters = null;
		if (mode != DisplayModes.VIEW)
			fieldParameters = getFieldParameters();

		List<String> parameters = getObjectParameters();

 		return ActivityLauncher.getIntent(getContext(), dataView, parameters, mode, fieldParameters);
	}

	IViewDefinition getObject()
	{
		WorkWithDefinition workwith = (WorkWithDefinition)Services.Application.getPattern(mPattern);
		if (workwith == null || workwith.getLevels().size() == 0)
			return null;

		// Find the data view inside the instance using the "component" property.
		IDataViewDefinition dataView = null;
		for (IDataViewDefinition wwDataView : workwith.getDataViews())
		{
			if (!Services.Strings.hasValue(mComponent) || Strings.toLowerCase(wwDataView.getName()).endsWith(Strings.toLowerCase(mComponent)))
			{
				dataView = wwDataView;
				break;
			}
 		}

		// Use default it not found (or not specified).
		if (dataView == null)
			dataView = workwith.getLevel(0).getList();

		return dataView;
	}

	private List<String> getObjectParameters()
	{
		return ActionParametersHelper.getParametersForDataView(this);
	}

	private Map<String, String> getFieldParameters()
	{
		return ActionParametersHelper.getParametersForBC(this);
	}

	private short getMode()
	{
		if (getDefinition().optStringProperty("@bcMode").equalsIgnoreCase("Delete")) //$NON-NLS-1$ //$NON-NLS-2$
			return DisplayModes.DELETE;
		else if (getDefinition().optStringProperty("@bcMode").equalsIgnoreCase("Update")) //$NON-NLS-1$ //$NON-NLS-2$
			return DisplayModes.EDIT;
		else if (getDefinition().optStringProperty("@bcMode").equalsIgnoreCase("Insert")) //$NON-NLS-1$ //$NON-NLS-2$
			return DisplayModes.INSERT;
		else if (getDefinition().optStringProperty("@bcMode").equalsIgnoreCase("Edit")) //$NON-NLS-1$ //$NON-NLS-2$
			return DisplayModes.EDIT;
		else // Default Mode
			return DisplayModes.VIEW;
	}

	@Override
	public boolean catchOnActivityResult()
	{
		return mWaitForResult;
	}

	@Override
	public boolean isActivityEnding()
	{
		return mIsReplace;
	}

	public final static String UPDATED_ENTITY_IN_INTENT = "UpdatedEntityInIntent";

	@Override
	public ActionResult afterActivityResult(int requestCode, int resultCode, Intent result)
	{
		boolean updatedData = false;

		if (resultCode != Activity.RESULT_OK)
			return ActionResult.SUCCESS_CONTINUE;

		// Get the entity inserted/updated/deleted and put it in the variable name if specified.
		Entity updatedEntity = IntentHelper.getObject(result, UPDATED_ENTITY_IN_INTENT, Entity.class);
		if (updatedEntity != null && Services.Strings.hasValue(mBCVariableName))
		{
			setOutputValue(mBCVariableName, updatedEntity);
			updatedData = true;
		}

		// 	Read any output parameters.
		List<Object> parameterValues = IntentHelper.getList(result, IntentParameters.Parameters);
		if (parameterValues != null && getObject() != null)
		{
			List<ObjectParameterDefinition> parameterDefinitions = getObject().getParameters();
			for (int i = 0; i < parameterDefinitions.size(); i++)
			{
				ActionParameter callParameter = getDefinition().getParameter(i);
				ObjectParameterDefinition objectParameter = parameterDefinitions.get(i);
				// Read and assign if:
				// 1) the parameter is defined as output in the called object, and
				// 2) it was called with an assignable expression (i.e. a variable), and
				// 3) we actually have a value for that position.
				if (callParameter==null)
				{
					Services.Log.Error("afterActivityResult callParameter is null. WWAction"); //$NON-NLS-1$
				}
				if (callParameter!=null && callParameter.isAssignable() && objectParameter.isOutput() && parameterValues.size() > i)
				{
					Object parameterValue = parameterValues.get(i);
					if (parameterValue != null)
					{
						setOutputValue(callParameter, parameterValue);
						updatedData = true;
					}
				}
			}
		}

		return (updatedData ? ActionResult.SUCCESS_CONTINUE_NO_REFRESH : ActionResult.SUCCESS_CONTINUE);
	}
}
