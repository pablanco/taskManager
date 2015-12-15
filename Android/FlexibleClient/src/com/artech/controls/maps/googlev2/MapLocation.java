package com.artech.controls.maps.googlev2;

import java.util.ArrayList;
import java.util.List;

import com.artech.controls.maps.common.IMapLocation;
import com.google.android.gms.maps.model.LatLng;

class MapLocation implements IMapLocation
{
	private final LatLng mLatLng;

	public MapLocation(LatLng latLng)
	{
		mLatLng = latLng;
	}

	public MapLocation(double latitude, double longitude)
	{
		this(new LatLng(latitude, longitude));
	}

	public static MapLocation from(LatLng latLng)
	{
		if (latLng != null)
			return new MapLocation(latLng);
		else
			return null;
	}

	public static List<MapLocation> listFrom(List<LatLng> latLngs)
	{
		ArrayList<MapLocation> list = new ArrayList<MapLocation>();
		for (LatLng latLng : latLngs)
			list.add(new MapLocation(latLng));

		return list;
	}

	public static List<LatLng> listToLatLng(List<IMapLocation> locations)
	{
		ArrayList<LatLng> list = new ArrayList<LatLng>();
		for (IMapLocation location : locations)
			list.add(((MapLocation)location).getLatLng());

		return list;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (super.equals(o))
			return true;

		if (!(o instanceof MapLocation))
			return false;

		MapLocation other = (MapLocation)o;
		return mLatLng.equals(other.mLatLng);
	}

	@Override
	public int hashCode()
	{
		return mLatLng.hashCode();
	}

	@Override
	public String toString()
	{
		return mLatLng.toString();
	}

	LatLng getLatLng() { return mLatLng; }

	@Override
	public double getLatitude() { return mLatLng.latitude; }

	@Override
	public double getLongitude() { return mLatLng.longitude; }
}
