package com.artech.controls.maps.common;

import android.app.Activity;

import com.artech.controls.maps.GxMapViewDefinition;

public interface IMapViewFactory
{
	IGxMapView createView(Activity activity, GxMapViewDefinition definition);
	void afterAddView(IGxMapView view);
}
