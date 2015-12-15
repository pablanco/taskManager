package com.artech.controls.maps.googlev2;

import java.util.HashMap;

import android.os.AsyncTask;

import com.artech.base.model.Entity;
import com.artech.controls.maps.GxMapViewDefinition;
import com.artech.controls.maps.common.MapPinHelper;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

class GxMapViewMarkers
{
	private final GoogleMap mMap;
	private final GxMapViewDefinition mDefinition;
	private HashMap<String, GxMapViewItem> mMarkerData;

	private LoaderTask mTask;
	private MapPinHelper mPinHelper;

	public GxMapViewMarkers(GxMapView mapView, GxMapViewDefinition definition)
	{
		mMap = mapView.getMap();
		mDefinition = definition;
		mMarkerData = new HashMap<String, GxMapViewItem>();
		mPinHelper = new MapPinHelper(mapView.getContext(), mDefinition, mapView.getAdapter().getImageLoader());
	}

	public void update(GxMapViewData mapData)
	{
		if (mTask != null)
			mTask.cancel(true);

		mMap.clear();
		mTask = new LoaderTask();
		mTask.execute(mapData);
	}

	public Entity getMarkerData(Marker marker)
	{
		GxMapViewItem mapItem = mMarkerData.get(marker.getId());
		if (mapItem != null)
			return mapItem.getData();
		else
			return null;
	}

	private class LoaderTask extends AsyncTask<GxMapViewData, MarkerInfo, Void>
	{
		@Override
		protected Void doInBackground(GxMapViewData... params)
		{
			GxMapViewData mapData = params[0];
			for (GxMapViewItem item : mapData)
			{
				if (isCancelled())
					return null;

				MarkerOptions marker = new MarkerOptions();
				marker.position(item.getLocation().getLatLng());
				marker.icon(getMarkerImage(item.getData()));

				publishProgress(new MarkerInfo(marker, item));
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(MarkerInfo... values)
		{
			Marker marker = mMap.addMarker(values[0].Options);
			mMarkerData.put(marker.getId(), values[0].Data);
		}
	}

	private class MarkerInfo
	{
		final MarkerOptions Options;
		final GxMapViewItem Data;

		MarkerInfo(MarkerOptions options, GxMapViewItem data)
		{
			Options = options;
			Data = data;
		}
	}

	private BitmapDescriptor getMarkerImage(Entity itemData)
	{
		MapPinHelper.ResourceOrBitmap pin = mPinHelper.getPinImage(itemData);
		
		if (pin.resourceId != null)
			return BitmapDescriptorFactory.fromResource(pin.resourceId);
		else if (pin.bitmap != null)
			return BitmapDescriptorFactory.fromBitmap(pin.bitmap);
		else
			return BitmapDescriptorFactory.defaultMarker();
	}
}
