package com.artech.activities;

import java.util.ArrayList;

import android.content.DialogInterface;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.artech.R;
import com.artech.application.MyApplication;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.GenexusApplication;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.filter.SearchDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.UIActionHelper;
import com.artech.compatibility.SherlockHelper;
import com.artech.controllers.IDataSourceBoundView;
import com.artech.controllers.IDataSourceController;
import com.artech.controllers.IDataSourceControllerInternal;
import com.artech.controls.GxSearchView;
import com.artech.controls.actiongroup.ActionBarMerger;
import com.artech.fragments.IDataView;

class ActivityMenuManager
{
	private final ActivityController mController;
	private final ActionBarMerger mActionBarHelper;

	private ArrayList<IDataView> mActiveDataViews;
	private ActiveGrids mActiveGrids;
	private GxSearchView mSearchView;
	private boolean mShowDynUrlPref = false;

	ActivityMenuManager(ActivityController controller, ActivityActionGroupManager actionGroups)
	{
		mController = controller;
		mActionBarHelper = actionGroups.getActionBar();
	}

	public void onCreateOptionsMenu(Menu menu)
	{
		prepare();

		initialize(menu);
		addStandardActions(menu);
		addDataViewActions(menu);
	}

	private void prepare()
	{
		mActiveDataViews = new ArrayList<IDataView>();
		for (IDataView dataview : mController.getGxActivity().getActiveDataViews() )
		{
			if (dataview.isDataReady())
				mActiveDataViews.add(dataview);
		}

		mActiveGrids = new ActiveGrids(mActiveDataViews);

		// Show Dynamic Url preference if there's a main present.
		IViewDefinition main = MyApplication.getApp().getMain();
		for (IDataView dataView : mActiveDataViews) {
			if (dataView.getDefinition() == main) {
				mShowDynUrlPref = true;
				break;
			}
		}

		// Also show it if the application is using GAM and there's a login object present.
		if (MyApplication.getApp().isSecure()) {
			String loginObjectName = MyApplication.getApp().getLoginObject();
			for (IDataView dataView : mActiveDataViews) {
				String dataViewName = dataView.getDefinition().getName();
				if (Strings.hasValue(dataViewName) && dataViewName.startsWith(loginObjectName + ".")) {
					mShowDynUrlPref = true;
					break;
				}
			}
		}
	}

	private void initialize(Menu menu)
	{
		if (menu.size() == 0)
		{
			MenuInflater inflater = mController.getActivity().getMenuInflater();
			inflater.inflate(R.menu.standardmenu, menu);
		}
	}

	private void addStandardActions(Menu menu)
	{
		// 1) Search. Show if any dataview has search fields.
		MenuItem searchItem = menu.findItem(R.id.menusearch);
		if (searchItem != null)
		{
			UIActionHelper.setStandardMenuItemImage(mController.getActivity(), searchItem, ActionDefinition.STANDARD_ACTION.SEARCH);
			setItemVisible(searchItem, mActiveGrids.HasSearch, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			if (mActiveGrids.HasSearch)
			{
				mSearchView = GxSearchView.create(mController.getActivity(), searchItem);
				mSearchView.setOnSearchClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v) { runActionWithGridChooser(mPrepareSearchHandler); }
				});
			}
		}

		// 2) Filter. Show if any dataview has filter/order.
		MenuItem filterItem = menu.findItem(R.id.menufilter);
		if (filterItem != null)
		{
			filterItem.setTitle(mActiveGrids.getFilterActionText()==null?"":mActiveGrids.getFilterActionText());
			UIActionHelper.setStandardMenuItemImage(mController.getActivity(), filterItem, ActionDefinition.STANDARD_ACTION.FILTER);
			setItemVisible(filterItem, mActiveGrids.hasFilterAction(), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		}

		// 3) Dynamic Url preferences. Show if enabled in the main object.
		MenuItem preferencesItem = menu.findItem(R.id.preferences);
		GenexusApplication app = MyApplication.getApp();
		if (preferencesItem != null && app != null)
			preferencesItem.setVisible(mShowDynUrlPref && app.getUseDynamicUrl());
	}

	void onSearchRequested()
	{
		if (mSearchView != null)
			mSearchView.show();
	}

	private void addDataViewActions(Menu menu)
	{
		mActionBarHelper.initializeMenu(menu, mActiveDataViews);
	}

	private void setItemVisible(MenuItem item, boolean visible, int actionBarMode)
	{
		item.setVisible(visible);

		// Only if action bar is visible, if not put them as menu
		if (ActivityHelper.hasActionBar(mController.getActivity()))
			MenuItemCompat.setShowAsAction(item, actionBarMode);
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		int itemId = item.getItemId();

		if (itemId == android.R.id.home)
		{
			ActivityLauncher.callApplicationMain(mController.getActivity(), true, true);
			return true;
		}
		else if (itemId == R.id.menusearch)
		{
			Services.Log.debug("onOptionsItemSelected() called for R.id.menusearch: This should never happen.");
			return true;
		}
		else if (itemId == R.id.menufilter)
		{
			showFilter();
			return true;
		}
		else if (itemId == R.id.preferences)
		{
			ActivityLauncher.callPreferences(mController.getActivity(), false, R.string.GXM_ServerUrlIncorrect, MyApplication.getApp().getAPIUri());
			return true;
		}
		else
			return mActionBarHelper.onOptionsItemSelected(itemId);
	}

	public void updateLoadingStatus(boolean loading)
	{
		if (ActivityHelper.hasActionBar(mController.getActivity()))
		{
			SherlockHelper.setProgressBarIndeterminateVisibility(mController.getActivity(), loading);
		}
	}

	/**
	 * Runs an action that applies over a single grid, by making the user choose a grid
	 * first if more than one is available.
	 */
	private void runActionWithGridChooser(final IActionWithGridChooser action)
	{
		// Build a list of the grids that can have the action applied on.
		ArrayList<IDataSourceController> grids = new ArrayList<IDataSourceController>();
		for (IDataSourceController dataSource : mActiveGrids.DataSources)
			if (action.isApplicable(dataSource))
				grids.add(dataSource);

		if (grids.size() == 0)
			return;

		if (grids.size() == 1)
		{
			action.run(grids.get(0));
			return;
		}

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mController.getActivity());
		dialogBuilder.setTitle(action.getName());

		String[] names = new String[grids.size()];
		for (int i = 0; i < grids.size(); i++)
			names[i] = grids.get(i).getName();

		dialogBuilder.setItems(names, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				IDataSourceController dataSource = mActiveGrids.DataSources.get(which);
				action.run(dataSource);
			}
		});

		dialogBuilder.show();
	}

	private void showFilter()
	{
		runActionWithGridChooser(mFilterHandler);
	}

	private static class ActiveGrids
	{
		private final ArrayList<IDataSourceController> DataSources;
		private boolean HasSearch;
		private boolean HasFilter;
		private boolean HasOrderChoice;

		public ActiveGrids(Iterable<IDataView> dataViews)
		{
			DataSources = new ArrayList<IDataSourceController>();
			for (IDataView dataView : dataViews)
			{
				if (dataView.getController() != null)
				{
					for (IDataSourceController dataSource : dataView.getController().getDataSources())
					{
						IDataSourceDefinition definition = dataSource.getDefinition();
						if (dataSource.getDefinition().isCollection())
						{
							// Even in an active DataView, a bound view may not be active (e.g. if it's not shown).
							IDataSourceBoundView dataSourceView = ((IDataSourceControllerInternal)dataSource).getBoundView();
							if (dataSourceView != null && dataSourceView.isActive())
							{
								DataSources.add(dataSource);
								SearchDefinition search = definition.getFilter().getSearch();

								HasSearch |= (search != null);
								HasFilter |= definition.getFilter().hasAdvancedFilter();
								HasOrderChoice |= (definition.getOrders().size() > 1);
							}
						}
					}
				}
			}
		}

		public boolean hasFilterAction()
		{
			return (HasFilter || HasOrderChoice);
		}

		public String getFilterActionText()
		{
			if (!hasFilterAction())
				return null;

			// Set button text (filters/order/order & filters).
			if (!HasFilter && HasOrderChoice)
				return Services.Strings.getResource(R.string.GXM_Order);
			else if (HasFilter && !HasOrderChoice)
				return Services.Strings.getResource(R.string.GXM_Filter);
			else
				return Services.Strings.getResource(R.string.GXM_FilterAndOrder);
		}
	}

	private interface IActionWithGridChooser
	{
		String getName();
		boolean isApplicable(IDataSourceController dataSource);
		void run(IDataSourceController dataSource);
	}

	private final IActionWithGridChooser mPrepareSearchHandler = new IActionWithGridChooser()
	{
		@Override
		public String getName()
		{
			return Services.Strings.getResource(R.string.GX_BtnSearch);
		}

		@Override
		public boolean isApplicable(IDataSourceController dataSource)
		{
			return (dataSource.getDefinition().getFilter().getSearch() != null);
		}

		@Override
		public void run(IDataSourceController dataSource)
		{
			// Prepare search on the selected grid.
			SearchHelper.prepare(dataSource);

			if (mSearchView != null)
				mSearchView.setQueryHint(dataSource.getDefinition().getFilter().getSearch().getCaption());
		}
	};

	private final IActionWithGridChooser mFilterHandler = new IActionWithGridChooser()
	{
		@Override
		public String getName()
		{
			return mActiveGrids.getFilterActionText();
		}

		@Override
		public boolean isApplicable(IDataSourceController dataSource)
		{
			return (dataSource.getDefinition().getFilter().hasAdvancedFilter() ||
					dataSource.getDefinition().getOrders().size() > 1);
		}

		@Override
		public void run(IDataSourceController dataSource)
		{
			ActivityLauncher.callFilters(mController.getModel().getUIContext(), dataSource);
		}
	};
}
