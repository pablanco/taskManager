package com.artech.base.metadata.layout;

import java.io.Serializable;

import com.artech.base.serialization.INodeObject;

public class ActionBarDefinition extends ActionGroupDefinition implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private boolean mIsVisible;

	ActionBarDefinition(LayoutDefinition parent, INodeObject jsonActionBar)
	{
		super(parent, jsonActionBar);
		mIsVisible = true;

		if (jsonActionBar != null) {
			mThemeClass = jsonActionBar.optString("@applicationBarsClass", "ApplicationBars"); //$NON-NLS-1$ $NON-NLS-2$
			mIsVisible = jsonActionBar.optBoolean("@showApplicationBars", true); //$NON-NLS-1$
		}
	}

	public boolean isVisible()
	{
		return mIsVisible;
	}
}
