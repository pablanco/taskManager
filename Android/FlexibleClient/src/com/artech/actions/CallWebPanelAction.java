package com.artech.actions;

import java.util.ArrayList;

import android.content.Intent;

import com.artech.activities.ActivityLauncher;
import com.artech.app.ComponentParameters;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.ActionParameter;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.ui.navigation.Navigation;
import com.artech.ui.navigation.NavigationHandled;
import com.artech.ui.navigation.UIObjectCall;

class CallWebPanelAction extends Action
{
	private boolean mWaitForResult;
	
	public CallWebPanelAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		super(context, definition, parameters);
	}

	@Override
	public boolean Do()
	{
		mWaitForResult = true;
		String webUrl = getUrl();

		Intent intent = ActivityLauncher.GetComponent(getContext(), webUrl, true);
		UIObjectCall call = new UIObjectCall(getContext(), new ComponentParameters(webUrl));

		NavigationHandled handled = Navigation.handle(call, intent);
		if (handled != NavigationHandled.NOT_HANDLED)
		{
			mWaitForResult = (handled == NavigationHandled.HANDLED_WAIT_FOR_RESULT);
			return true;
		}
	
		getContext().startActivity(intent);
		return true;
	}

	@Override
	public boolean catchOnActivityResult()
	{
		return mWaitForResult;
	}
	
	private String getUrl()
	{
		ArrayList<String> urlParameters = new ArrayList<String>();
		for (ActionParameter parameter : getDefinition().getParameters())
		{
			Object value = getParameterValue(parameter);
			if (value != null)
				urlParameters.add(Services.HttpService.UriEncode(value.toString()));
		}

		String link = Services.Application.link(getDefinition().getGxObject());
		if (urlParameters.size() > 0)
			link += Strings.QUESTION + Services.Strings.join(urlParameters, Strings.COMMA);
		
		return link;
	}
}