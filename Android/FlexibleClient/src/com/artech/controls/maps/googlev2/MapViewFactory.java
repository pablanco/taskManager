package com.artech.controls.maps.googlev2;

import android.app.Activity;

import com.artech.R;
import com.artech.base.services.Services;
import com.artech.base.utils.Function;
import com.artech.base.utils.Strings;
import com.artech.controls.maps.GxMapViewDefinition;
import com.artech.controls.maps.common.IGxMapView;
import com.artech.controls.maps.common.IMapViewFactory;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;

class MapViewFactory implements IMapViewFactory
{
	@Override
	public IGxMapView createView(final Activity activity, final GxMapViewDefinition definition)
	{
		return createInstance(activity, new Function<Void, IGxMapView>()
		{
			@Override
			public IGxMapView run(Void input)
			{
				return new GxMapView(activity, definition);
			}
		});
	}

	@Override
	public void afterAddView(IGxMapView view)
	{
		// Nothing to do.
	}

	static MapView createStandardMapView(final Activity activity, final GoogleMapOptions options)
	{
		return createInstance(activity, new Function<Void, MapView>()
		{
			@Override
			public MapView run(Void input)
			{
				return new MapView(activity, options);
			}
		});
	}

	private static <TView> TView createInstance(Activity activity, Function<Void, TView> creator)
	{
		String apiKey = activity.getResources().getString(R.string.MapsApiKey);
		if (Strings.hasValue(apiKey))
		{
			if (GoogleMapsHelper.checkGoogleMapsV2(activity))
			{
				try
				{
					return creator.run(null);
				}
				catch (Exception e)
				{
					Services.Log.Error("Exception creating map instance", e); //$NON-NLS-1$
				}
			}
			else
				Services.Log.Error("Google Play Services and/or Google Maps is not installed."); //$NON-NLS-1$
		}
		else
			Services.Log.Error("No key was provided for Google Maps API V2."); //$NON-NLS-1$

		return null; // Could not create.
	}
}
