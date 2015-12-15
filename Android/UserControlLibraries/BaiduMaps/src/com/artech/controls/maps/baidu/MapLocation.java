package com.artech.controls.maps.baidu;

import com.artech.controls.maps.common.IMapLocation;
import com.baidu.platform.comapi.basestruct.GeoPoint;

public class MapLocation implements IMapLocation
{
	private final GeoPoint mPoint;

	public MapLocation(GeoPoint point)
	{
		mPoint = point;
	}

	public MapLocation(double latitude, double longitude)
	{
		this(new GeoPoint((int)(latitude * 1E6), (int)(longitude * 1E6)));
	}

	public static MapLocation from(GeoPoint point)
	{
		if (point != null)
			return new MapLocation(point);
		else
			return null;
	}

	@Override
	public boolean equals(Object o)
	{
		if (super.equals(o))
			return true;

		if (!(o instanceof MapLocation))
			return false;

		MapLocation other = (MapLocation)o;
		return mPoint.equals(other.mPoint);
	}

	@Override
	public int hashCode()
	{
		return mPoint.hashCode();
	}

	@Override
	public String toString()
	{
		return mPoint.toString();
	}

	GeoPoint getGeoPoint() { return mPoint; }

	@Override
	public double getLatitude() { return mPoint.getLatitudeE6() / 1E6; }

	@Override
	public double getLongitude() { return mPoint.getLongitudeE6() / 1E6; }
}
