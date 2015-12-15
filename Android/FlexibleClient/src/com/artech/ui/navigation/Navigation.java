package com.artech.ui.navigation;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;

import com.artech.activities.GenexusActivity;
import com.artech.base.metadata.IViewDefinition;
import com.artech.ui.navigation.controllers.StandardNavigationController;

public class Navigation
{
	private static ArrayList<NavigationType> sNavigationTypes;

	static
	{
		// Initialize known navigation controllers.
		sNavigationTypes = new ArrayList<NavigationType>();
		sNavigationTypes.add(new com.artech.ui.navigation.slide.SlideNavigation());
		sNavigationTypes.add(new com.artech.ui.navigation.tabbed.TabbedNavigation());
	}

	public static void addNavigationType(NavigationType type)
	{
		sNavigationTypes.add(0, type);
	}

	public static NavigationController createController(GenexusActivity activity, IViewDefinition view)
	{
		for (NavigationType navigation : sNavigationTypes)
		{
			if (navigation.isNavigationFor(view))
				return navigation.createController(activity, view);
		}

		return new StandardNavigationController(activity);
	}

	public static NavigationHandled handle(UIObjectCall call, Intent callIntent)
	{
		Activity hostActivity = call.getContext().getActivity();
		if (hostActivity instanceof INavigationActivity)
		{
			NavigationController controller = ((INavigationActivity)hostActivity).getNavigationController();
			if (controller != null)
				return controller.handle(call, callIntent);
		}

		return NavigationHandled.NOT_HANDLED;
	}
}
