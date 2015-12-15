package com.artech.common;

import java.util.Vector;

import com.artech.actions.Action;
import com.artech.application.MyApplication;
import com.artech.base.metadata.ActionParameter;
import com.artech.base.services.ServiceResponse;
import com.artech.base.utils.Strings;

public class ActionsHelper
{
	public static ServiceResponse runLoginAction(Action loginAction)
	{
		Vector<String> values = new Vector<String>();
		for (ActionParameter parameter : loginAction.getDefinition().getParameters())
		{
			Object value = loginAction.getParameterValue(parameter);
			values.add(value != null ? value.toString() : Strings.EMPTY);
		}

		String userName = Strings.EMPTY;
		String userPassword = Strings.EMPTY;
		if (values.size()>1)
		{
			userName = values.elementAt(0);
			userPassword = values.elementAt(1);
		}

		return ServiceHelper.StringToPostSecurity(
				MyApplication.getApp().UriMaker.getLoginUrl(),
				SecurityHelper.getOauthParameters(SecurityHelper.TYPE_STANDARD, userName, userPassword, null));
	}

	public static ServiceResponse runLoginExternalAction(Action loginAction)
	{
		Vector<String> values = new Vector<String>();
		for (ActionParameter parameter : loginAction.getDefinition().getParameters())
		{
			Object value = loginAction.getParameterValue(parameter);
			values.add(value != null ? value.toString() : Strings.EMPTY);
		}

		String type = Strings.EMPTY;
		String userName = Strings.EMPTY;
		String userPassword = Strings.EMPTY;
		if (values.size()>0)
		{
			type = Strings.toLowerCase(values.elementAt(0));
		}
		if (values.size()>2)
		{
			userName = values.elementAt(1);
			userPassword = values.elementAt(2);
		}

		return ServiceHelper.StringToPostSecurity(
				MyApplication.getApp().UriMaker.getLoginUrl(),
				SecurityHelper.getOauthParameters(type, userName, userPassword, null));
	}
}
