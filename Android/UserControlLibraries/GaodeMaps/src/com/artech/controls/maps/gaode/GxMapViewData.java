package com.artech.controls.maps.gaode;

import com.artech.base.model.Entity;
import com.artech.controllers.ViewData;
import com.artech.controls.maps.GxMapViewDefinition;
import com.artech.controls.maps.common.MapDataBase;

class GxMapViewData extends MapDataBase<GxMapViewItem, MapLocation, MapLocationBounds>
{
	private static final long serialVersionUID = 1L;

	public GxMapViewData(GxMapViewDefinition mapDefinition, ViewData itemsData)
	{
		super(itemsData, new MapUtils(mapDefinition));
		}

	@Override
	protected GxMapViewItem newMapItem(MapLocation location, Entity itemData)
	{
		return new GxMapViewItem(location, itemData);
	}
}
