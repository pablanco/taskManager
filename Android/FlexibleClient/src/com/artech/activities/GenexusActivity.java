package com.artech.activities;

import java.util.Collections;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

import com.artech.actions.UIContext;
import com.artech.android.gcm.GcmIntentService;
import com.artech.app.ComponentId;
import com.artech.app.ComponentParameters;
import com.artech.app.ComponentUISettings;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.DashboardItem;
import com.artech.base.metadata.DashboardMetadata;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.enums.DisplayModes;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.fragments.BaseFragment;
import com.artech.fragments.DashboardFragment;
import com.artech.fragments.FragmentFactory;
import com.artech.fragments.IDataView;
import com.artech.fragments.LayoutFragment;
import com.artech.fragments.LayoutFragmentActivity;
import com.artech.fragments.LayoutFragmentActivityState;
import com.artech.ui.navigation.INavigationActivity;
import com.artech.ui.navigation.Navigation;
import com.artech.ui.navigation.NavigationController;
import com.artech.ui.navigation.controllers.StandardNavigationController;

public class GenexusActivity extends LayoutFragmentActivity implements INavigationActivity
{
	private boolean mIsActive;
	private NavigationController mNavigationController = new StandardNavigationController(this);
	private ComponentParameters mMainParams;

	@Override
	protected boolean initializeController(ActivityController controller)
	{
		Intent intent = getIntent();
		if (controller.initializeFrom(getIntent()))
		{
			mMainParams = controller.getModel().getMain().getParams();
		}
		else if (Strings.hasValue(intent.getStringExtra(IntentParameters.DashBoardMetadata)))
		{
			DashboardMetadata mainViewDefinition = (DashboardMetadata) Services.Application.getPattern(intent.getStringExtra(IntentParameters.DashBoardMetadata));
			mMainParams = new ComponentParameters(mainViewDefinition, DisplayModes.VIEW, Collections.<String>emptyList());
		}

		if (mMainParams != null)
			mNavigationController = Navigation.createController(this, mMainParams.Object);

		return (mMainParams != null);
	}

	@Override
	protected boolean initializeView(ActivityController controller, Bundle savedInstanceState, LayoutFragmentActivityState previousState)
	{
		mNavigationController.onCreate(savedInstanceState);
		return mNavigationController.start(mMainParams, previousState);
	}

	@Override
	public NavigationController getNavigationController()
	{
		return mNavigationController;
	}

	@Override
	public UIContext getUIContext()
	{
		return getController().getModel().getUIContext();
	}

	@Override
	public @NonNull BaseFragment createComponent(@NonNull ComponentId id, @NonNull ComponentParameters parameters, @NonNull ComponentUISettings uiSettings)
	{
		BaseFragment fragment = FragmentFactory.newFragment(parameters);
		initializeComponent(fragment, id, parameters, uiSettings);
		registerFragment(fragment);

		if (uiSettings.isMain)
		{
			// If has notification action, handle it (only if main is an sd panel).
			handleIntent(getIntent(), parameters.Object, fragment);
		}

		return fragment;
	}

	private void initializeComponent(BaseFragment fragment, ComponentId id, ComponentParameters params, ComponentUISettings uiSettings)
	{
		if (fragment != null)
		{
			if (params.Object instanceof IDataViewDefinition)
			{
				LayoutFragment layoutFragment = (LayoutFragment)fragment;
				initializeLayoutFragment(layoutFragment, id, params, uiSettings);
				layoutFragment.setDesiredSize(uiSettings.size);
			}
			else if (params.Object instanceof DashboardMetadata)
			{
				DashboardFragment dashboardFragment = (DashboardFragment)fragment;
				dashboardFragment.initialize((DashboardMetadata)params.Object, getUIContext().getConnectivitySupport());
				restoreFragmentState(dashboardFragment);
			}
		}
	}

	private void handleIntent(Intent intent, IViewDefinition definition, BaseFragment fragment)
	{
		if (!handleNotification(intent.getStringExtra(IntentParameters.NotificationAction), intent.getStringExtra(IntentParameters.NotificationParameters), definition, fragment))
		{
			handleAppIntent(intent, fragment.getContextEntity());
		}
	}

	private void handleAppIntent(Intent intent, Entity entity)
	{
		Services.Application.handleIntent(getUIContext(), intent, entity);
	}

	static boolean handleNotification(String notificationAction, String notificationParameters , IViewDefinition definition, BaseFragment fragment)
	{
		if (Services.Strings.hasValue(notificationAction))
		{
			ActionDefinition actionDefinition = definition.getEvent(notificationAction);
			// handle notification action for Dashboard Slide
			if (actionDefinition==null && definition instanceof DashboardMetadata)
			{
				DashboardMetadata dashboardMetadata = ((DashboardMetadata)definition);
				DashboardItem dashboardItem = dashboardMetadata.getNotificationActions().get(notificationAction);
				if (dashboardItem!=null)
					actionDefinition = dashboardItem.getActionDefinition();
			}
			if (actionDefinition!=null)
			{
				if (Services.Strings.hasValue(notificationParameters))
				{
					fragment.getContextEntity().setExtraMembers(definition.getVariables());

					GcmIntentService.addNotificationParametersToEntity(fragment.getContextEntity(), notificationParameters);
				}
				fragment.runAction(actionDefinition, null);
				return true;
			}
		}
		return false;
	}

	@Override
	public void destroyComponent(BaseFragment fragment)
	{
		for (BaseFragment child : fragment.getChildFragments())
			destroyComponent(child);

		if (fragment instanceof LayoutFragment)
		{
			// Requires custom finalization.
			LayoutFragment layoutFragment = (LayoutFragment)fragment;
			finalizeLayoutFragment(layoutFragment);
		}

		unregisterFragment(fragment);
	}

	public boolean isActive()
	{
		return mIsActive;
	}

	// *************************************************************************
	// Forwarding of events to NavigationController.
	// *************************************************************************

	@Override
	public void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		mNavigationController.onPostCreate(savedInstanceState);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		mIsActive = true;
		mNavigationController.onResume();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		mNavigationController.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		mNavigationController.onSaveInstanceState(outState);
	}

	@Override
	protected void saveActivityState(LayoutFragmentActivityState outState)
	{
		super.saveActivityState(outState);
		mNavigationController.saveActivityState(outState);
	}

	@Override
	protected void onPause()
	{
		mNavigationController.onPause();
		mIsActive = false;
		super.onPause();
	}

	public void setTitle(CharSequence title, IDataView fromDataView)
	{
		if (mNavigationController.setTitle(fromDataView, title))
			return;

		// Default implementation.
		setTitle(title);
	}

	@Override
	public View findViewById(int id)
	{
		View view = mNavigationController.findViewById(id);
		if (view != null)
			return view;

		return super.findViewById(id);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (mNavigationController.onOptionsItemSelected(item))
			return true;

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onKeyUp(int keyCode, @NonNull KeyEvent event)
	{
		if (mNavigationController.onKeyUp(keyCode, event))
			return true;

		return super.onKeyUp(keyCode, event);
	}
	
	@Override
	public void onBackPressed()
	{
		if (mNavigationController.onBackPressed())
			return;

		super.onBackPressed();
	}	
}
