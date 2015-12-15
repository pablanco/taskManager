package com.artech.controls.maps.common;

public interface IMapLocationBounds<LOCATION extends IMapLocation>
{
	LOCATION southwest();
	LOCATION northeast();
}
