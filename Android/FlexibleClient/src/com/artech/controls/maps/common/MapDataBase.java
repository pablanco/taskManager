package com.artech.controls.maps.common;

import java.util.ArrayList;
import java.util.List;

import android.util.Pair;

import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.controllers.ViewData;
import com.artech.controls.maps.GxMapViewDefinition;
import com.artech.utils.Cast;

public abstract class MapDataBase<ITEM_TYPE extends MapItemBase<LOCATION_TYPE>, LOCATION_TYPE extends IMapLocation, BOUNDS_TYPE extends IMapLocationBounds<LOCATION_TYPE>> extends ArrayList<ITEM_TYPE>
{
	private static final long serialVersionUID = 1L;

	private final MapUtilsBase<LOCATION_TYPE, BOUNDS_TYPE> mMapUtils;
	private LOCATION_TYPE mCustomCenter;
	private Double mZoomRadius;

	protected MapDataBase(ViewData itemsData, MapUtilsBase<LOCATION_TYPE, BOUNDS_TYPE> mapUtils)
	{
		mMapUtils = mapUtils;
		GxMapViewDefinition mapDefinition = mapUtils.getMapDefinition();

		for (Entity itemData : itemsData.getEntities())
		{
			ITEM_TYPE item = newMapItem(itemData);
			if (item != null)
				add(item);

			if (mapDefinition.getInitialCenter() == GxMapViewDefinition.INITIAL_CENTER_CUSTOM)
				mCustomCenter = newMapLocation(itemData, mapDefinition.getCustomCenterExpression());

			if (mapDefinition.getInitialZoom() == GxMapViewDefinition.INITIAL_ZOOM_RADIUS)
				mZoomRadius = Services.Strings.tryParseDouble(Cast.as(String.class, itemData.getProperty(mapDefinition.getZoomRadiusExpression())));
		}
	}

	private ITEM_TYPE newMapItem(Entity itemData)
	{
		LOCATION_TYPE location = newMapLocation(itemData, mMapUtils.getMapDefinition().getGeoLocationExpression());
		if (location != null)
			return newMapItem(location, itemData);
		else
			return null;
	}

	private LOCATION_TYPE newMapLocation(Entity itemData, String geolocationExpression)
	{
		if (Strings.hasValue(geolocationExpression))
		{
			String geolocation = Cast.as(String.class, itemData.getProperty(geolocationExpression));
			if (Strings.hasValue(geolocation))
			{
				Pair<Double, Double> coordinates = MapUtilsBase.parseGeoLocation(geolocation);
				if (coordinates != null)
					return mMapUtils.newMapLocation(coordinates.first, coordinates.second);
			}
		}

		return null;
	}

	protected abstract ITEM_TYPE newMapItem(LOCATION_TYPE location, Entity itemData);

	public BOUNDS_TYPE calculateBounds()
	{
		return mMapUtils.calculateBounds(getLocations(), mCustomCenter, mZoomRadius);
	}

	public List<LOCATION_TYPE> getLocations()
	{
		ArrayList<LOCATION_TYPE> locations = new ArrayList<LOCATION_TYPE>();
		for (MapItemBase<LOCATION_TYPE> item : this)
			locations.add(item.getLocation());

		return locations;
	}

	public LOCATION_TYPE getCustomCenter()
	{
		return mCustomCenter;
	}

	public Double getZoomRadius()
	{
		return mZoomRadius;
	}

}
