package com.artech.android.layout;

import android.content.Context;

import com.artech.base.metadata.layout.TableDefinition;
import com.artech.ui.Coordinator;

public class GxLayoutInTab extends GxLayout
{
	private boolean mHasScroll;
	private boolean mIsActiveTab;
	
	public GxLayoutInTab(Context context, TableDefinition layout, Coordinator coordinator)
    {
		super(context, layout, coordinator);
	}

	public void setHasScroll(boolean hasScroll)
	{
		mHasScroll = hasScroll;
	}

	public boolean getHasScroll()
	{
		return mHasScroll;
	}

	public boolean isActiveTab()
	{
		return mIsActiveTab;
	}

	public void setIsActiveTab(boolean value)
	{
		mIsActiveTab = value;
	}

}