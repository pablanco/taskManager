package com.artech.controls.actiongroup;

import android.support.v7.internal.view.menu.MenuBuilder;
import android.support.v7.internal.view.menu.MenuPopupHelper;
import android.view.MenuItem;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.layout.ActionGroupDefinition;
import com.artech.compatibility.SherlockHelper;
import com.artech.controls.IGxControl;
import com.artech.fragments.IDataView;
import com.artech.ui.Anchor;

public class ActionGroupPopupControl extends ActionGroupBaseControl
{
	private final MenuItemManager mItemManager;
	private MenuPopupHelper mCurrentPopup;

	public ActionGroupPopupControl(IDataView dataView, ActionGroupDefinition definition)
	{
		super(dataView, definition);
		mItemManager = new MenuItemManager(dataView.getUIContext());
		mItemManager.setDefinition(definition);
	}

	@Override
	protected void showActionGroup()
	{
		Anchor anchor = getContext().getAnchor();
		if (anchor != null && anchor.getView() != null)
		{
			// Create the Menu.
			MenuBuilder menu = new MenuBuilder(getActivity());
			menu.setCallback(mMenuCallback);
			mItemManager.initializeMenu(menu);

			// Show it with a Popup.
			mCurrentPopup = new MenuPopupHelper(SherlockHelper.getActionBarThemedContext(getActivity()), menu, anchor.getView());
			mCurrentPopup.show();
		}
	}

	@Override
	protected void hideActionGroup()
	{
		if (mCurrentPopup != null)
		{
			mCurrentPopup.dismiss();
			mCurrentPopup = null;
		}

		mItemManager.finalizeMenu();
	}

	private final MenuBuilder.Callback mMenuCallback = new MenuBuilder.Callback()
	{
		@Override
		public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item)
		{
			ActionDefinition action = mItemManager.getItemEvent(item.getItemId());
			runAction(action);
			return true;
		}

		@Override
		public void onMenuModeChange(MenuBuilder menu) { }
	};

	@Override
	public IGxControl getControl(String name)
	{
		return mItemManager.getControl(name);
	}
}
