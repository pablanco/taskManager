package com.artech.activities;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import android.content.Intent;

import com.artech.actions.UIContext;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;

public class IntentHandlers
{
	private static final ArrayList<IIntentHandler> sHandlers = new ArrayList<IIntentHandler>();

    public static void addHandler(IIntentHandler handler)
    {
    	sHandlers.add(handler);
    }

	public static void addHandler(String className)
	{
		try
		{
			// Create an instance via Reflection, if possible.
			Class<?> clazz = Class.forName(className);
			Constructor<?> constructor = clazz.getConstructor();
			sHandlers.add((IIntentHandler)constructor.newInstance());
		}
		catch (Exception e)
		{
          	Services.Log.warning(String.format("Intent Handler with class name '%s' was not found.", className), e); //$NON-NLS-1$
 		}
	}

	public static boolean tryHandleIntent(UIContext context, Intent intent, Entity entity)
	{
		for (IIntentHandler intentHandler : sHandlers)
		{
			if (intentHandler.tryHandleIntent(context, intent, entity))
				return true;
		}

		return false;
	}
}