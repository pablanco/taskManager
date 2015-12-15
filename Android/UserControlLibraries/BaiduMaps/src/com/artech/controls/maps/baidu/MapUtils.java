package com.artech.controls.maps.baidu;

import android.location.Location;
import android.util.Pair;

import com.artech.controls.maps.GxMapViewDefinition;
import com.artech.controls.maps.common.MapUtilsBase;
import com.baidu.platform.comapi.basestruct.GeoPoint;

class MapUtils extends MapUtilsBase<MapLocation, MapLocationBounds>
{
	public MapUtils(GxMapViewDefinition definition)
	{
		super(definition);
	}

	@Override
	protected MapLocation newMapLocation(double latitude, double longitude)
	{
		return new MapLocation(latitude, longitude);
	}

	@Override
	protected MapLocationBounds newMapBounds(MapLocation southwest, MapLocation northeast)
	{
		return new MapLocationBounds(southwest, northeast);
	}

	/**
	 * Converts a {@link Location} instance to a {@link GeoPoint} one.
	 */
	public static GeoPoint locationToGeoPoint(Location location)
	{
		return new MapLocation(location.getLatitude(), location.getLongitude()).getGeoPoint();
	}

	/**
	 * Decodes a {@link GeoPoint} instance from its string representation.
	 */
	public static GeoPoint stringToGeoPoint(String str)
	{
		Pair<Double, Double> coordinates = parseGeoLocation(str);
		if (coordinates != null)
			return new GeoPoint((int)(coordinates.first * 1E6), (int)(coordinates.second * 1E6));
		else
			return null;
	}
}
