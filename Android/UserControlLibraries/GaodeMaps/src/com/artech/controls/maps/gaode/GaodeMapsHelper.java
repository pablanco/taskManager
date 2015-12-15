package com.artech.controls.maps.gaode;

import com.amap.api.maps.AMap;
import com.artech.base.utils.Strings;
import com.artech.controls.maps.GxMapViewDefinition;

class GaodeMapsHelper
{
	static int mapTypeToGaodeMapType(String mapType)
	{
		if (Strings.hasValue(mapType))
		{
			if (mapType.equalsIgnoreCase(GxMapViewDefinition.MAP_TYPE_SATELLITE))
				return AMap.MAP_TYPE_SATELLITE;
		}

		return AMap.MAP_TYPE_NORMAL;
	}

	static String mapTypeFromGaodeMapType(int googleMapType)
	{
		switch (googleMapType)
		{
			case AMap.MAP_TYPE_SATELLITE :
				return GxMapViewDefinition.MAP_TYPE_SATELLITE;
			default :
				return GxMapViewDefinition.MAP_TYPE_STANDARD;
		}
	}
}
