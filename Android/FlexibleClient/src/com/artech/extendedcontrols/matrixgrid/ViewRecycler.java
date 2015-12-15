package com.artech.extendedcontrols.matrixgrid;

import java.util.HashMap;
import java.util.Stack;

import android.view.View;

class ViewRecycler<TKey, TView extends View>
{
	private HashMap<TKey, Stack<TView>> mDump;

	public ViewRecycler()
	{
		mDump = new HashMap<TKey, Stack<TView>>();
	}

	public void put(TKey viewKey, TView view)
	{
		Stack<TView> typeViews = mDump.get(viewKey);
		if (typeViews == null)
		{
			typeViews = new Stack<TView>();
			mDump.put(viewKey, typeViews);
		}

		typeViews.push(view);
	}

	public TView get(TKey viewKey)
	{
		Stack<TView> typeViews = mDump.get(viewKey);
		if (typeViews != null && ! typeViews.isEmpty())
			return typeViews.pop();
		else
			return null;
	}
}
