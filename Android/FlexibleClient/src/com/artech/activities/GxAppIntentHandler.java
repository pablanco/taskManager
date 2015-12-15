package com.artech.activities;

import android.content.Intent;
import android.net.Uri;

import com.artech.actions.ActionExecution;
import com.artech.actions.DynamicCallAction;
import com.artech.actions.UIContext;
import com.artech.application.MyApplication;
import com.artech.base.model.Entity;
import com.artech.base.utils.Strings;

public class GxAppIntentHandler implements IIntentHandler
{
	private static final String SCHEME = "gxapp";

	@Override
	public boolean tryHandleIntent(UIContext context, Intent intent, Entity entity)
	{
		Uri uri = intent.getData();
		if (uri != null && SCHEME.equalsIgnoreCase(uri.getScheme()) && context.getPackageName().equalsIgnoreCase(uri.getHost()))
		{
			String dynamicCall = createDynamicCall(uri);
			if (dynamicCall != null)
			{
				DynamicCallAction action = DynamicCallAction.redirect(context, entity, dynamicCall);
				if (action != null)
				{
					ActionExecution exec = new ActionExecution(action);
					exec.executeAction(); // Will also finish() the current activity.
					return true;
				}
			}
		}

		return false;
	}

	private static String createDynamicCall(Uri uri)
	{
		// Uri is: gxapp://<package-name>/<object-name>?<parameters>
		// TODO: We should also support parameters in extras, in case they are big.
		String objectName = uri.getPath();
		if (objectName != null && objectName.startsWith("/"))
			objectName = objectName.substring(1);

		if (isValidObject(objectName))
		{
			if (Strings.hasValue(uri.getQuery()))
				return objectName + "?" + uri.getQuery();
			else
				return objectName;
		}
		else
			return null;
	}

	private static boolean isValidObject(String objectName)
	{
		// TODO: Check that the Object is exported!
		return (MyApplication.getApp().getDefinition().getView(objectName) != null);
	}
}
