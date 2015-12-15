package com.artech.controls.maps.googlev2;

import java.util.List;

import android.util.Pair;

import com.artech.controls.maps.GxMapViewDefinition;
import com.artech.controls.maps.common.MapUtilsBase;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

class MapUtils extends MapUtilsBase<MapLocation, MapLocationBounds>
{
	protected MapUtils(GxMapViewDefinition definition)
	{
		super(definition);
	}

	@Override
	protected MapLocation newMapLocation(double latitude, double longitude)
	{
		return new MapLocation(latitude, longitude);
	}

	@Override
	protected MapLocationBounds newMapBounds(List<MapLocation> locations)
	{
		// Override to use LatLngBounds implementation directly.
		LatLngBounds.Builder builder = LatLngBounds.builder();
		for (MapLocation location : locations)
			builder.include(location.getLatLng());

		return new MapLocationBounds(builder.build());
	}

	@Override
	protected MapLocationBounds newMapBounds(MapLocation southwest, MapLocation northeast)
	{
		LatLngBounds latLngBounds = new LatLngBounds(southwest.getLatLng(), northeast.getLatLng());
		return new MapLocationBounds(latLngBounds);
	}

	/**
	 * Decodes a {@link LatLng} instance from its string representation.
	 */
	public static LatLng stringToLatLng(String str)
	{
		Pair<Double, Double> coordinates = parseGeoLocation(str);
		if (coordinates != null)
			return new LatLng(coordinates.first, coordinates.second);
		else
			return null;
	}
}
