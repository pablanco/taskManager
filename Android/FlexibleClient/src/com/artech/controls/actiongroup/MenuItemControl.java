package com.artech.controls.actiongroup;

import android.view.MenuItem;

import com.artech.base.metadata.layout.ActionGroupItemDefinition;

class MenuItemControl extends ActionGroupBaseItemControl<MenuItemControl>
{
	private MenuItem mMenuItem;
	private final int mMenuId;
	private OnRequestLayoutListener mOnRequestLayoutListener;

	public MenuItemControl(ActionGroupItemDefinition definition, int menuId)
	{
		super(definition);
		mMenuId = menuId;
	}

	public int getMenuId()
	{
		return mMenuId;
	}

	public void setMenuItem(MenuItem menuItem)
	{
		mMenuItem = menuItem;
	}

	public interface OnRequestLayoutListener
	{
		void onRequestLayout(MenuItemControl item);
	}

	public void setOnRequestLayoutListener(OnRequestLayoutListener listener)
	{
		mOnRequestLayoutListener = listener;
	}

	@Override
	public void requestLayout()
	{
		super.requestLayout();
		if (mOnRequestLayoutListener != null)
			mOnRequestLayoutListener.onRequestLayout(this);
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		if (mMenuItem != null)
			mMenuItem.setEnabled(enabled);
	}

	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		if (mMenuItem != null)
			mMenuItem.setVisible(visible);
	}

	@Override
	public void setCaption(String caption)
	{
		super.setCaption(caption);
		if (mMenuItem != null)
			mMenuItem.setTitle(caption);
	}
}
