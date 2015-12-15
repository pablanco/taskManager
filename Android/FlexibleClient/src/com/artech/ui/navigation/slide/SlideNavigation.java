package com.artech.ui.navigation.slide;

import android.content.Intent;

import com.artech.activities.GenexusActivity;
import com.artech.app.ComponentParameters;
import com.artech.application.MyApplication;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.DashboardMetadata;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.settings.PlatformDefinition;
import com.artech.base.utils.PlatformHelper;
import com.artech.ui.navigation.CallOptionsHelper;
import com.artech.ui.navigation.CallTarget;
import com.artech.ui.navigation.NavigationController;
import com.artech.ui.navigation.NavigationType;

public class SlideNavigation implements NavigationType
{
	static final String INTENT_EXTRA_IS_HUB_CALL = "com.artech.ui.navigation.slide.isHub";

	static final CallTarget TARGET_LEFT = new CallTarget("Left", "Target[1]", "Slide[1]");
	static final CallTarget TARGET_CONTENT = new CallTarget("Center", "Content", "Detail", "Target[2]", "Slide[2]");
	static final CallTarget TARGET_RIGHT = new CallTarget("Right", "Target[3]", "Slide[3]");

	public enum Target
	{
		Left,
		Content,
		Right
	}

	@Override
	public boolean isNavigationFor(IViewDefinition mainView)
	{
		return (PlatformHelper.getNavigationStyle() == PlatformDefinition.NAVIGATION_SLIDE);
	}

	@Override
	public NavigationController createController(GenexusActivity activity, IViewDefinition mainView)
	{
		return new SlideNavigationController(activity);
	}

	static SlideComponents getComponents(Intent intent, ComponentParameters intentParams)
	{
		SlideComponents slide = new SlideComponents();

		if (intentParams.Object == MyApplication.getApp().getMain())
		{
			// Main is always a hub.
			slide.IsHub = true;
			slide.IsLeftMainComponent = true;

			// Call to main component. Show as left drawer.
			slide.set(Target.Left, intentParams);
			slide.PendingAction = getPendingAction(intentParams.Object);
		}
		else
		{
			// Call to another object. Show main on left.
			slide.set(Target.Left, new ComponentParameters(MyApplication.getApp().getMain()));
			slide.set(Target.Content, intentParams);
			slide.IsLeftMainComponent = false;

			if (CallTarget.BLANK.isTarget(CallOptionsHelper.getCurrentCallOptions(intent)))
				slide.set(Target.Left, null); // Don't show main on left if a "new window" target was specified.

			// Is this a call to hub view?
			if (intent != null && intent.getExtras() != null && intent.getBooleanExtra(INTENT_EXTRA_IS_HUB_CALL, false))
				slide.IsHub = true;
		}

		return slide;
	}

	private static ActionDefinition getPendingAction(IViewDefinition intentView)
	{
		ActionDefinition slideStart = intentView.getEvent("Slide.Start");
		if (slideStart != null)
			return slideStart;

		if (intentView instanceof DashboardMetadata)
		{
			// Dashboard -> Try to execute first option as content.
			DashboardMetadata dashboard = (DashboardMetadata)intentView;
			if (dashboard.getItems().size() != 0)
				return dashboard.getItems().get(0).getActionDefinition();
		}
		else if (intentView instanceof IDataViewDefinition)
		{
			// Panel -> Try to execute 'GxStart' as content (for compatibility).
			return intentView.getEvent("GxStart");
		}

		return null;
	}
}
