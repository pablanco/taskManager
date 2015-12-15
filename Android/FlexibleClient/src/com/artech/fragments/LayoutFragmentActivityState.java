package com.artech.fragments;

import java.util.HashMap;

import com.artech.base.metadata.IViewDefinition;
import com.artech.utils.Cast;

public class LayoutFragmentActivityState
{
	public static final String INSPECTOR_MODE= "InspectorMode";
	public static final String INSPECTOR_MODE_CURRENT_CONTROL_NAME = "InspectorMode_CurrentControlName";
	private HashMap<IViewDefinition, LayoutFragmentState> mFragments = new HashMap<IViewDefinition, LayoutFragmentState>();
	private HashMap<String, Object> mProperties = new HashMap<String, Object>();

	public void saveState(BaseFragment fragment)
	{
		LayoutFragmentState state = new LayoutFragmentState(fragment.getId(), fragment.getContextEntity());
		fragment.saveFragmentState(state);
		mFragments.put(fragment.getDefinition(), state);
	}

	public LayoutFragmentState getState(IViewDefinition definition)
	{
		return mFragments.get(definition);
	}

	public Object getProperty(String key)
	{
		return mProperties.get(key);
	}

	public <T> T getProperty(Class<T> type, String key)
	{
		return Cast.as(type, mProperties.get(key));
	}

	public boolean getBooleanProperty(String key, boolean defaultValue)
	{
		Boolean value = getProperty(Boolean.class, key);
		return (value != null ? value : defaultValue);
	}

	public void setProperty(String key, Object value)
	{
		mProperties.put(key, value);
	}
}
