package com.artech.activities;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;

import com.artech.R;
import com.artech.actions.ActionExecution;
import com.artech.actions.ActionFactory;
import com.artech.actions.ActionParameters;
import com.artech.actions.ActionResult;
import com.artech.actions.ICustomActionRunner;
import com.artech.actions.UIContext;
import com.artech.android.analytics.Tracker;
import com.artech.android.layout.OrientationLock;
import com.artech.app.ComponentId;
import com.artech.app.ComponentParameters;
import com.artech.application.MyApplication;
import com.artech.base.controls.IGxControlActivityLauncher;
import com.artech.base.controls.IGxControlNotifyEvents;
import com.artech.base.controls.IGxControlNotifyEvents.EventType;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.Events;
import com.artech.base.metadata.GenexusApplication;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.enums.RequestCodes;
import com.artech.base.metadata.loader.ApplicationLoader;
import com.artech.base.metadata.loader.LoadResult;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityHelper;
import com.artech.base.providers.GxUri;
import com.artech.base.services.Services;
import com.artech.common.ApplicationHelper;
import com.artech.common.IntentHelper;
import com.artech.common.SecurityHelper;
import com.artech.controllers.AutoRefreshManager;
import com.artech.controllers.DataSourceController;
import com.artech.controllers.DataViewController;
import com.artech.controllers.IDataSourceController;
import com.artech.controllers.IDataViewController;
import com.artech.controls.GxVideoView;
import com.artech.controls.IGxControl;
import com.artech.controls.IGxEdit;
import com.artech.fragments.IDataView;
import com.artech.fragments.LayoutFragmentActivityState;
import com.artech.providers.EntityDataProvider;
import com.artech.utils.Cast;

public class ActivityController
{
	// Model & View
	private final ActivityModel mModel;
	private final Activity mActivity;

	// An activity may display multiple data views at once. They are tracked here.
	// LinkedHashMap is used to preserve DV order.
	private final LinkedHashMap<IDataView, DataViewController> mControllers;
	private ActivityControllerState mRestoredState;

	// Current Context
	private boolean mIsRunning;
	private Refresh mPendingRefresh;
	private final AutoRefreshManager mAutoRefreshManager;

	// Actions
	private final ActivityMenuManager mMenuManager;
	private final ActivityActionGroupManager mActionGroupManager;
	private IGxControlActivityLauncher mLastActivityLauncher;

	// Progress dialog shown while loading metadata.
	private ProgressDialog mProgressDialog = null;

	public ActivityController(Activity activity)
	{
		mActivity = activity;
		mModel = new ActivityModel();
		mControllers = new LinkedHashMap<IDataView, DataViewController>();

		mActionGroupManager = new ActivityActionGroupManager(activity);
		mMenuManager = new ActivityMenuManager(this, mActionGroupManager);

		mAutoRefreshManager = new AutoRefreshManager(this);
	}

	public boolean initializeFrom(Intent intent)
	{
		Bundle intentData = intent.getExtras();
		return mModel.initializeFrom(mActivity, intentData);
	}

	public ActivityModel getModel() { return mModel; }

	public IDataViewController getController(UIContext context, IDataView dataView, ComponentId id, ComponentParameters params)
	{
		DataViewController controller = mControllers.get(dataView);
		if (controller == null)
		{
			// See if we can restore this controller from the previous state (e.g. before rotating).
			if (mRestoredState != null)
				controller = mRestoredState.restoreController(id, params, this, dataView);

			if (controller == null)
				controller = new DataViewController(this, id, mModel.createDataView(context, params), dataView);

			mControllers.put(dataView, controller);
			mActionGroupManager.addDataView(dataView);
		}

		return controller;
	}

	public void remove(IDataView dataView)
	{
		DataViewController controller = mControllers.get(dataView);
		if (controller != null)
		{
			controller.onPause();
			mAutoRefreshManager.removeAll(controller.getDataSources());

			notifyControlEvent(dataView.getUIContext(), EventType.ACTIVITY_PAUSED);
			notifyControlEvent(dataView.getUIContext(), EventType.ACTIVITY_DESTROYED);

			mControllers.remove(dataView);
			mActionGroupManager.removeDataView(dataView);
		}
	}

	IDataSourceController getDataSource(int id)
	{
		// One of the controllers should have this data source.
		for (IDataViewController dataView : mControllers.values())
		{
			IDataSourceController dataSource = dataView.getDataSource(id);
			if (dataSource != null)
				return dataSource;
		}

		return null;
	}

	public void onResume()
	{
		// Notify controls that the activity has been resumed.
		for (IDataView dataView : getGxActivity().getActiveDataViews())
			notifyControlEvent(dataView.getUIContext(), EventType.ACTIVITY_RESUMED);

		// Update search.
		Pair<IDataSourceController, String> searchInfo = SearchHelper.getCurrentSearch(this);
		if (searchInfo != null)
		{
			IDataSourceController dataSource = searchInfo.first;

			GxUri uri = dataSource.getModel().getUri();
			dataSource.getModel().setUri(uri.setSearch(searchInfo.second));
			onRefresh(dataSource, false);
		}

		mIsRunning = true;
		for (DataViewController controller : mControllers.values())
			controller.onResume();

		// If a refresh was asked when the activity was dormant, do it now.
		if (mPendingRefresh != null)
			mPendingRefresh.execute();

		mAutoRefreshManager.onResume();
	}

	private void afterPause()
	{
		mAutoRefreshManager.onPause();

		for (DataViewController controller : mControllers.values())
			controller.onPause();

		mIsRunning = false;
	}

	public void onPause()
	{
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
		}

		// Notify controls that the activity has been paused.
		for (IDataView dataView : getGxActivity().getActiveDataViews())
			notifyControlEvent(dataView.getUIContext(), EventType.ACTIVITY_PAUSED);

		afterPause();
	}

	public void onDestroy()
	{
		// Notify controls that the activity will be destroyed.
		for (IDataView dataView : getGxActivity().getActiveDataViews())
			notifyControlEvent(dataView.getUIContext(), EventType.ACTIVITY_DESTROYED);

		afterPause();

		mActionGroupManager.removeAll();
		mAutoRefreshManager.onDestroy();
		mControllers.clear();
	}

	public boolean isRunning() { return mIsRunning; }

	/**
	 * Used to signal the ActivityController that a new data controller has been created.
	 */
	public void track(IDataSourceController controller)
	{
		mAutoRefreshManager.addDataSource(controller);
	}

	/**
	 * Refresh all data sources in the activity
	 * @param keepPosition If true, will ask for the same records that are currently loaded (instead of the first n).
	 */
	public void onRefresh(boolean keepPosition)
	{
		onRefresh(new Refresh(keepPosition));
	}

	/**
	 * Refresh a data view in the activity (possibly including its children too).
	 * @param dataView Data view to refresh.
	 * @param keepPosition If true, will ask for the same records that are currently loaded (instead of the first n).
	 */
	public void onRefresh(IDataViewController dataView, boolean keepPosition, boolean includeChildren)
	{
		onRefresh(new Refresh(dataView, keepPosition, includeChildren));
	}

	/**
	 * Refresh a particular data source in the activity.
	 * @param dataSource Data source to refresh.
	 * @param keepPosition If true, will ask for the same records that are currently loaded (instead of the first n).
	 */
	public void onRefresh(IDataSourceController dataSource, boolean keepPosition)
	{
		onRefresh(new Refresh(dataSource, keepPosition));
	}

	/**
	 * Executes a refresh operation; or saves it for later if the controller is currently disabled.
	 */
	private void onRefresh(Refresh refresh)
	{
		// Execute the refresh if controllers are enabled; save it as pending otherwise.
		if (mIsRunning)
			refresh.execute();
		else
			mPendingRefresh = refresh;
	}

	/**
	 * Class for a pending refresh operation.
	 */
	private class Refresh
	{
		/**
		 * Refresh of the whole activity.
		 */
		public Refresh(boolean keepPosition)
		{
			mDataView = null;
			mDataSource = null;
			mKeepPosition = keepPosition;
			mIncludeChildren = true;
		}

		/**
		 * Refresh of a particular data view.
		 */
		public Refresh(IDataViewController dataView, boolean keepPosition, boolean includeChildren)
		{
			mDataView = dataView;
			mDataSource = null;
			mKeepPosition = keepPosition;
			mIncludeChildren = includeChildren;
		}

		/**
		 * Refresh of a particular data source.
		 */
		public Refresh(IDataSourceController dataSource, boolean keepPosition)
		{
			mDataView = null;
			mDataSource = dataSource;
			mKeepPosition = keepPosition;
			mIncludeChildren = false;
		}

		private final IDataViewController mDataView;
		private final IDataSourceController mDataSource;
		private final boolean mKeepPosition;
		private final boolean mIncludeChildren;

		public void execute()
		{
			mPendingRefresh = null;
			List<DataViewController> componentsToRefresh = new ArrayList<>();

			if (mDataSource != null && mDataSource instanceof DataSourceController)
			{
				// Specific refresh for a data source.
				((DataSourceController)mDataSource).onRefresh(mKeepPosition);
			}
			else if (mDataView != null && mDataView instanceof DataViewController)
			{
				// Specific refresh for a data view (possibly its children).
				componentsToRefresh.add((DataViewController)mDataView);
				if (mIncludeChildren)
				{
					for (DataViewController controller : mControllers.values())
						if (controller.getId().isDescendantOf(mDataView.getId()))
							componentsToRefresh.add(controller);
				}
			}
			else
			{
				// Generic refresh, all components in the activity.
				componentsToRefresh = new ArrayList<>(mControllers.values());
			}

			for (DataViewController controller : componentsToRefresh)
				controller.onRefresh(mKeepPosition);

			// Also notify controls that a refresh has occurred.
			for (IDataView dataView : getGxActivity().getActiveDataViews())
				notifyControlEvent(dataView.getUIContext(), EventType.REFRESH);
		}
	}

	public Activity getActivity() { return mActivity; }
	public IGxActivity getGxActivity() { return (IGxActivity)mActivity; }

	public void runAction(UIContext context, ActionDefinition action, Entity entity)
	{
		if (action == null)
			return;

		// Notify controls that wish to be invoked when an action is about to run.
		notifyControlEvent(context, EventType.ACTION_CALLED);

		// Analytics tracking.
		Tracker.onAction(context, action);

		// Execute action.
		runAction(context, action, prepareActionParameters(entity));
	}

	private void notifyControlEvent(UIContext context, EventType event)
	{
		List<IGxControlNotifyEvents> notifiableControls = context.getViews(IGxControlNotifyEvents.class);
		for (IGxControlNotifyEvents control : notifiableControls)
			control.notifyEvent(event);
	}

	private void runAction(final UIContext context, final ActionDefinition action, final ActionParameters parameters)
	{
		// Ask the activity if it wants to handle this action in a special way.
		ICustomActionRunner customRunner = Cast.as(ICustomActionRunner.class, mActivity);
		if (customRunner != null && customRunner.runAction(action, parameters.getEntity()))
			return; // Already handled.

		// Lock activity orientation while action is running (released by ActionExecution.onEndEvent()).
		OrientationLock.lock(mActivity, OrientationLock.REASON_RUN_EVENT);

		ActionExecution exec = new ActionExecution(ActionFactory.getAction(context, action, parameters));
		exec.executeAction();
	}

	private static ActionParameters prepareActionParameters(Entity from)
	{
		// Get the "TRUE" root entity for executing the action.
		// For a normal Entity (e.g. the Form entity, or an entity in a grid row with a DP) it's the same one.
		// For a "member" entity (e.g. an SDT variable or an SDT collection item) it's the first parent entity
		// that is not a member itself (i.e. one of the "normal" cases outlined above).
		Entity root = EntityHelper.forEvaluation(from);

		if (root != null)
			return new ActionParameters(root);
		else
			return new ActionParameters(from);
	}

	public boolean onSearchRequested()
	{
		mMenuManager.onSearchRequested();
		return true;
	}

	public void updateLoadingStatus(boolean loading)
	{
		mMenuManager.updateLoadingStatus(loading);
	}

	public boolean onCreateOptionsMenu(Menu menu)
	{
		mMenuManager.onCreateOptionsMenu(menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		return mMenuManager.onOptionsItemSelected(item);
	}

	public IGxControl getControl(IDataView dataView, String controlName)
	{
		// Search in action bar and action groups (either the groups themselves or their controls).
		IGxControl groupControl = mActionGroupManager.getControl(dataView, controlName);
		if (groupControl != null)
			return groupControl;

		return null;
	}

	public boolean handleOnBackPressed()
	{
		Iterable<IDataView> activeDataViews = getGxActivity().getActiveDataViews();
		return handleOnBackPressed(activeDataViews);
	}

	public boolean handleOnBackPressed(IDataView dataView)
	{
		ArrayList<IDataView> activeDataViews = new ArrayList<IDataView>();
		activeDataViews.add(dataView);
		return handleOnBackPressed(activeDataViews);
	}

	private boolean handleOnBackPressed(Iterable<IDataView> activeDataViews)
	{
		if (handleOnBackPressedAsRefresh(activeDataViews))
			return true;

		return handleOnBackPressedWithEvent(activeDataViews);

	}

	private boolean handleOnBackPressedAsRefresh(Iterable<IDataView> activeDataViews)
	{
		// Use back to remove filters/search (if present) instead of going back to previous activity.
		// Iterate over all ACTIVE data views, and prevent back if ANY of them had filters/search.
		boolean handledAsRefresh = false;
		for (IDataView dataView : activeDataViews)
		{
			IDataViewController dvController = mControllers.get(dataView);
			if (dvController != null)
			{
				boolean refreshDataView = false;
				for (IDataSourceController dsController : dvController.getDataSources())
				{
					GxUri dsUri = dsController.getModel().getUri();
					if (dsUri != null && (dsUri.resetFilter() || dsUri.resetSearch()))
					{
						dsController.getModel().setUri(dsUri);
						refreshDataView = true;
					}
				}

				if (refreshDataView)
					onRefresh(dvController, false, false);

				handledAsRefresh |= refreshDataView;
			}
		}

		return handledAsRefresh;
	}

	private boolean handleOnBackPressedWithEvent(Iterable<IDataView> activeDataViews)
	{
		for (IDataView dataView : activeDataViews)
		{
			IViewDefinition definition = dataView.getDefinition();
			ActionDefinition backEvent = definition.getEvent(Events.BACK);
			if (backEvent != null)
			{
				dataView.runAction(backEvent, null);
				return true; // Runs only the FIRST Back event in case there are multiple ones.
			}
		}

		return false; // No Back event found.
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		boolean avoidRefresh = false;

		// See if a particular control started this activity, and let it handle the result.
		if (mLastActivityLauncher != null && mLastActivityLauncher.handleOnActivityResult(requestCode, resultCode, data))
		{
			mLastActivityLauncher = null;
			return;
		}

		if (resultCode == Activity.RESULT_OK || requestCode == RequestCodes.ACTION_ALWAYS_SUCCESSFUL)
		{
			if (requestCode == RequestCodes.PICKER)
			{
				//TODO: this need to be changed and use other method to get the PickingElementId
				String pickingElementId = GxBaseActivity.PickingElementId;
				for (IDataView dataView : mControllers.keySet())
				{
					List<IGxEdit> list = dataView.getUIContext().findControlsBoundTo(pickingElementId);
					if (list.size() >= 1)
						list.get(0).setValueFromIntent(data);
				}
			}

			if (requestCode == RequestCodes.FILTERS && data != null)
			{
				processFilterRequest(data);
				return;
			}

			if (ActivityHelper.isActionRequest(requestCode))
			{
				// An action continuation
				ActionResult actionResult = ActionExecution.continueCurrentFromActivityResult(requestCode, resultCode, data, mActivity);
				avoidRefresh = (actionResult == ActionResult.SUCCESS_CONTINUE_NO_REFRESH);
			}
			else if (requestCode == RequestCodes.LOGIN)
			{
				// TODO: Reload data?
			}
			else if (requestCode == RequestCodes.PREFERENCE)
			{
				String serverUrl = data.getStringExtra(IntentParameters.ServerURL);
				if (serverUrl != null) {
					new UpdateAppUrlTask().execute(serverUrl);
				}
			}
		}
		else if (requestCode == Activity.RESULT_FIRST_USER) {
			// Start of user-defined activity results.
			for (IDataView dataView : getGxActivity().getActiveDataViews())
			{
				List<GxVideoView> videoControls = dataView.getUIContext().getViews(GxVideoView.class);
				for (GxVideoView videoControl : videoControls)
				{
					videoControl.retryYoutubeInitialization();
				}
			}
		}
		else
		{
			// Not Activity.RESULT_OK, so perform tasks related to errors.
			if ( ActivityHelper.isActionRequest(requestCode) )
			{
				// Clean pending actions if one of them failed.
				ActionExecution.cleanCurrentOrLastPendingActionFromActivityResult(requestCode, resultCode, data, mActivity);
			}
			else if (requestCode == RequestCodes.LOGIN)
			{
				if (resultCode == ActivityFlowControl.RETURN_TO_LOGIN)
					return;

				// finish ww?
				mActivity.finish();
				return;
			}
		}

		// Returning from any other activity - refresh if necessary
		// (UNLESS returning from picker - refreshing would lose inserted data).
		if (!avoidRefresh && requestCode != RequestCodes.PICKER && requestCode != RequestCodes.ACTIONNOREFRESH)
			onRefresh(true);
	}

	// Updates the app's server url & reloads the metadata from the new server url.
	private class UpdateAppUrlTask extends AsyncTask<String, Void, Void> {

		@Override
		protected void onPreExecute() {
			mProgressDialog = ProgressDialog.show(mActivity, mActivity.getResources().getText(R.string.GXM_Loading), mActivity.getResources().getText(R.string.GXM_PleaseWait), true);
		}

		@Override
		protected Void doInBackground(String... params) {
			String serverUrl = params[0];

			GenexusApplication application = MyApplication.getApp();
			ApplicationLoader.MetadataReady = false;

			if (application.isSecure()) {
				SecurityHelper.logout();
			} else {
				EntityDataProvider.clearAllCaches();
			}

			Services.Log.info("GenexusActivity", "App url modified. New Value: " + serverUrl); //$NON-NLS-1$ $NON-NLS-2$
			application.setAPIUri(updateUri(serverUrl));

			loadApplication(application);

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			mProgressDialog.dismiss();
			ActivityLauncher.callApplicationMain(mActivity, true, true);
		}

	}

	private void loadApplication(GenexusApplication application)
	{
		//Lock screen
		OrientationLock.lock(mActivity, OrientationLock.REASON_LOAD_METADATA);

		LoadResult loadResult;
		try
		{
			// Load the Application.
			loadResult = Services.Application.initialize();
		}
		catch (Exception ex)
		{
			// Uncaught exception, possibly "out of memory".
			loadResult = LoadResult.error(ex);
		}
		finally
		{
			OrientationLock.unlock(mActivity, OrientationLock.REASON_LOAD_METADATA);
		}

		if (loadResult.getCode() == LoadResult.RESULT_OK && application.getUseDynamicUrl())
		{
			if (!ApplicationLoader.MetadataReady ||	(ApplicationLoader.MetadataReady &&
					!ApplicationHelper.checkApplicationUri(application.getAPIUri())))
			{
				ActivityLauncher.callPreferences(mActivity, true, R.string.GXM_ServerUrlIncorrect, application.getAPIUri());
			}
		}
	}

	private static String updateUri(String serverUrl)
	{
		if (serverUrl.contains("://")) //$NON-NLS-1$
			return serverUrl;
		else
			return "http://".concat(serverUrl); //$NON-NLS-1$
	}

	private void processFilterRequest(Intent data)
	{
		// Replace the Uri of the data source with the one with filter information.
		IDataSourceController dataSource = getDataSource(data.getIntExtra(IntentParameters.Filters.DataSourceId, 0));
		GxUri filterUri = IntentHelper.getObject(data, IntentParameters.Filters.Uri, GxUri.class);

		if (dataSource != null && filterUri != null)
		{
			// Update URI...
			dataSource.getModel().setUri(filterUri);
			dataSource.getModel().setFilterExtraInfo(data.getStringExtra(IntentParameters.Filters.FiltersFK));

			// ... and fire data load.
			onRefresh(dataSource, false);
		}
	}

	private static final String STATE_CONTROLLER_STATE = "ActivityController::DataViewControllers";

	public void saveState(LayoutFragmentActivityState state)
	{
		ActivityControllerState controllerState = new ActivityControllerState();
		controllerState.save(mControllers);
		state.setProperty(STATE_CONTROLLER_STATE, controllerState);
	}

	public void restoreState(LayoutFragmentActivityState state)
	{
		if (state != null)
			mRestoredState = (ActivityControllerState)state.getProperty(STATE_CONTROLLER_STATE);
	}

	public void setCurrentActivityLauncher(IGxControlActivityLauncher launcher)
	{
		mLastActivityLauncher = launcher;
	}
}
