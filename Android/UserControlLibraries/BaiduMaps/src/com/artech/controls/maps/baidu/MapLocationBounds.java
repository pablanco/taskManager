package com.artech.controls.maps.baidu;

import com.artech.controls.maps.common.IMapLocationBounds;
import com.baidu.platform.comapi.basestruct.GeoPoint;

public class MapLocationBounds implements IMapLocationBounds<MapLocation>
{
	private int mMinimumLatitude;
	private int mMaximumLatitude;
	private int mMinimumLongitude;
	private int mMaximumLongitude;

	public MapLocationBounds(MapLocation southwest, MapLocation northeast)
	{
		mMinimumLatitude = southwest.getGeoPoint().getLatitudeE6();
		mMinimumLongitude = southwest.getGeoPoint().getLongitudeE6();
		mMaximumLatitude = northeast.getGeoPoint().getLatitudeE6();
		mMaximumLongitude = northeast.getGeoPoint().getLongitudeE6();
	}

	@Override
	public MapLocation southwest()
	{
		return new MapLocation(new GeoPoint(mMinimumLatitude, mMinimumLongitude));
	}

	@Override
	public MapLocation northeast()
	{
		return new MapLocation(new GeoPoint(mMaximumLatitude, mMaximumLongitude));
	}

	public int getLatitudeSpan()
	{
		return mMaximumLatitude - mMinimumLatitude;
	}

	public int getLongitudeSpan()
	{
		return mMaximumLongitude - mMinimumLongitude;
	}

	public GeoPoint getCenter()
	{
		return new GeoPoint((mMinimumLatitude + mMaximumLatitude) / 2, (mMinimumLongitude + mMaximumLongitude) / 2);
	}
}
