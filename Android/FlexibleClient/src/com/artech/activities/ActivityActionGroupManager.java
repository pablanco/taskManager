package com.artech.activities;

import java.util.HashMap;

import android.app.Activity;

import com.artech.controls.IGxControl;
import com.artech.controls.actiongroup.ActionBarMerger;
import com.artech.controls.actiongroup.ActionGroupManager;
import com.artech.fragments.IDataView;

class ActivityActionGroupManager
{
	private final HashMap<IDataView, ActionGroupManager> mActionGroups;
	private final ActionBarMerger mActionBars;

	public ActivityActionGroupManager(Activity activity)
	{
		mActionGroups = new HashMap<IDataView, ActionGroupManager>();
		mActionBars = new ActionBarMerger(activity);
	}

	public void addDataView(IDataView dataView)
	{
		mActionGroups.put(dataView, new ActionGroupManager(dataView));
		mActionBars.add(dataView);
	}

	public void removeDataView(IDataView dataView)
	{
		ActionGroupManager groupManager = mActionGroups.get(dataView);
		if (groupManager != null)
		{
			groupManager.onCloseDataView();
			mActionGroups.remove(dataView);
		}

		mActionBars.remove(dataView);
	}

	public void removeAll()
	{
		for (ActionGroupManager groupManager : mActionGroups.values())
			groupManager.onCloseDataView();

		mActionGroups.clear();
		mActionBars.clear();
	}

	public IGxControl getControl(IDataView dataView, String controlName)
	{
		if (dataView != null)
		{
			ActionGroupManager groupManager = mActionGroups.get(dataView);
			if (groupManager != null)
			{
				IGxControl groupControl = groupManager.getControl(controlName);
				if (groupControl != null)
					return groupControl;
			}

			IGxControl actionBarControl = mActionBars.getControl(dataView, controlName);
			if (actionBarControl != null)
				return actionBarControl;
		}

		return null;
	}

	ActionBarMerger getActionBar()
	{
		return mActionBars;
	}
}
