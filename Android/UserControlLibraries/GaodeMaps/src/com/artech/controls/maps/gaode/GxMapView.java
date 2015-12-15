package com.artech.controls.maps.gaode;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.CancelableCallback;
import com.amap.api.maps.AMap.OnCameraChangeListener;
import com.amap.api.maps.AMap.OnMapLoadedListener;
import com.amap.api.maps.AMap.OnMarkerClickListener;
import com.amap.api.maps.AMap.OnMyLocationChangeListener;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.Projection;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.Marker;
import com.artech.base.controls.IGxControlNotifyEvents;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.controllers.ViewData;
import com.artech.controls.grids.GridAdapter;
import com.artech.controls.grids.GridHelper;
import com.artech.controls.maps.GxMapViewDefinition;
import com.artech.controls.maps.common.IGxMapView;
import com.artech.controls.maps.common.MapItemViewHelper;

@SuppressLint("ViewConstructor")
class GxMapView extends MapView implements IGxMapView, IGxControlNotifyEvents
{
	private final GxMapViewDefinition mDefinition;
	private boolean mIsReady;
	private boolean mOnResumeInvoked;

	private GridHelper mHelper;
	private GridAdapter mAdapter;
	private GxMapViewMarkers mMarkers;
	private GxMapViewCamera mCamera;
	private MapItemViewHelper mItemViewHelper;

	private boolean mIsMapLoaded;
	private CameraUpdate mPendingCameraUpdate;
	private boolean mIsMarkerClickCameraChange;

	private final static int ITEM_VIEW_WIDTH_MARGIN = 20; // dips
	private final static int MARKER_CAMERA_ANIMATION_DURATION = 500; // ms

	public GxMapView(Context context, GxMapViewDefinition definition)
	{
		super(context, new AMapOptions());
		mDefinition = definition;
		onCreate(new Bundle());
		initialize();
	}

	// TODO: Pass on events: onSaveInstanceState(Bundle) & onLowMemory().

	private void initialize()
	{
		mHelper = new GridHelper(this, mDefinition.getGrid());
		mAdapter = new GridAdapter(getContext(), mHelper, mDefinition.getGrid());
		mMarkers = new GxMapViewMarkers(this, mDefinition);
		mCamera = new GxMapViewCamera(this);
		mItemViewHelper = new MapItemViewHelper(this);

		try
		{
			MapsInitializer.initialize(getContext());
		}
		catch (RemoteException e)
		{
			Services.Log.error(e);
			return;
		}

		AMap map = getMap();
		if (map != null)
		{
			mHelper.setReservedSpace(ITEM_VIEW_WIDTH_MARGIN);

			map.setMapType(GaodeMapsHelper.mapTypeToGaodeMapType(mDefinition.getMapType()));
			map.setOnMapLoadedListener(mMapLoadedListener);
			map.setMyLocationEnabled(mDefinition.getShowMyLocation());
			map.setOnMyLocationChangeListener(mMyLocationChangeListener);
			map.setOnMarkerClickListener(mMarkerClickListener);
			map.setOnCameraChangeListener(mCameraChangeListener);

			mIsReady = true;
		}
	}

	@Override
	public void notifyEvent(EventType type)
	{
		switch (type)
		{
			case ACTIVITY_RESUMED :
				onResume();
				mOnResumeInvoked = true;
				break;

			case ACTIVITY_PAUSED :
				onPause();
				break;

			case ACTIVITY_DESTROYED :
				onDestroy();
				break;
				
			default :
				break;
		}
	}

	GridAdapter getAdapter() { return mAdapter; }

	@Override
	public void addListener(GridEventsListener listener)
	{
		mHelper.setListener(listener);
	}

	void animateCamera(CameraUpdate update)
	{
		AMap map = getMap();
		if (map != null && mIsMapLoaded)
		{
			try
			{
				map.animateCamera(update);
				return; // Done!
			}
			catch (IllegalStateException e)
			{
				// Map is not ready.
			}
		}

		mPendingCameraUpdate = update;
	}

	@Override
	public void update(ViewData data)
	{
		if (mIsReady)
		{
			// MapView.onResume() may not have been called if the fragment was added
			// afterwards (e.g. with slide navigation).
			if (!mOnResumeInvoked)
			{
				mOnResumeInvoked = true;
				onResume();
			}

			mAdapter.setData(data);

			// Add markers and position camera according to center/zoom properties.
			GxMapViewData mapData = new GxMapViewData(mDefinition, data);
			mMarkers.update(mapData);
			mCamera.update(mapData);
		}
	}

	private final OnMapLoadedListener mMapLoadedListener = new OnMapLoadedListener()
	{
		@Override
		public void onMapLoaded()
		{
			mIsMapLoaded = true;
			firePendingCameraUpdate();
		}
	};

	private final OnMarkerClickListener mMarkerClickListener = new OnMarkerClickListener()
	{
		@Override
		public boolean onMarkerClick(Marker marker)
		{
			// Show the "InfoWindow" associated to the marker.
			// Marker's InfoWindow cannot be used, because that view is not active. So we simulate it using this.
			Entity itemData = mMarkers.getMarkerData(marker);
			if (itemData != null)
			{
				// Do not show InfoWindow if item has no layout.
				if (mAdapter.isItemViewEmpty(itemData))
					return false;

				int position = mAdapter.getIndexOf(itemData);
				if (position != -1)
				{
					View itemView = mAdapter.getView(position, null, null);

					// Run default action if item detail is clicked.
					if (mDefinition.getGrid().getDefaultAction() != null)
						itemView.setOnClickListener(new OnItemViewClickListener(itemData));

					// Move camera to point (slightly off) and show item view afterwards.
					Projection projection = getMap().getProjection();
					Point centerPoint = projection.toScreenLocation(marker.getPosition());
					centerPoint.y -= GxMapView.this.getHeight() * MapItemViewHelper.MARKER_INFO_WINDOW_OFF_CENTER_FACTOR;

					// Bug workaround: according to documentation, a CameraUpdate built from newLatLng()
					// should not change zoom, but it does. Therefore, pass in current zoom.
					float currentZoom = getMap().getCameraPosition().zoom;
					CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(projection.fromScreenLocation(centerPoint), currentZoom);

					getMap().animateCamera(cameraUpdate, MARKER_CAMERA_ANIMATION_DURATION, new OnMarkerCameraUpdateCallback(itemView));
					return true;
				}
			}

			return false;
		}
	};

	private class OnMarkerCameraUpdateCallback implements CancelableCallback
	{
		private final View mItemView;

		OnMarkerCameraUpdateCallback(View itemView)
		{
			mItemView = itemView;
		}

		@Override
		public void onFinish()
		{
			mItemViewHelper.displayItem(mItemView);
			mIsMarkerClickCameraChange = true;
		}

		@Override
		public void onCancel() { }
	}

	private class OnItemViewClickListener implements OnClickListener
	{
		private final Entity mItemData;

		private OnItemViewClickListener(Entity itemData)
		{
			mItemData = itemData;
		}

		@Override
		public void onClick(View v)
		{
			mHelper.runDefaultAction(mItemData);
		}
	};

	private final OnCameraChangeListener mCameraChangeListener = new OnCameraChangeListener()
	{
		@Override
		public void onCameraChange(CameraPosition position)
		{
			// Fire camera update if it was pending due to layout not having been performed yet.
			firePendingCameraUpdate();

			if (mIsMarkerClickCameraChange)
			{
				mIsMarkerClickCameraChange = false;
				return;
			}

			// Remove item view when the map is scrolled.
			mItemViewHelper.removeCurrentItem();
		}

		@Override
		public void onCameraChangeFinish(CameraPosition position) { }
	};

	private void firePendingCameraUpdate()
	{
		CameraUpdate pendingUpdate = mPendingCameraUpdate;
		mPendingCameraUpdate = null;

		if (pendingUpdate != null)
			GxMapView.this.animateCamera(pendingUpdate);
	}

	private final OnMyLocationChangeListener mMyLocationChangeListener = new OnMyLocationChangeListener()
	{
		@Override
		public void onMyLocationChange(Location location)
		{
			// TODO Auto-generated method stub
		}
	};

	@Override
	public String getMapType()
	{
		AMap map = getMap();
		if (map != null)
			return GaodeMapsHelper.mapTypeFromGaodeMapType(map.getMapType());
		else
			return GxMapViewDefinition.MAP_TYPE_STANDARD;
	}

	@Override
	public void setMapType(String mapType)
	{
		AMap map = getMap();
		if (map != null)
			map.setMapType(GaodeMapsHelper.mapTypeToGaodeMapType(mapType));
	}
}
