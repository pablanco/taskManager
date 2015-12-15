package com.artech.fragments;

import java.util.HashMap;

import com.artech.base.model.Entity;

class LayoutFragmentState
{
	private final int mId;
	private Entity mData;
	private final HashMap<String, Object> mProperties;

	public LayoutFragmentState(int id, Entity data)
	{
		mId = id;
		mData = data;
		mProperties = new HashMap<String, Object>();
	}

	public int getId() { return mId; }
	public Entity getData() { return mData; }
	public Object getProperty(String key) { return mProperties.get(key); }

	public void setData(Entity data) { mData = data; }
	public void setProperty(String key, Object value) { mProperties.put(key, value); }
}
