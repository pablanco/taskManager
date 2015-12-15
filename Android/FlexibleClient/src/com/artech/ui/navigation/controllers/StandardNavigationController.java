package com.artech.ui.navigation.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.artech.R;
import com.artech.activities.ActivityHelper;
import com.artech.activities.GenexusActivity;
import com.artech.adapters.AdaptersHelper;
import com.artech.android.layout.OrientationLock;
import com.artech.app.ComponentId;
import com.artech.app.ComponentParameters;
import com.artech.app.ComponentUISettings;
import com.artech.application.MyApplication;
import com.artech.base.metadata.ILayoutDefinition;
import com.artech.base.metadata.layout.Size;
import com.artech.base.metadata.theme.ThemeApplicationBarClassDefinition;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.fragments.BaseFragment;
import com.artech.fragments.LayoutFragmentActivityState;
import com.artech.ui.navigation.CallOptions;
import com.artech.ui.navigation.CallOptionsHelper;
import com.artech.ui.navigation.CallType;
import com.artech.ui.navigation.NavigationHandled;
import com.artech.ui.navigation.UIObjectCall;

public class StandardNavigationController extends AbstractNavigationController
{
	private final GenexusActivity mActivity;

	private final static ComponentId COMPONENT_POPUP = new ComponentId(null, "POPUP");

	public StandardNavigationController(GenexusActivity activity)
	{
		mActivity = activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// EnabledHeaderRowPattern
		if (mActivity.getMainLayout() != null && mActivity.getMainLayout().getEnableHeaderRowPattern())
		{
			//set up new drawer properties.
			// ActionBar EnableHeaderRowPattern
			ActivityHelper.setActionBarOverlay(mActivity);
			// StatusBar EnableHeaderRowPattern
			ActivityHelper.setStatusBarOverlay(mActivity);
		}

		mActivity.setContentView(R.layout.standard_navigation);

		// set support toolbar
		Toolbar toolbar = (Toolbar)mActivity.findViewById(R.id.toolbar);
		//mActivity.setSupportActionBar(toolbar);
		if (mActivity instanceof AppCompatActivity)
		{
			AppCompatActivity myActivity = (AppCompatActivity)mActivity;
			myActivity.setSupportActionBar(toolbar);

			setupActionBarInitLayout(mActivity, mActivity.getMainLayout(), true);
		}
	}

	public static void setupActionBarInitLayout(Activity myActivity, ILayoutDefinition layout, boolean addMargins)
	{
		if (layout != null)
		{
			if (layout.getEnableHeaderRowPattern())
			{
				ThemeApplicationBarClassDefinition appBarClass = layout.getHeaderRowApplicationBarClass();
				if (appBarClass != null)
				{
					ActivityHelper.setActionBarThemeClass(myActivity, appBarClass);
				}

				FrameLayout contentLayout = (FrameLayout)myActivity.findViewById(R.id.content_frame);
				RelativeLayout.LayoutParams relativeLayoutParams = (RelativeLayout.LayoutParams)contentLayout.getLayoutParams();
				relativeLayoutParams.addRule(RelativeLayout.BELOW, R.id.toolbarDummyTop);
				contentLayout.setLayoutParams(relativeLayoutParams);

				if (CompatibilityHelper.isStatusBarOverlayingAvailable() && addMargins)
				{
					//add margins (size status bar) to ActionBar
					android.support.v7.widget.Toolbar toolbar = (Toolbar) myActivity.findViewById(R.id.toolbar);
					RelativeLayout.LayoutParams toolbarRelativeLayoutParams = (RelativeLayout.LayoutParams) toolbar.getLayoutParams();
					toolbarRelativeLayoutParams.setMargins(0, AdaptersHelper.getStatusBarHeight(myActivity), 0, 0);
					toolbar.setLayoutParams(toolbarRelativeLayoutParams);
				}
				return;
			}

			// Action Bar below statusbar
			FrameLayout contentLayout = (FrameLayout)myActivity.findViewById(R.id.content_frame);
			RelativeLayout.LayoutParams relativeLayoutParams = (RelativeLayout.LayoutParams)contentLayout.getLayoutParams();
			relativeLayoutParams.addRule(RelativeLayout.BELOW, R.id.toolbar);
			contentLayout.setLayoutParams(relativeLayoutParams);

			if (CompatibilityHelper.isStatusBarOverlayingAvailable() && addMargins)
			{
				android.support.v7.widget.Toolbar toolbar = (Toolbar) myActivity.findViewById(R.id.toolbar);
				RelativeLayout.LayoutParams toolbarRelativeLayoutParams = (RelativeLayout.LayoutParams) toolbar.getLayoutParams();
				toolbarRelativeLayoutParams.setMargins(0, 0, 0, 0);
				toolbar.setLayoutParams(toolbarRelativeLayoutParams);
			}

			if ( layout.getApplicationBarClass() != null)
				ActivityHelper.setActionBarThemeClass(myActivity, layout.getApplicationBarClass());
		}
	}


	@Override
	public boolean start(ComponentParameters mainParams, LayoutFragmentActivityState previousState)
	{
		BaseFragment fragment = mActivity.createComponent(ComponentId.ROOT, mainParams, ComponentUISettings.main());
		FragmentTransaction fragmentTransaction = mActivity.getSupportFragmentManager().beginTransaction();
		fragmentTransaction.replace(R.id.content_frame, fragment);
		fragmentTransaction.commit();

		return true;
	}

	@Override
	public NavigationHandled handle(UIObjectCall call, Intent callIntent)
	{
		if (handlePopup(mActivity, call))
			return NavigationHandled.HANDLED_WAIT_FOR_RESULT;

		// Let the action create a new activity.
		return NavigationHandled.NOT_HANDLED;
	}

	/**
	 * Standard popup/callout handling. Can be used by other navigations.
	 * @param call Call to (possibly) handle.
	 * @return True if handled (was a popup/callout), otherwise false.
	 */
	public static boolean handlePopup(GenexusActivity activity, UIObjectCall call)
	{
		CallOptions callOptions = CallOptionsHelper.getCallOptions(call.getObject(), call.getMode());
		if (callOptions.getCallType() == CallType.CALLOUT || callOptions.getCallType() == CallType.POPUP)
		{
			// Use DialogFragment for Callouts & Popups, consider that we don't have navigation inside this kind of dialogs
			handlePopup(activity, call, callOptions);

			// Remove global configured CallOptions after call.
			CallOptionsHelper.resetCallOptions(call.getObject().getObjectName());

			return true;
		}
		else
			return false;
	}

	private static void handlePopup(GenexusActivity activity, UIObjectCall call, CallOptions callOptions)
	{
		// Get the fragment for the given object.
		ComponentParameters parameters = call.toComponentParams();
		Size popupSize = CallOptionsHelper.getTargetSize(call, callOptions);
		ComponentUISettings uiSettings = new ComponentUISettings(false, null, popupSize);
		BaseFragment popupFragment = activity.createComponent(COMPONENT_POPUP, parameters, uiSettings);

		popupFragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);

		// Today we are not using the anchor rect to give a contextual position to the dialog,
		// pass this information anyway for a different approach later.
		if (callOptions.getCallType() == CallType.CALLOUT)
			popupFragment.setDialogAnchor(call.getContext().getAnchor());

		// Show the dialog
		OrientationLock.lock(activity, OrientationLock.REASON_SHOW_POPUP);
		popupFragment.show(activity.getSupportFragmentManager(), "genexus");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
    	if (item.getItemId() == android.R.id.home)
    	{
    		// Home button normally calls main. Disable it if already on main.
    		if (mActivity.getMainDefinition() == MyApplication.getApp().getMain())
    			return true;
    	}

		return false;
	}
}
