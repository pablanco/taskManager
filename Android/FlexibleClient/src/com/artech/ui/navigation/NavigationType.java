package com.artech.ui.navigation;

import com.artech.activities.GenexusActivity;
import com.artech.base.metadata.IViewDefinition;

public interface NavigationType
{
	boolean isNavigationFor(IViewDefinition mainView);
	NavigationController createController(GenexusActivity activity, IViewDefinition mainView);
}
