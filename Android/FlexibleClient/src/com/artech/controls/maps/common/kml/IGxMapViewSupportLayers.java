package com.artech.controls.maps.common.kml;

import com.artech.controls.maps.common.IGxMapView;

public interface IGxMapViewSupportLayers extends IGxMapView
{
	void addLayer(MapLayer layer);
	void removeLayer(MapLayer layer);
	void setLayerVisible(MapLayer layer, boolean visible);
	void adjustBoundsToLayer(MapLayer layer);
}
