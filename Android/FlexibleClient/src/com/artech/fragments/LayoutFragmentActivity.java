package com.artech.fragments;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.artech.actions.UIContext;
import com.artech.activities.ActivityController;
import com.artech.activities.ActivityHelper;
import com.artech.activities.ActivityLauncher;
import com.artech.activities.ActivityModel;
import com.artech.activities.DataViewHelper;
import com.artech.activities.GxBaseActivity;
import com.artech.activities.IGxActivity;
import com.artech.android.analytics.Tracker;
import com.artech.app.ComponentId;
import com.artech.app.ComponentParameters;
import com.artech.app.ComponentUISettings;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.ILayoutDefinition;
import com.artech.base.metadata.enums.Orientation;
import com.artech.base.metadata.layout.LayoutDefinition;
import com.artech.base.metadata.theme.ThemeApplicationBarClassDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.DataRequest;
import com.artech.common.SecurityHelper;
import com.artech.controllers.DataViewController;
import com.artech.controllers.DataViewModel;
import com.artech.controllers.IDataViewController;
import com.artech.controllers.ViewData;
import com.artech.controls.IGxLocalizable;
import com.artech.utils.Cast;

/**
 * Base class for all activities that support fragments.
 */
public abstract class LayoutFragmentActivity extends GxBaseActivity implements IGxActivity, IDataViewHost, IGxLocalizable
{
	private ActivityController mController;
	private Set<BaseFragment> mFragments;
	private Set<IDataView> mDataViews;
	private LayoutFragment mMainFragment;

	// Analytics Tracking.
	private final Tracker.ActivityTracker mTracker = new Tracker.ActivityTracker(this);

	// State
	private boolean mLoginCalled;
	private LayoutFragmentActivityState mPreviousState;
	private boolean mPreviousStateRestored;
	private long mActivityTimestamp;
	private boolean mActivityDestroyedToApplyOrientation;

	public abstract UIContext getUIContext();

	private boolean mPaused = true;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
    	ActivityHelper.onBeforeCreate(this);
		super.onCreate(savedInstanceState);
		ActivityHelper.initialize(this, savedInstanceState);

		if (!Services.Application.isLoaded())
		{
			finish();
			return;
		}

		mFragments = new LinkedHashSet<BaseFragment>();
		mDataViews = new LinkedHashSet<IDataView>();
		mController = new ActivityController(this);

		if (!initializeController(mController))
		{
			setContentView(ActivityHelper.getInvalidMetadataMessage(this));
			return;
		}

		// Restore state if changing orientation. Store it in local variable to pass to fragments later.
		mPreviousState = Cast.as(LayoutFragmentActivityState.class, getLastCustomNonConfigurationInstance());
		mController.restoreState(mPreviousState);

		if (getMainDefinition() != null)
		{
			LayoutDefinition mainLayout = Cast.as(LayoutDefinition.class, getMainLayout());
			if (mainLayout != null)
			{
				Orientation desiredOrientation = mainLayout.getActualOrientation();
				if (desiredOrientation != Orientation.UNDEFINED && desiredOrientation != Services.Device.getScreenOrientation())
				{
					mActivityDestroyedToApplyOrientation = true;
					ActivityHelper.setOrientation(this, desiredOrientation);
					return;
				}
			}

			Services.Log.debug(String.format("Starting '%s'...", getMainDefinition().getName()));

			DataViewHelper.setTitle(this, null, getMainDefinition().getCaption());
		}

		initializeView(mController, savedInstanceState, mPreviousState);

		// Hide title bar if MAIN data view instructs it. This must be done after calling setContentView().
		if (getMainLayout() != null && !getMainLayout().getShowApplicationBar())
			ActivityHelper.setActionBarVisibility(this, false);

		ActivityHelper.applyStyle(this, getMainLayout());

		// Analytics tracking.
		mTracker.onStart(getMainDefinition());

		mActivityTimestamp = System.nanoTime();
		// com.android.debug.hv.ViewServer.get(this).addWindow(this);
	}

	@Override
	public IDataViewDefinition getMainDefinition()
	{
		if (mController != null && mController.getModel() != null && mController.getModel().getMain() != null)
			return mController.getModel().getMain().getDefinition();
		else
			return null;
	}

	@Override
	public ILayoutDefinition getMainLayout()
	{
		// TODO: This is a redundant calculation; should be done somewhere else.
		if (mController != null && mController.getModel() != null && mController.getModel().getMain() != null)
		{
			DataViewModel model = mController.getModel().getMain();
			if (model.getDefinition() != null)
				return model.getDefinition().getLayoutForMode(model.getParams().Mode);
		}

		return null;
	}

	protected abstract boolean initializeController(ActivityController controller);
	protected abstract boolean initializeView(ActivityController controller, Bundle savedInstanceState, LayoutFragmentActivityState previousState);

	protected void registerFragment(BaseFragment fragment)
	{
		mFragments.add(fragment);
	}

	protected void unregisterFragment(BaseFragment fragment)
	{
		mFragments.remove(fragment);
	}

	protected void initializeLayoutFragment(LayoutFragment component, ComponentId id, ComponentParameters params, ComponentUISettings uiSettings)
	{
		mDataViews.add(component);

		IDataViewController controller = mController.getController(getUIContext(), component, id, params);
		component.initialize(getUIContext().getConnectivitySupport(), this, (LayoutFragment)uiSettings.parent, controller);

		if (uiSettings.size != null)
			component.setDesiredSize(uiSettings.size);

		if (uiSettings.isMain)
		{
			mMainFragment = component;
			LayoutDefinition mainLayout = component.getLayout();

			if (mainLayout != null)
			{
				// Change orientation if MAIN data view asks to do so.
				Orientation desiredOrientation = mainLayout.getActualOrientation();
				if (desiredOrientation != Orientation.UNDEFINED)
				{
					// Make sure ALL activity stops before CHANGING orientation.
					// Otherwise controllers may return with data for the now-defunct activity.
					if (desiredOrientation != Services.Device.getScreenOrientation())
					{
						mController.onDestroy();
						mActivityDestroyedToApplyOrientation = true;
					}

					// Request a FIXED orientation if needed.
					// This could mean rotating now (if current orientation is different) or preventing a future rotation.
					if (!mainLayout.isOrientationSwitchable())
						ActivityHelper.setOrientation(this, desiredOrientation);
				}
			}
		}

		// Restore state associated to this DV.
		restoreFragmentState(component);

		// Main data view is always active.
		if (uiSettings.isMain)
			component.setActive(true);

		UIContext context = component.getUIContext();
		if (context.getActivity() == null)
			context = getUIContext(); // Workaround: Fragment.getActivity() is null until the fragment has been attached.

		// If entering a restricted data view, redirect to login.
		// Must be done at the end of this method (after expanding layout), because successful login will return here.
		// Also, only call login ONCE per activity (without flag, it may be called once by detail and once by inline section).
		if (!mLoginCalled && SecurityHelper.callLoginIfNecessary(context, params.Object))
		{
			mLoginCalled = true;
			Services.Log.debug(String.format("Redirecting from '%s' startup to login.", params.Object));
		}
	}

	protected void restoreFragmentState(BaseFragment fragment)
	{
		if (mPreviousState != null)
		{
			LayoutFragmentState fragmentState = mPreviousState.getState(fragment.getDefinition());
			if (fragmentState != null)
			{
				if (fragment instanceof LayoutFragment)
				{
					LayoutFragment layoutFragment = (LayoutFragment)fragment;
					if (fragmentState.getData() != null && !fragmentState.getData().isEmpty())
					{
						// Set entity from saved state.
						ViewData data = ViewData.customData(fragmentState.getData(), DataRequest.RESULT_SOURCE_LOCAL);
						((DataViewController)layoutFragment.getController()).restoreRootData(data);
						layoutFragment.update(data);
					}
				}

				// Opportunity to restore custom state.
				fragment.restoreFragmentState(fragmentState);
			}
		}
	}

	public boolean isLoginPending()
	{
		return mLoginCalled;
	}

	protected void finalizeLayoutFragment(LayoutFragment dataView)
	{
		mDataViews.remove(dataView);
		mController.remove(dataView);
		dataView.setActive(false);
	}

	@Override
	public Iterable<IDataView> getActiveDataViews()
	{
		ArrayList<IDataView> activeViews = new ArrayList<IDataView>();
		for (IDataView dataView : mDataViews)
			if (dataView.isActive() && dataView.getLayout() != null)
				activeViews.add(dataView);

		return activeViews;
	}

	@Override
	public ActivityModel getModel()
	{
		return mController.getModel();
	}

	@Override
	public ActivityController getController()
	{
		return mController;
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		ActivityHelper.onNewIntent(this, intent);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if (!ActivityHelper.onResume(this))
			return;

		// Restore global activity state.
		if (mPreviousState != null && !mPreviousStateRestored)
		{
			restoreActivityState(mPreviousState);
			mPreviousStateRestored = true;
		}

		mController.onResume();
		mPaused = false;
		// com.android.debug.hv.ViewServer.get(this).setFocusedWindow(this);
	}

	@Override
	protected void onPause()
	{
		ActivityHelper.onPause(this);
		mController.onPause();
		mPaused = true;
		super.onPause();
	}

	@Override
	public void refreshData(boolean keepPosition)
	{
		mController.onRefresh(keepPosition);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if (mController != null)
			return mController.onCreateOptionsMenu(menu);

		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		return mController.onOptionsItemSelected(item);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		ActivityHelper.onSaveInstanceState(this, outState);
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		// Handle action continuation and refresh/reload on activity result.
		mController.onActivityResult(requestCode, resultCode, data);
		ActivityHelper.onActivityResult(this, requestCode, resultCode, data);
		mLoginCalled = false;
	}

	@Override
	public boolean onSearchRequested()
	{
		return mController.onSearchRequested();
	}

	@Override
	public final Object onRetainCustomNonConfigurationInstance()
	{
		// If this configuration change is due to the activity rotating because the main
		// fragment told us to do so, then return null, because there is no state to save.
		// The new activity should be reconstructed based only on the supplied intent.
		if (mActivityDestroyedToApplyOrientation)
			return null;

		// Fix for (at least) Samsung Galaxy bug: when the camera is invoked and returns, the activity
		// may be rotated twice very quickly. In that case, the 2nd, interim activity doesn't get the
		// data from the taken photo into the control, and when it's asked to copy control data into the
		// entity, overwrites the correct value.
		// As a temporary fix, assume that an activity with a very short lifetime doesn't have any changed data.
		long lifetimeMillis = (System.nanoTime() - mActivityTimestamp) / 1000000;
		if (mPreviousState != null && lifetimeMillis < 800)
			return mPreviousState;

		LayoutFragmentActivityState state = new LayoutFragmentActivityState();
		mController.saveState(state);

		saveActivityState(state);

		for (BaseFragment fragment : mFragments)
			state.saveState(fragment);

		return state;
	}

	private static final String STATE_ACTION_BAR_THEME_CLASS = "ActionBar::ThemeClass";

	/**
	 * Override to save custom state (for orientation change).
	 */
	protected void saveActivityState(LayoutFragmentActivityState state)
	{
		// Save Action Bar state.
		state.setProperty(STATE_ACTION_BAR_THEME_CLASS, ActivityHelper.getActionBarThemeClass(this));
	}

	/**
	 * Override to restore custom state (from orientation change).
	 */
	protected void restoreActivityState(LayoutFragmentActivityState state)
	{
		ActivityHelper.setActionBarThemeClass(this, state.getProperty(ThemeApplicationBarClassDefinition.class, STATE_ACTION_BAR_THEME_CLASS));
	}

	@Override
	public void setReturnResult()
	{
		Intent data = new Intent();

		if (mMainFragment != null)
			mMainFragment.setReturnResult(data);

		setResult(Activity.RESULT_OK, data);
	}

	@Override
	public void onBackPressed()
	{
		if (mController != null && mController.handleOnBackPressed())
			return;

		super.onBackPressed();
	}

	@Override
	public void finish()
	{
		Services.Device.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				LayoutFragmentActivity.super.finish();
				ActivityLauncher.onReturn(LayoutFragmentActivity.this, getIntent());
			}
		});
	}

	@Override
	public void onStop()
	{
		super.onStop();
		mTracker.onStop();
	}

	@Override
	protected void onDestroy()
	{
		if (mController != null)
			mController.onDestroy();

		ActivityHelper.onDestroy(this);
		super.onDestroy();

		// com.android.debug.hv.ViewServer.get(this).removeWindow(this);
	}

	@Override
	public void onTranslationChanged() {
		if (getMainDefinition() != null && Strings.hasValue(getMainDefinition().getCaption())) {
			DataViewHelper.setTitle(this, null, getMainDefinition().getCaption());
		}
	}

	public boolean isPaused()
	{
		return mPaused;
	}

	public LayoutFragment getMainFragment() {
		return mMainFragment;
	}
}
