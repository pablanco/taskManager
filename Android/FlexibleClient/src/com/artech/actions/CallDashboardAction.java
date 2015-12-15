package com.artech.actions;

import java.util.Collections;
import java.util.List;

import android.content.Intent;

import com.artech.activities.ActivityLauncher;
import com.artech.app.ComponentParameters;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.DashboardMetadata;
import com.artech.base.metadata.IPatternMetadata;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.enums.DisplayModes;
import com.artech.base.metadata.enums.RequestCodes;
import com.artech.base.services.Services;
import com.artech.ui.navigation.Navigation;
import com.artech.ui.navigation.NavigationHandled;
import com.artech.ui.navigation.UIObjectCall;
import com.artech.utils.Cast;

public class CallDashboardAction extends Action
{
	private boolean mWaitForResult;

	public CallDashboardAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		super(context, definition, parameters);
		mWaitForResult = true;
	}

	@Override
	public boolean Do()
	{
		mWaitForResult = true;

		Intent intent = getIntentForAction();
		ComponentParameters params = new ComponentParameters(getObject(), getMode(), getObjectParameters());
		UIObjectCall call = new UIObjectCall(getContext(), params);

		NavigationHandled handled = Navigation.handle(call, intent);
		if (handled != NavigationHandled.NOT_HANDLED)
		{
			mWaitForResult = (handled == NavigationHandled.HANDLED_WAIT_FOR_RESULT);
			return true;
		}

		getActivity().startActivityForResult(intent, RequestCodes.ACTION);
		return true;
	}

	private Intent getIntentForAction()
	{
		return ActivityLauncher.getDashboard(getContext(), getDefinition().getGxObject());
	}

	@Override
	public boolean catchOnActivityResult() { return mWaitForResult; }

	private IViewDefinition getObject()
	{
		IPatternMetadata obj = Services.Application.getPattern(getDefinition().getGxObject());
		return Cast.as(DashboardMetadata.class, obj);
	}

	private List<String> getObjectParameters()
	{
		// Dashboards do not take arguments, for now.
		return Collections.emptyList();
	}

	private short getMode()
	{
		return DisplayModes.VIEW;
	}
}
