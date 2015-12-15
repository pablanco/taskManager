package com.artech.controls.maps.common;

import com.artech.base.model.Entity;

public abstract class MapItemBase<LOCATION_TYPE extends IMapLocation>
{
	private final LOCATION_TYPE mLocation;
	private final Entity mData;

	protected MapItemBase(LOCATION_TYPE location, Entity itemData)
	{
		mLocation = location;
		mData = itemData;
	}

	public LOCATION_TYPE getLocation()
	{
		return mLocation;
	}

	public Entity getData()
	{
		return mData;
	}
}
