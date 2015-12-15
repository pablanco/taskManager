package com.artech.controls.maps.common;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;
import android.util.Pair;

import com.artech.android.api.LocationHelper;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.controls.maps.GxMapViewDefinition;

public abstract class MapUtilsBase<LOCATION_TYPE extends IMapLocation, BOUNDS_TYPE extends IMapLocationBounds<LOCATION_TYPE>>
{
	private final GxMapViewDefinition mDefinition;

	private static final double DEFAULT_RADIUS_KM = 0.3;
	
	protected MapUtilsBase(GxMapViewDefinition definition)
	{
		mDefinition = definition;
	}

	public GxMapViewDefinition getMapDefinition()
	{
		return mDefinition;
	}

	protected abstract LOCATION_TYPE newMapLocation(double latitude, double longitude);
	protected abstract BOUNDS_TYPE newMapBounds(LOCATION_TYPE southwest, LOCATION_TYPE northeast);

	public BOUNDS_TYPE calculateBounds(List<LOCATION_TYPE> points, LOCATION_TYPE customCenter, Double customZoomRadius)
	{
		BOUNDS_TYPE box;
		LOCATION_TYPE myLocation = null;

		if (mDefinition.needsUserLocation())
			myLocation = newMapLocation(LocationHelper.getLastKnownLocation());

		LOCATION_TYPE boxCenter = null;
		ArrayList<LOCATION_TYPE> boxPoints = new ArrayList<LOCATION_TYPE>();
		Double boxRadius = null;

		// Center choices: myself, custom, or none (determined by points to show).
		if (mDefinition.getInitialCenter() == GxMapViewDefinition.INITIAL_CENTER_MY_LOCATION && myLocation != null)
			boxCenter = myLocation;
		else if (mDefinition.getInitialCenter() == GxMapViewDefinition.INITIAL_CENTER_CUSTOM && customCenter != null)
			boxCenter = customCenter;

		// Zoom choices: default (show all points), nearest (show me and nearest point), radius (from center).
		if (mDefinition.getInitialZoom() == GxMapViewDefinition.INITIAL_ZOOM_DEFAULT)
			boxPoints.addAll(points);
		else if (mDefinition.getInitialZoom() == GxMapViewDefinition.INITIAL_ZOOM_NEAREST_POINT && myLocation != null)
			boxPoints.add(getNearest(myLocation, points));

		if (myLocation != null)
			boxPoints.add(myLocation);

		if (mDefinition.getInitialZoom() == GxMapViewDefinition.INITIAL_ZOOM_RADIUS && customZoomRadius != null)
			boxRadius = (customZoomRadius / 1000.0); // customZoomRadius is in meters.

		if (boxCenter != null)
		{
			if (boxPoints.size() != 0)
				box = getBoundingBox(boxCenter, boxPoints);
			else
				box = getBoundingBox(boxCenter);
		}
		else
		{
			if (boxPoints.size() != 0)
				box = newMapBounds(boxPoints);
			else
				box = null;
		}

		if (box != null && boxRadius != null)
			box = getBoundingBox(getCenterOf(box), customZoomRadius / 1000.0); // customZoomRadius in M.

		// Try to avoid creating a "0-sized" box (which would be shown with a HUGE magnification).
		if (box != null && box.southwest().equals(box.northeast()))
			box = getBoundingBox(box.southwest(), DEFAULT_RADIUS_KM);

		return box;
	}

	private LOCATION_TYPE newMapLocation(Location location)
	{
		if (location != null)
			return newMapLocation(location.getLatitude(), location.getLongitude());
		else
			return null;
	}

	protected BOUNDS_TYPE newMapBounds(List<LOCATION_TYPE> locations)
	{
		// The latitude is clamped between -80 degrees and +80 degrees inclusive.
		double minLatitude = +81;
		double maxLatitude = -81;

		// The longitude is clamped between -180 degrees and +180 degrees inclusive.
		double minLongitude = +181;
		double maxLongitude = -181;

		for (LOCATION_TYPE location : locations)
    	{
			double lat = location.getLatitude();
			minLatitude = (minLatitude > lat) ? lat : minLatitude;
			maxLatitude = (maxLatitude < lat) ? lat : maxLatitude;

			double lon = location.getLongitude();
			minLongitude = (minLongitude > lon) ? lon : minLongitude;
			maxLongitude = (maxLongitude < lon) ? lon : maxLongitude;
    	}

		LOCATION_TYPE southwest = newMapLocation(minLatitude, minLongitude);
		LOCATION_TYPE northeast = newMapLocation(maxLatitude, maxLongitude);

		return newMapBounds(southwest, northeast);
	}

	private double distanceBetween(LOCATION_TYPE point1, LOCATION_TYPE point2)
	{
		float[] results = new float[1];
		Location.distanceBetween(point1.getLatitude(), point1.getLongitude(), point2.getLatitude(), point2.getLongitude(), results);
		return results[0];
	}

	private LOCATION_TYPE getNearest(LOCATION_TYPE from, List<LOCATION_TYPE> points)
	{
		LOCATION_TYPE nearest = null;
		double nearestDistance = Double.MAX_VALUE;

		for (LOCATION_TYPE point : points)
		{
			double pointDistance = distanceBetween(from, point);
			if (pointDistance < nearestDistance)
			{
				nearest = point;
				nearestDistance = pointDistance;
			}
		}

		return nearest;
	}

	private BOUNDS_TYPE getBoundingBox(LOCATION_TYPE center)
	{
		ArrayList<LOCATION_TYPE> points = new ArrayList<LOCATION_TYPE>();
		points.add(center);
		return newMapBounds(points);
	}

	private BOUNDS_TYPE getBoundingBox(LOCATION_TYPE center, List<LOCATION_TYPE> points)
	{
		// Get the box that contains all points.
		BOUNDS_TYPE box = newMapBounds(points);

		// Extend it so that it is centered where desired.
		return extendBoxToCenterOn(box, center);
	}

	private BOUNDS_TYPE extendBoxToCenterOn(BOUNDS_TYPE box, LOCATION_TYPE center)
	{
		Pair<Double, Double> latitudes = extendSegmentToCenterAround(box.southwest().getLatitude(), box.northeast().getLatitude(), center.getLatitude());
		Pair<Double, Double> longitudes = extendSegmentToCenterAround(box.southwest().getLongitude(), box.northeast().getLongitude(), center.getLongitude());

		LOCATION_TYPE southwest = newMapLocation(latitudes.first, longitudes.first);
		LOCATION_TYPE northeast = newMapLocation(latitudes.second, longitudes.second);

		return newMapBounds(southwest, northeast);
	}

	private static Pair<Double, Double> extendSegmentToCenterAround(double start, double end, double center)
	{
		if (center < start)
			start = center - (end - center); // Move start to the left, so that it surpasses center and stands at the same distance as center does from end.
		else if (center > end)
			end = center + (center - start); // Move end to the right, so that it surpasses center and stands at the same distance as center does from start.
		else
		{
			// Center is already between start and end. Push the *closest* one from center further along.
			if ((end - center) < (center - start))
				end = center + (center - start);
			else
				start = center - (end - center);
		}

		return new Pair<Double, Double>(start, end);
	}

	public BOUNDS_TYPE getDefaultBoundingBox(LOCATION_TYPE center)
	{
		return getBoundingBox(center, DEFAULT_RADIUS_KM);
	}
	
	public BOUNDS_TYPE getBoundingBox(LOCATION_TYPE center, double radiusKm)
	{
		ArrayList<LOCATION_TYPE> centerAndCardinals = new ArrayList<LOCATION_TYPE>();
		centerAndCardinals.add(center);

		centerAndCardinals.add(getPointAtDistanceAndBearing(center, radiusKm, 0)); // North
		centerAndCardinals.add(getPointAtDistanceAndBearing(center, radiusKm, Math.PI * 0.5)); // East
		centerAndCardinals.add(getPointAtDistanceAndBearing(center, radiusKm, Math.PI * 1.0)); // South
		centerAndCardinals.add(getPointAtDistanceAndBearing(center, radiusKm, Math.PI * 1.5)); // West

		return newMapBounds(centerAndCardinals);
	}

	private LOCATION_TYPE getPointAtDistanceAndBearing(LOCATION_TYPE from, double distanceKm, double bearingRadians)
	{
	    final double EARTH_RADIUS_KM = 6371.01;
	    double distRatio = distanceKm / EARTH_RADIUS_KM;
	    double distRatioSine = Math.sin(distRatio);
	    double distRatioCosine = Math.cos(distRatio);

	    double startLatRad = Math.toRadians(from.getLatitude());
	    double startLonRad = Math.toRadians(from.getLongitude());

	    double startLatCos = Math.cos(startLatRad);
	    double startLatSin = Math.sin(startLatRad);

	    double endLatRads = Math.asin((startLatSin * distRatioCosine) + (startLatCos * distRatioSine * Math.cos(bearingRadians)));

	    double endLonRads = startLonRad + Math.atan2(Math.sin(bearingRadians) * distRatioSine * startLatCos,
	            distRatioCosine - startLatSin * Math.sin(endLatRads));

	    return newMapLocation(Math.toDegrees(endLatRads), Math.toDegrees(endLonRads));
	}

	public static Pair<Double, Double> parseGeoLocation(String str)
	{
		if (!Strings.hasValue(str))
			return null;

		String[] latlon = Services.Strings.split(str, ',');
		if (latlon.length == 2)
		{
			try
			{
				double lat = Double.valueOf(latlon[0]);
				double lon = Double.valueOf(latlon[1]);
				return new Pair<Double, Double>(lat, lon);
			}
			catch (NumberFormatException ex) { }
		}

		Services.Log.Error(String.format("Unexpected geolocation format in '%s'.", str)); //$NON-NLS-1$
		return null;
	}

	private LOCATION_TYPE getCenterOf(BOUNDS_TYPE box)
	{
		double centerLatitude = (box.southwest().getLatitude() + box.northeast().getLatitude()) / 2.0;
		double centerLongitude = (box.southwest().getLongitude() + box.northeast().getLongitude()) / 2.0;

		return newMapLocation(centerLatitude, centerLongitude);
	}
}
