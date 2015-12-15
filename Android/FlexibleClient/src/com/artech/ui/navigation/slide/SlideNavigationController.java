package com.artech.ui.navigation.slide;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.artech.R;
import com.artech.activities.ActivityFlowControl;
import com.artech.activities.ActivityHelper;
import com.artech.activities.DataViewHelper;
import com.artech.activities.GenexusActivity;
import com.artech.activities.IntentParameters;
import com.artech.adapters.AdaptersHelper;
import com.artech.android.ResourceManager;
import com.artech.app.ComponentId;
import com.artech.app.ComponentParameters;
import com.artech.app.ComponentUISettings;
import com.artech.base.metadata.ILayoutDefinition;
import com.artech.base.metadata.enums.LayoutModes;
import com.artech.base.services.Services;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.base.utils.Strings;
import com.artech.compatibility.SherlockHelper;
import com.artech.fragments.BaseFragment;
import com.artech.fragments.IDataView;
import com.artech.fragments.LayoutFragmentActivityState;
import com.artech.ui.navigation.CallOptions;
import com.artech.ui.navigation.CallOptionsHelper;
import com.artech.ui.navigation.CallTarget;
import com.artech.ui.navigation.CallType;
import com.artech.ui.navigation.FragmentLauncher;
import com.artech.ui.navigation.NavigationHandled;
import com.artech.ui.navigation.UIObjectCall;
import com.artech.ui.navigation.controllers.AbstractNavigationController;
import com.artech.ui.navigation.controllers.StandardNavigationController;
import com.artech.ui.navigation.slide.SlideNavigation.Target;
import com.artech.utils.Cast;
import com.artech.utils.KeyboardUtils;

class SlideNavigationController extends AbstractNavigationController
{
	private final GenexusActivity mActivity;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;

	private SlideComponents mComponents;
	private SlideNavigationTargets mTargets;
	private Pair<UIObjectCall, Target> mPendingReplace;

	private CharSequence mContentTitle;
	private boolean mStartupComplete;

	public SlideNavigationController(GenexusActivity activity)
	{
		mActivity = activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		//EnableHeaderRowPattern
		//set up new drawer properties.
		// ActionBar EnableHeaderRowPattern
		ActivityHelper.setActionBarOverlay(mActivity);
		// StatusBar EnableHeaderRowPattern
		ActivityHelper.setStatusBarOverlay(mActivity);

		// Set up layout.
		mActivity.setContentView(R.layout.slide_navigation);
		mDrawerLayout = (DrawerLayout)mActivity.findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerShadow(R.drawable.gx_drawer_shadow, GravityCompat.START);
		mTargets = new SlideNavigationTargets(mDrawerLayout);

		Toolbar toolbar = (Toolbar)mActivity.findViewById(R.id.toolbar);
		mActivity.setSupportActionBar(toolbar);


		// Set drawer background according to theme.
		setupDrawer(R.id.left_drawer);
		setupDrawer(R.id.right_drawer);

		// Enable ActionBar app icon to behave as action to toggle nav drawer
		mActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		mActivity.getSupportActionBar().setHomeButtonEnabled(true);

		if (CompatibilityHelper.isStatusBarOverlayingAvailable())
		{
			// set size of statusbar to dummy view
			FrameLayout toolbarStatusBarDummyTop = (FrameLayout)mActivity.findViewById(R.id.statusBarDummyTop);
			ViewGroup.LayoutParams params = toolbarStatusBarDummyTop.getLayoutParams();
			params.height = AdaptersHelper.getStatusBarHeight(mActivity);
			params.width = ViewGroup.LayoutParams.MATCH_PARENT;
			toolbarStatusBarDummyTop.setLayoutParams(params);

			//set toolbar margin, do it only one time
			RelativeLayout.LayoutParams toolbarRelativeLayoutParams = (RelativeLayout.LayoutParams) toolbar.getLayoutParams();
			toolbarRelativeLayoutParams.setMargins(0, AdaptersHelper.getStatusBarHeight(mActivity), 0, 0);
			toolbar.setLayoutParams(toolbarRelativeLayoutParams);
		}

	}

	private void setupDrawer(int drawerContainerId)
	{
		// Set drawer background according to theme.
		View drawerContainer = mActivity.findViewById(drawerContainerId);
		int drawerBackground = ResourceManager.getResource(mActivity, android.R.drawable.screen_background_dark, android.R.drawable.screen_background_light);
		drawerContainer.setBackgroundResource(drawerBackground);

	}

	@Override
	public boolean start(ComponentParameters mainParams, LayoutFragmentActivityState previousState)
	{
		if (previousState != null)
			mComponents = SlideComponents.readFrom(previousState);

		if (mComponents == null)
			mComponents = SlideNavigation.getComponents(mActivity.getIntent(), mainParams);

		mTargets.start(mActivity, mComponents.IsLeftMainComponent ? mComponents.get(Target.Left) : mComponents.get(Target.Content));

		// Set up custom drawer toggle manager.
		mDrawerToggle = new DrawerToggle(mActivity, mDrawerLayout);
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		mDrawerToggle.setDrawerIndicatorEnabled(mComponents.IsHub);

		FragmentTransaction transaction = mActivity.getSupportFragmentManager().beginTransaction();

		// Initialize the drawer and content fragments (if available).
		IDataView contentFragment = initializeFragment(transaction, Target.Content, !mComponents.IsLeftMainComponent);
		IDataView leftFragment = initializeDrawerFragment(transaction, Target.Left, mComponents.IsLeftMainComponent);
		IDataView rightFragment = initializeDrawerFragment(transaction, Target.Right, false);

		mTargets.putFragment(Target.Left, leftFragment);
		mTargets.putFragment(Target.Content, contentFragment);
		mTargets.putFragment(Target.Right, rightFragment);

		mTargets.restoreStateFrom(previousState);

		transaction.commit();

		if (!mActivity.isLoginPending())
			afterStart();

		return true;
	}

	private IDataView initializeFragment(FragmentTransaction transaction, Target target, boolean isMain)
	{
		ComponentParameters params = mComponents.get(target);
		if (params != null)
		{
			ComponentId componentId = SlideNavigationTargets.getComponentId(target);
			ComponentUISettings uiSettings = new ComponentUISettings(isMain, null, mTargets.getSize(mActivity, target, params));

			BaseFragment fragment = mActivity.createComponent(componentId, params, uiSettings);
			int targetViewId = SlideNavigationTargets.getTargetViewId(target);
			transaction.replace(targetViewId, fragment);

			if (target == Target.Content)
			{
			 	UIObjectCall data = new UIObjectCall(mActivity.getUIContext(), params);
				if (data.getObjectLayout() != null)
				{
					ILayoutDefinition layout = data.getObjectLayout();
					StandardNavigationController.setupActionBarInitLayout(mActivity, layout, false);
				}
			}

			return Cast.as(IDataView.class, fragment);
		}
		else
			return null;
	}

	private IDataView initializeDrawerFragment(FragmentTransaction transaction, Target target, boolean isMain)
	{
		if (mComponents.get(target) != null)
		{
			// Initialize the drawer fragment.
			return initializeFragment(transaction, target, isMain);
		}
		else
		{
			// Lock drawer if no fragment will be shown in it.
			int drawerGravity = SlideNavigationTargets.getGravity(target);
			mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, drawerGravity);
			return null;
		}
	}

	private void afterStart()
	{
		// Run the startup action.
		if (mTargets.getFragment(Target.Left) != null && mComponents.PendingAction != null &&
			!Strings.hasValue(mActivity.getIntent().getStringExtra(IntentParameters.NotificationAction)))
		{
			mTargets.getFragment(Target.Left).runAction(mComponents.PendingAction, null);
			mComponents.PendingAction = null;
		}

		mStartupComplete = true;
	}

	@Override
	public NavigationHandled handle(final UIObjectCall call, Intent callIntent)
	{
		CallOptions callOptions = CallOptionsHelper.getCallOptions(call.getObject(), call.getMode());

		if (CallTarget.BLANK.isTarget(callOptions))
			return NavigationHandled.NOT_HANDLED; // Always create a new Activity for a Target=Blank call.

		if (StandardNavigationController.handlePopup(mActivity, call))
			return NavigationHandled.HANDLED_WAIT_FOR_RESULT; // Wait for popup/callout output.

		if (!canCreateFragment(call))
			return NavigationHandled.NOT_HANDLED; // We cannot perform this action locally, so always call a new activity.

		Target target;
		boolean isReplace;

		// If the call specifies a custom target, it's implicit to be replace.
		if (SlideNavigation.TARGET_LEFT.isTarget(callOptions))
		{
			target = Target.Left;
			isReplace = true;
		}
		else if (SlideNavigation.TARGET_CONTENT.isTarget(callOptions))
		{
			target = Target.Content;
			isReplace = true;
		}
		else if (SlideNavigation.TARGET_RIGHT.isTarget(callOptions))
		{
			target = Target.Right;
			isReplace = true;
		}
		else
		{
			// No (or invalid) target: Use default behavior.
			target = Target.Content;
			boolean isFromLeft = (mTargets.getFragment(Target.Left) != null &&
								  call.getContext().getDataView() == mTargets.getFragment(Target.Left));

			isReplace = (isFromLeft || callOptions.getCallType() == CallType.REPLACE);
		}

		// We currently don't have PUSH, so any non-replaces are handled as standard calls.
		if (!isReplace)
			return NavigationHandled.NOT_HANDLED;

		// Executing from menu, but we are NOT currently in a hub?
		// We want to execute a new activity, but signal it that it's a hub.
		if (!mComponents.IsHub && target == Target.Content)
		{
			callIntent.putExtra(SlideNavigation.INTENT_EXTRA_IS_HUB_CALL, true);
			return NavigationHandled.NOT_HANDLED;
		}

		// Run the specified component by replacing fragment in this activity.
		// Subsequent lines in the event (if any) execute immediately.
		final Target replaceTarget = target;
		Services.Device.runOnUiThread(new Runnable()
		{
			@Override
			public void run() { replaceFragment(call, replaceTarget); }
		});

		return NavigationHandled.HANDLED_CONTINUE;
	}

	private boolean canCreateFragment(UIObjectCall call)
	{
		// TODO: Should this be in GenexusActivity?
		// The only unsupported thing is an edit fragment.
		return (call.getObject() != null && call.getMode() == LayoutModes.VIEW);
	}

	@SuppressWarnings("ResourceType") // @EdgeGravity is not public!
	private void replaceFragment(UIObjectCall data, Target target)
	{
		if (!mActivity.isActive())
		{
			mPendingReplace = new Pair<UIObjectCall, SlideNavigation.Target>(data, target);
			return;
		}

		// Close the virtual keyboard. The focus was either in the old fragment (which is destroyed)
		// or inside the drawer (which is closed). In both cases, it should not be visible anymore.
		View focused = mActivity.getCurrentFocus();
		if (focused != null && focused instanceof EditText)
			KeyboardUtils.hideKeyboard(focused);

		// If there was a previous fragment, discard it.
		IDataView previousFragment = mTargets.getFragment(target);
		if (previousFragment != null)
			mActivity.destroyComponent((BaseFragment) previousFragment);

		ComponentParameters params = data.toComponentParams();
		mComponents.set(target, params);

		ComponentId id = SlideNavigationTargets.getComponentId(target);
		ComponentUISettings uiSettings = new ComponentUISettings(target == Target.Content, null, mTargets.getSize(mActivity, target, params));

		BaseFragment newFragment = mActivity.createComponent(id, params, uiSettings);
		IDataView newDataView = (IDataView)newFragment;
		mTargets.putFragment(target, newDataView);

		// TODO: Should use BackStack depending on data.getCallOptions().
		FragmentTransaction transaction = mActivity.getSupportFragmentManager().beginTransaction();
		prepareFragmentTransaction(transaction, data);
		transaction.replace(SlideNavigationTargets.getTargetViewId(target), newFragment);
		transaction.commit();

		if (target == Target.Content)
		{
			// For content, set title and hide the menu (show content full-screen).
			DataViewHelper.setTitle(mActivity, newDataView, data.getObject().getCaption());
			mDrawerLayout.closeDrawer(SlideNavigationTargets.getGravity(Target.Left));
			mDrawerLayout.closeDrawer(SlideNavigationTargets.getGravity(Target.Right));

			if (data.getObjectLayout() != null)
			{
				ILayoutDefinition layout = data.getObjectLayout();
				StandardNavigationController.setupActionBarInitLayout(mActivity, layout, false);
			}
		}
		else
		{
			// Drawer may have been locked, unlock it.
			mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, SlideNavigationTargets.getGravity(target));

			// If showing drawer on an open Target (e.g. CallType = Replace on itself) set it as Active.
			if (mDrawerLayout.isDrawerOpen(SlideNavigationTargets.getGravity(target)))
			{
				newDataView.setActive(true);
				SherlockHelper.invalidateOptionsMenu(mActivity);
			}
		}
	}

	private static void prepareFragmentTransaction(FragmentTransaction transaction, UIObjectCall data)
	{
		CallOptions callOptions = CallOptionsHelper.getCallOptions(data.getObject(), data.getMode());
		FragmentLauncher.applyCallOptions(transaction, data.getObject(), callOptions);
	}

	private class DrawerToggle extends ActionBarDrawerToggle
	{
		public DrawerToggle(Activity activity, DrawerLayout drawerLayout)
		{
			super(activity, drawerLayout, 0, 0);
		}

		private IDataView getDrawerFragmentFromDrawerView(View view)
		{
			if (view.getId() == R.id.right_drawer)
				return mTargets.getFragment(Target.Right);
			else
				return mTargets.getFragment(Target.Left);
		}

		@Override
		public void onDrawerClosed(View drawerView)
		{
			IDataView drawerFragment = getDrawerFragmentFromDrawerView(drawerView);
			if (drawerFragment != null)
				drawerFragment.setActive(false);

			if (mTargets.getFragment(Target.Content) != null)
				mTargets.getFragment(Target.Content).setActive(true);

			mActivity.setTitle(mContentTitle);

			SherlockHelper.invalidateOptionsMenu(mActivity);
		}

		@Override
		public void onDrawerOpened(View drawerView)
		{
			IDataView drawerFragment = getDrawerFragmentFromDrawerView(drawerView);
			if (drawerFragment != null)
				drawerFragment.setActive(true);

			if (mTargets.getFragment(Target.Content) != null)
				mTargets.getFragment(Target.Content).setActive(false);

			mActivity.setTitle(R.string.app_name);
			SherlockHelper.invalidateOptionsMenu(mActivity);
		}
	}

	@Override
	public void onPostCreate(Bundle savedInstanceState)
	{
		if (mDrawerToggle != null)
			mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		if (mDrawerToggle != null)
			mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void saveActivityState(LayoutFragmentActivityState outState)
	{
		if (mComponents != null)
			mComponents.saveTo(outState);

		if (mTargets != null)
			mTargets.saveStateTo(outState);
	}

	@Override
	public void onResume()
	{
		if (mTargets == null)
			return; // onCreate() was not called? Probably because the activity is being destroyed.

		if (!mStartupComplete)
		{
			// If startup was aborted due to a login call, continue it now.
			if (!mActivity.isLoginPending())
				afterStart();
		}
		else
		{
			Pair<UIObjectCall, Target> pendingReplace = mPendingReplace;
			mPendingReplace = null;

			// Fire the replaceFragment() operation that was ignored the last time
			// (e.g. due to calling the login activity).
			if (pendingReplace != null)
				replaceFragment(pendingReplace.first, pendingReplace.second);
		}
	}

	@Override
	public boolean setTitle(IDataView fromDataView, CharSequence title)
	{
		if (mDrawerLayout != null && fromDataView == mTargets.getFragment(Target.Content))
		{
			mContentTitle = title;

			if (!mDrawerLayout.isDrawerVisible(GravityCompat.START))
				mActivity.setTitle(mContentTitle);

			return true;
		}
		else if (mDrawerLayout == null && fromDataView == null)
		{
			// Setting title from metadata (instead of data). Store it.
			mContentTitle = title;
		}

		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == android.R.id.home)
		{
			if (mComponents.IsHub)
			{
				mDrawerToggle.onOptionsItemSelected(item);
				return true; // Bug: ActionBarDrawerToggle.onOptionsItemSelected() always return false. Change to return when it's fixed.
			}
			else
			{
				// Not a hub, use as back.
				ActivityFlowControl.finishWithCancel(mActivity);
				return true;
			}
		}

		return false;
	}
}
