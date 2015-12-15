package com.artech.ui.navigation.slide;

import java.util.HashMap;

import com.artech.app.ComponentParameters;
import com.artech.base.metadata.ActionDefinition;
import com.artech.fragments.LayoutFragmentActivityState;
import com.artech.utils.Cast;

class SlideComponents
{
	public boolean IsHub;
	private HashMap<SlideNavigation.Target, ComponentParameters> mComponents = new HashMap<SlideNavigation.Target, ComponentParameters>();
	public boolean IsLeftMainComponent;
	public ActionDefinition PendingAction;

	private final static String STATE_KEY = "Gx::SlideNavigation::Components";

	public ComponentParameters get(SlideNavigation.Target target)
	{
		return mComponents.get(target);
	}
	
	public void set(SlideNavigation.Target target, ComponentParameters params)
	{
		mComponents.put(target, params);
	}
	
	public void saveTo(LayoutFragmentActivityState state)
	{
		state.setProperty(STATE_KEY, this);
	}

	public static SlideComponents readFrom(LayoutFragmentActivityState state)
	{
		if (state != null)
			return Cast.as(SlideComponents.class, state.getProperty(STATE_KEY));
		else
			return null;
	}
}
