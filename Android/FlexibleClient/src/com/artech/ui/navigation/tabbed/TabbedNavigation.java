package com.artech.ui.navigation.tabbed;

import java.util.Arrays;

import com.artech.activities.GenexusActivity;
import com.artech.base.metadata.DashboardMetadata;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.settings.PlatformDefinition;
import com.artech.base.utils.PlatformHelper;
import com.artech.ui.navigation.NavigationController;
import com.artech.ui.navigation.NavigationType;

public class TabbedNavigation implements NavigationType
{
	private static final Integer[] COMPATIBLE_NAVIGATION_STYLES = new Integer[] { 
		PlatformDefinition.NAVIGATION_DEFAULT, PlatformDefinition.NAVIGATION_FLIP, PlatformDefinition.NAVIGATION_UNKNOWN };
	
	@Override
	public boolean isNavigationFor(IViewDefinition mainView)
	{
		return (Arrays.asList(COMPATIBLE_NAVIGATION_STYLES).contains(PlatformHelper.getNavigationStyle()) &&
				mainView instanceof DashboardMetadata &&
				((DashboardMetadata)mainView).getControl().equalsIgnoreCase(DashboardMetadata.CONTROL_TABS));
	}

	@Override
	public NavigationController createController(GenexusActivity activity, IViewDefinition mainView)
	{
		return new TabbedNavigationController(activity, (DashboardMetadata)mainView);
	}
}
