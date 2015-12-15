package com.artech.controls.actiongroup;

import java.util.ArrayList;
import java.util.BitSet;

import android.app.Activity;
import android.view.Menu;
import android.view.View;

import com.artech.actions.ICustomMenuManager;
import com.artech.base.metadata.ActionDefinition;
import com.artech.controls.IGxControl;
import com.artech.fragments.IDataView;
import com.artech.ui.Anchor;

public class ActionBarMerger
{
	private final Activity mActivity;
	private final ArrayList<DataViewActionBar> mActionBars;
	private final BitSet mActionBarIds;

	public ActionBarMerger(Activity activity)
	{
		mActionBars = new ArrayList<DataViewActionBar>();
		mActionBarIds = new BitSet();
		mActivity = activity;
	}

	public void add(IDataView dataView)
	{
		// Remove if present before.
		remove(dataView);
		addActionBarFor(dataView);
	}

	private DataViewActionBar addActionBarFor(IDataView dataView)
	{
		int barId = mActionBarIds.nextClearBit(0);
		DataViewActionBar actionBar = new DataViewActionBar(dataView, barId);

		mActionBars.add(actionBar);
		mActionBarIds.set(barId);

		return actionBar;
	}

	public void remove(IDataView dataView)
	{
		for (DataViewActionBar actionBar : mActionBars)
		{
			if (actionBar.mDataView == dataView)
			{
				mActionBars.remove(actionBar);
				mActionBarIds.clear(actionBar.mId);
				break;
			}
		}
	}

	public void clear()
	{
		mActionBars.clear();
		mActionBarIds.clear();
	}

	public IGxControl getControl(IDataView dataView, String controlName)
	{
		DataViewActionBar actionBar = getActionBar(dataView);
		if (actionBar != null)
			return actionBar.getControl(controlName);
		else
			return null;
	}

	public void initializeMenu(Menu menu, Iterable<IDataView> dataViews)
	{
		for (IDataView dataView : dataViews)
		{
			DataViewActionBar actionBar = getActionBar(dataView);
			if (actionBar == null)
				actionBar = addActionBarFor(dataView);

			if (actionBar != null)
				actionBar.initializeMenu(menu);

			if (dataView instanceof ICustomMenuManager)
				((ICustomMenuManager)dataView).onCustomCreateOptionsMenu(menu);
		}
	}

	public boolean onOptionsItemSelected(int itemId)
	{
		int barId = (itemId & ActionBarMenuItemManager.GROUP_ID_MASK) / ActionBarMenuItemManager.GROUP_ID_FACTOR;
		DataViewActionBar actionBar = getActionBar(barId);
		if (actionBar != null)
		{
			ActionDefinition event = actionBar.getItemEvent(itemId);
			if (event != null)
			{
				// The menu item that was pressed acts as anchor for the action.
				Anchor anchor = null;
				View menuItemView = mActivity.findViewById(itemId);
				if (menuItemView != null)
					anchor = new Anchor(menuItemView);

				actionBar.mDataView.runAction(event, anchor);
				return true;
			}
		}

		return false;
	}

	private class DataViewActionBar
	{
		private final IDataView mDataView;
		private final int mId;
		private ActionBarMenuItemManager mItemManager;

		public DataViewActionBar(IDataView dataView, int barId)
		{
			mDataView = dataView;
			mId = barId;
			initializeItemManager();
		}

		private void initializeItemManager()
		{
			if (mItemManager == null && mDataView.getLayout() != null)
				mItemManager = new ActionBarMenuItemManager(mActivity, mDataView, mId);
		}

		public ActionDefinition getItemEvent(int itemId)
		{
			initializeItemManager();
			if (mItemManager != null)
				return mItemManager.getItemEvent(itemId);
			else
				return null;
		}

		public void initializeMenu(Menu menu)
		{
			initializeItemManager();
			if (mItemManager != null)
				mItemManager.initializeMenu(menu);
		}

		public IGxControl getControl(String controlName)
		{
			initializeItemManager();
			if (mItemManager != null)
				return mItemManager.getControl(controlName);
			else
				return null;
		}
	}

	private DataViewActionBar getActionBar(IDataView dataView)
	{
		for (DataViewActionBar actionBar : mActionBars)
		{
			if (actionBar.mDataView == dataView)
				return actionBar;
		}

		return null;
	}

	private DataViewActionBar getActionBar(int barId)
	{
		for (DataViewActionBar actionBar : mActionBars)
		{
			if (actionBar.mId == barId)
				return actionBar;
		}

		return null;
	}
}
