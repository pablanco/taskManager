package com.artech.controls.maps.baidu;

import android.app.Activity;
import android.util.Pair;

import com.artech.controls.maps.common.IMapLocation;
import com.artech.controls.maps.common.IMapViewFactory;
import com.artech.controls.maps.common.IMapsProvider;

public class MapsProvider implements IMapsProvider
{
	private static final String PROVIDER_ID = "MAPS_BAIDU";

	@Override
	public String getId()
	{
		return PROVIDER_ID;
	}

	@Override
	public IMapViewFactory getMapViewFactory()
	{
		return new MapViewFactory();
	}

	@Override
	public Class<? extends Activity> getLocationPickerActivityClass()
	{
		return LocationPickerActivity.class;
	}

	@Override
	public String getMapImageUrl(String location, int width, int height, String mapType)
	{
		final String URL_FORMAT = "http://api.map.baidu.com/staticimage?markers=%s,%s&width=%s&height=%s&zoom=15"; //$NON-NLS-1$

		// Baidu expects <longitude,latitude> instead of <latitude,longitude>.
		Pair<Double, Double> latlon = MapUtils.parseGeoLocation(location);
		if (latlon != null)
			return String.format(URL_FORMAT, latlon.second, latlon.first, width, height);
		else
			return null;
	}

	@Override
	public IMapLocation newMapLocation(double latitude, double longitude)
	{
		return new MapLocation(latitude, longitude);
	}
}
