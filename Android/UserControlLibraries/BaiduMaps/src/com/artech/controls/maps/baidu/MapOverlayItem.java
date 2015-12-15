package com.artech.controls.maps.baidu;

import android.graphics.drawable.Drawable;
import android.location.Location;

import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.controls.maps.GxMapViewDefinition;
import com.artech.utils.Cast;
import com.baidu.mapapi.map.OverlayItem;
import com.baidu.platform.comapi.basestruct.GeoPoint;

public class MapOverlayItem extends OverlayItem
{
	private int mId;
	private Entity mEntity;
	private String mPendingImage;

	public static MapOverlayItem from(GxMapViewDefinition mapDefinition, Entity entity, int index)
	{
		String geolocation = Cast.as(String.class, entity.getProperty(mapDefinition.getGeoLocationExpression()));
		if (!Services.Strings.hasValue(geolocation))
			return null;

		GeoPoint geoPoint = MapUtils.stringToGeoPoint(geolocation);
		if (geoPoint != null)
		{
			MapOverlayItem item = new MapOverlayItem(geoPoint, index, entity);
			setItemImage(mapDefinition, item, entity);
			return item;
		}

		return null;
	}

	private static void setItemImage(GxMapViewDefinition mapDefinition, MapOverlayItem item, Entity entity)
	{
		// Set "base" marker in UI thread, because it is static. TODO: fix for KBN, doesn't work there.
		Drawable image = mapDefinition.getPinImage();

		item.setMarker(image);

		// Load data image, if specified, in background.
		if (Services.Strings.hasValue(mapDefinition.getPinImageExpression()))
		{
			String imageValue = Cast.as(String.class, entity.getProperty(mapDefinition.getPinImageExpression()));
			if (Services.Strings.hasValue(imageValue))
				item.mPendingImage = imageValue;
		}
	}

	String getPendingImage() { return mPendingImage; }

	public static MapOverlayItem custom(Location location, Drawable image)
	{
		MapOverlayItem item = new MapOverlayItem(MapUtils.locationToGeoPoint(location), -1, null);
		item.setMarker(image);
		return item;
	}

	public MapOverlayItem(GeoPoint point, int id, Entity entity)
	{
		super(point, Strings.EMPTY, Strings.EMPTY); // For now, no title/snippet.
		mId = id;
		mEntity = entity;
	}

	@Override
	public void setMarker(Drawable marker)
	{
		// Put the image "above" the point (pin is center lowest pixel).
//		if (marker != null)
//			marker.setBounds(-marker.getIntrinsicWidth() / 2, -marker.getIntrinsicHeight() , marker.getIntrinsicWidth() / 2, 0);

		super.setMarker(marker);
	}

	public int getId() { return mId; }
	public Entity getEntity() { return mEntity; }
	public boolean hasDetail() { return (mEntity != null); }
}
