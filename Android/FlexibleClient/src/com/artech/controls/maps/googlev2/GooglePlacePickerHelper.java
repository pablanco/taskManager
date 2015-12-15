package com.artech.controls.maps.googlev2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.artech.android.GooglePlayServicesHelper;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

/**
 * Helper class to manage Places API functionality.
 * Created by matiash on 23/07/2015.
 */
public class GooglePlacePickerHelper
{
	private static final MapUtils sMapUtils = new MapUtils(null);
	private static Boolean sCheckForApiKeyResult = null;
	private static final double DEFAULT_RADIUS_KM = 0.14; // guessed from default behavior when no LatLngBounds is supplied.

	public static boolean isAvailable(Context context)
	{
		if (!GooglePlayServicesHelper.isPlayServicesAvailable(context))
			return false;

		if (sCheckForApiKeyResult == null)
		{
			// Also verify that we have an API key for the Place Picker API.
			try
			{
				ApplicationInfo app = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
				sCheckForApiKeyResult = (app != null && Strings.hasValue(app.metaData.getString("com.google.android.geo.API_KEY")));
			}
			catch (PackageManager.NameNotFoundException e)
			{
				e.printStackTrace();
				sCheckForApiKeyResult = false;
			}
		}

		return sCheckForApiKeyResult;
	}

	public static @Nullable Intent buildIntent(@NonNull Activity activity, String initialValue)
	{
		try
		{
			PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

			// Center on current value, if any.
			LatLng latLng = MapUtils.stringToLatLng(initialValue);
			if (latLng != null)
			{
				LatLngBounds initialBounds = sMapUtils.getBoundingBox(new MapLocation(latLng), DEFAULT_RADIUS_KM).getLatLngBounds();
				builder.setLatLngBounds(initialBounds);
			}

			return builder.build(activity);
		}
		catch (GooglePlayServicesRepairableException e)
		{
			GooglePlayServicesHelper.showError(activity, e.getConnectionStatusCode());
			return null;
		}
		catch (GooglePlayServicesNotAvailableException e)
		{
			GooglePlayServicesHelper.showError(activity, e.errorCode);
			return null;
		}
	}

	public static @Nullable String getLocationValueFromResult(@NonNull Context context, int resultCode, Intent data)
	{
		if (resultCode == Activity.RESULT_OK)
		{
			Place place = PlacePicker.getPlace(data, context);
			if (place != null)
			{
				LatLng latLng = place.getLatLng();
				return String.valueOf(latLng.latitude) + Strings.COMMA + String.valueOf(latLng.longitude);
			}
		}
		else if (resultCode == PlacePicker.RESULT_ERROR)
		{
			Services.Log.warning("Call to PlacePicker returned with RESULT_ERROR. Is 'Google Places API for Android' enabled in the Developer Console?");
		}

		return null;
	}
}
