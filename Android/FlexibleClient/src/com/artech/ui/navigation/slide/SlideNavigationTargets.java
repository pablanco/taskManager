package com.artech.ui.navigation.slide;

import java.util.HashMap;

import android.app.Activity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;

import com.artech.R;
import com.artech.adapters.AdaptersHelper;
import com.artech.app.ComponentId;
import com.artech.app.ComponentParameters;
import com.artech.base.metadata.ILayoutDefinition;
import com.artech.base.metadata.layout.Size;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.fragments.IDataView;
import com.artech.fragments.LayoutFragmentActivityState;
import com.artech.ui.navigation.UIObjectCall;
import com.artech.ui.navigation.slide.SlideNavigation.Target;

class SlideNavigationTargets
{
	private final DrawerLayout mDrawerLayout;
	private HashMap<Target, IDataView> mFragments;

	private Size mDrawerSize;
	private Size mContentSize;

	private static final String STATE_DRAWER_OPEN_FORMAT = "Gx::SlideNavigation::IsDrawerOpen::%s";

	public SlideNavigationTargets(DrawerLayout drawerLayout)
	{
		mDrawerLayout = drawerLayout;
		mFragments = new HashMap<Target, IDataView>();
	}

	public void start(Activity activity, ComponentParameters mainParams)
	{
		// Calculate drawer and content sizes. These are fixed here because later on the R.id.content view
		// may change size if the keyboard is opened and we need the "original" values to measure.
		// Use the "main" component for this (it decides whether the action bar is visible or not).
		ILayoutDefinition mainLayout = UIObjectCall.getLayoutFromViewDefinition(mainParams.Object, mainParams.Mode);
		Size windowSize = AdaptersHelper.getWindowSize(activity, mainLayout);

		int drawerWidth = (int)activity.getResources().getDimension(R.dimen.drawer_width);
		mDrawerSize = new Size(drawerWidth, windowSize.getHeight());
		mContentSize = windowSize;
	}

	public static int getTargetViewId(Target target)
	{
		switch (target)
		{
			case Left :	return R.id.left_drawer;
			case Content : return R.id.content_frame;
			case Right : return R.id.right_drawer;
			default : throw new IllegalArgumentException(String.format("Invalid target: '%s'.", target));
		}
	}

	public static ComponentId getComponentId(Target target)
	{
		switch (target)
		{
			case Left :
			case Content :
			case Right :
				return new ComponentId(null, "[SlideNavigation]::" + target.toString());

			default :
				throw new IllegalArgumentException(String.format("Invalid target: '%s'.", target));
		}
	}

	public static int getGravity(Target target)
	{
		switch (target)
		{
			case Left :
				return GravityCompat.START;

			case Right :
				return GravityCompat.END;

			default :
				throw new IllegalArgumentException("Target '%s' does not have an associated gravity.");
		}
	}

	public Size getSize(Activity activity, Target target, ComponentParameters params)
	{
		ILayoutDefinition layout = UIObjectCall.getLayoutFromViewDefinition(params.Object, params.Mode);
		if (target == Target.Left || target == Target.Right)
		{
			int drawerWidth = (int)activity.getResources().getDimension(R.dimen.drawer_width);
			int drawerHeight = AdaptersHelper.getDisplayHeight(activity, layout);
			if (layout != null && layout.getEnableHeaderRowPattern())
			{
				// TODO: when use EnableHeaderRowPattern in slide menu, substract statusbar
				// and remove setFitSystemWindows, like 4.x does?
				//drawerHeight -= AdaptersHelper.getStatusBarHeight(activity);
			}
			else
			{
				drawerHeight += AdaptersHelper.getStatusAndActionBarHeight(activity, layout);
				if (!CompatibilityHelper.isStatusBarOverlayingAvailable())
					drawerHeight -= AdaptersHelper.getStatusBarHeight(activity);;

			}
			mDrawerSize = new Size(drawerWidth, drawerHeight);
			return mDrawerSize;
		}
		else
		{
			int sizeWidth = AdaptersHelper.getDisplayWidth(activity);
			int sizeHeight = AdaptersHelper.getDisplayHeight(activity, layout);
			mContentSize =  new Size(sizeWidth, sizeHeight);
			return mContentSize;
		}
	}

	public void putFragment(Target target, IDataView fragment)
	{
		mFragments.put(target, fragment);
	}

	public IDataView getFragment(Target target)
	{
		return mFragments.get(target);
	}

	public void saveStateTo(LayoutFragmentActivityState outState)
	{
		saveDrawerStateTo(Target.Left, outState);
		saveDrawerStateTo(Target.Right, outState);
	}

	private void saveDrawerStateTo(Target target, LayoutFragmentActivityState outState)
	{
		String key = String.format(STATE_DRAWER_OPEN_FORMAT, target);
		boolean value = mDrawerLayout.isDrawerOpen(getGravity(target));
		outState.setProperty(key, value);
	}

	public void restoreStateFrom(LayoutFragmentActivityState inState)
	{
		boolean wasDrawerOpenLeft = restoreDrawerStateFrom(Target.Left, inState);
		boolean wasDrawerOpenRight = restoreDrawerStateFrom(Target.Right, inState);
		// set content active to true if drawer is closed
		if (!wasDrawerOpenLeft && !wasDrawerOpenRight && inState!=null)
		{
			IDataView mContentFragment = mFragments.get(Target.Content);
			if (mContentFragment != null)
				mContentFragment.setActive(true);
		}
	}

	private boolean restoreDrawerStateFrom(Target target, LayoutFragmentActivityState inState)
	{
		IDataView targetDataView = mFragments.get(target);
		if (targetDataView != null)
		{
			if (inState != null)
			{
				String key = String.format(STATE_DRAWER_OPEN_FORMAT, target);
				boolean wasDrawerOpen = inState.getBooleanProperty(key, false);
				targetDataView.setActive(wasDrawerOpen);
				return wasDrawerOpen;
			}
			else
			{
				// Drawer is closed by default.
				targetDataView.setActive(false);
			}
		}
		return false;
	}
}
