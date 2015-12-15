package com.artech.controls.maps;

import android.app.Activity;
import android.content.Context;

import com.artech.R;
import com.artech.base.services.Services;
import com.artech.base.utils.NameMap;
import com.artech.base.utils.Strings;
import com.artech.controls.maps.common.IMapViewFactory;
import com.artech.controls.maps.common.IMapsProvider;

public class Maps
{
	private static NameMap<IMapsProvider> sMapProviders;

	static
	{
		// Initialize known maps providers.
		sMapProviders = new NameMap<IMapsProvider>();
		addProvider(new com.artech.controls.maps.googlev2.MapsProvider());
	}

	public static void addProvider(IMapsProvider provider)
	{
		sMapProviders.put(provider.getId(), provider);
	}

	static IMapViewFactory getMapViewFactory(Context context)
	{
		IMapsProvider provider = getProvider(context);
		if (provider != null)
			return provider.getMapViewFactory();
		else
			return null;
	}

	public static Class<? extends Activity> getLocationPickerActivityClass(Context context)
	{
		IMapsProvider provider = getProvider(context);
		if (provider != null)
			return provider.getLocationPickerActivityClass();
		else
			return null;
	}

	public static String getMapImageUrl(Context context, String location, int width, int height, String mapType)
	{
		IMapsProvider provider = getProvider(context);
		if (provider != null)
			return provider.getMapImageUrl(location, width, height, mapType);
		else
			return null;
	}

	static IMapsProvider getProvider(Context context)
	{
		String providerId = context.getResources().getString(R.string.MapsApi);
		if (Strings.hasValue(providerId))
		{
			IMapsProvider provider = sMapProviders.get(providerId);
			if (provider != null)
				return provider;
		}

		Services.Log.Error(String.format("Unknown value for MapsApi (%s).", providerId)); //$NON-NLS-1$
		return null;
	}
}
