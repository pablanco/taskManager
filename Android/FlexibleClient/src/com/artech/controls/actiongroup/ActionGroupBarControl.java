package com.artech.controls.actiongroup;

import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.layout.ActionGroupDefinition;
import com.artech.compatibility.SherlockHelper;
import com.artech.controls.IGxControl;
import com.artech.fragments.IDataView;

class ActionGroupBarControl extends ActionGroupBaseControl
{
	private final MenuItemManager mItemManager;
	private ActionMode mCurrentBar;

	public ActionGroupBarControl(IDataView dataView, ActionGroupDefinition definition)
	{
		super(dataView, definition);
		mItemManager = new MenuItemManager(dataView.getUIContext());
		mItemManager.setDefinition(definition);
	}

	@Override
	protected void showActionGroup()
	{
		mCurrentBar = SherlockHelper.startActionMode(getActivity(), mActionModeCallback);
		mCurrentBar.setTitle(getCaption());
	}

	@Override
	protected void hideActionGroup()
	{
		finishCurrentActionMode();
	}

	private final ActionMode.Callback mActionModeCallback = new ActionMode.Callback()
	{
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu)
		{
			mItemManager.initializeMenu(menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu)
		{
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item)
		{
			ActionDefinition action = mItemManager.getItemEvent(item.getItemId());
			if (action != null)
				runAction(action);

			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) { }
	};

	private void finishCurrentActionMode()
	{
		if (mCurrentBar != null)
		{
			mCurrentBar.finish();
			mCurrentBar = null;
		}

		mItemManager.finalizeMenu();
	}

	@Override
	public void setCaption(String caption)
	{
		super.setCaption(caption);
		if (mCurrentBar != null)
			mCurrentBar.setTitle(caption);
	}

	@Override
	public IGxControl getControl(String name)
	{
		return mItemManager.getControl(name);
	}
}
