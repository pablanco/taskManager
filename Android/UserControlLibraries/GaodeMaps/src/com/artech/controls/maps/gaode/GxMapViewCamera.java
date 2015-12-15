package com.artech.controls.maps.gaode;

import android.os.AsyncTask;

import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.LatLngBounds;
import com.artech.base.services.Services;

class GxMapViewCamera
{
	private final GxMapView mMap;
	private CalculateBoundsTask mTask;

	static final int CAMERA_MARGIN_DIPS = 40;

	public GxMapViewCamera(GxMapView mapView)
	{
		mMap = mapView;
	}

	public void update(GxMapViewData mapData)
	{
		if (mTask != null)
			mTask.cancel(true);

		mTask = new CalculateBoundsTask();
		mTask.execute(mapData);
	}

	private void updateCamera(LatLngBounds bounds)
	{
		if (bounds != null)
		{
			CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, Services.Device.dipsToPixels(CAMERA_MARGIN_DIPS));
			mMap.animateCamera(update);
		}
	}

	private class CalculateBoundsTask extends AsyncTask<GxMapViewData, Void, LatLngBounds>
	{
		@Override
		protected LatLngBounds doInBackground(GxMapViewData... params)
		{
			GxMapViewData mapData = params[0];

			MapLocationBounds bounds = mapData.calculateBounds();
			return (bounds != null ? bounds.getLatLngBounds() : null);
		}

		@Override
		protected void onPostExecute(LatLngBounds result)
		{
			updateCamera(result);
		}
	}
}
