package com.artech.controls.maps.googlev2;

import java.util.List;

import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.view.View;

import com.artech.base.controls.IGxControlNotifyEvents;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.controllers.ViewData;
import com.artech.controls.grids.GridAdapter;
import com.artech.controls.grids.GridHelper;
import com.artech.controls.maps.GxMapViewDefinition;
import com.artech.controls.maps.common.IGxMapView;
import com.artech.controls.maps.common.IMapLocation;
import com.artech.controls.maps.common.MapItemViewHelper;
import com.artech.controls.maps.common.kml.IGxMapViewSupportLayers;
import com.artech.controls.maps.common.kml.MapLayer;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

class GxMapView extends MapView implements IGxMapView, IGxControlNotifyEvents, IGxMapViewSupportLayers
{
	private final GxMapViewDefinition mDefinition;
	private boolean mIsReady;
	private boolean mOnResumeInvoked;
	private boolean mOnDestroyInvoked;

	private GridHelper mHelper;
	private GridAdapter mAdapter;
	private GxMapViewMarkers mMarkers;
	private GxMapViewCamera mCamera;
	private MapItemViewHelper mItemViewHelper;

	private CameraUpdate mPendingCameraUpdate;
	private boolean mIsMarkerClickCameraChange;

	private final static int ITEM_VIEW_WIDTH_MARGIN = 20; // dips
	private final static int MARKER_CAMERA_ANIMATION_DURATION = 500; // ms

	public GxMapView(Context context, GxMapViewDefinition definition)
	{
		super(context, new GoogleMapOptions());
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

		MapsInitializer.initialize(getContext());
		
		GoogleMap map = getMap();
		if (map != null)
		{
			mHelper.setReservedSpace(ITEM_VIEW_WIDTH_MARGIN);

			map.setMapType(GoogleMapsHelper.mapTypeToGoogleMapType(mDefinition.getMapType()));
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
		if (mOnDestroyInvoked)
			return; // Ignore double onDestroy().

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
				mOnDestroyInvoked = true;
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
		GoogleMap map = getMap();
		if (map != null)
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

					CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(projection.fromScreenLocation(centerPoint));
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
	}

	private final OnCameraChangeListener mCameraChangeListener = new OnCameraChangeListener()
	{
		@Override
		public void onCameraChange(CameraPosition position)
		{
			// Fire camera update if it was pending due to layout not having been performed yet.
			if (mPendingCameraUpdate != null)
			{
				CameraUpdate pendingUpdate = mPendingCameraUpdate;
				mPendingCameraUpdate = null;

				GxMapView.this.animateCamera(pendingUpdate);
			}

			if (mIsMarkerClickCameraChange)
			{
				mIsMarkerClickCameraChange = false;
				return;
			}

			// Remove item view when the map is scrolled.
			mItemViewHelper.removeCurrentItem();
		}
	};

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
		GoogleMap map = getMap();
		if (map != null)
			return GoogleMapsHelper.mapTypeFromGoogleMapType(map.getMapType());
		else
			return GxMapViewDefinition.MAP_TYPE_STANDARD;
	}

	@Override
	public void setMapType(String mapType)
	{
		GoogleMap map = getMap();
		if (map != null)
			map.setMapType(GoogleMapsHelper.mapTypeToGoogleMapType(mapType));
	}

	@Override
	public void addLayer(MapLayer layer)
	{
		for (MapLayer.MapFeature feature : layer.features)
		{
			if (feature.type == MapLayer.FeatureType.Polygon)
			{
				MapLayer.Polygon polygon = (MapLayer.Polygon)feature;
				
				PolygonOptions options = new PolygonOptions();
				options.geodesic(true);
				
				// Polygon points, holes, and other properties.
				options.addAll(MapLocation.listToLatLng(polygon.outerBoundary));
				for (List<IMapLocation> hole : polygon.holes)
					options.addHole(MapLocation.listToLatLng(hole));
				if (polygon.strokeWidth != null)
					options.strokeWidth(polygon.strokeWidth);
				if (polygon.strokeColor != null)
					options.strokeColor(polygon.strokeColor);
				if (polygon.fillColor != null)
					options.fillColor(polygon.fillColor);

				// Add the polygon and store the reference to the map object.
				feature.mapObject = getMap().addPolygon(options);
			}
			else if (feature.type == MapLayer.FeatureType.Polyline)
			{
				MapLayer.Polyline polyline = (MapLayer.Polyline)feature;

				PolylineOptions options = new PolylineOptions();
				options.geodesic(true);

				// Polyline points and other properties.
				options.addAll(MapLocation.listToLatLng(polyline.points));
				if (polyline.strokeWidth != null)
					options.width(polyline.strokeWidth);
				if (polyline.strokeColor != null)
					options.color(polyline.strokeColor);

				feature.mapObject = getMap().addPolyline(options);
			}
		}
	}

	@Override
	public void removeLayer(MapLayer layer)
	{
		for (MapLayer.MapFeature feature : layer.features)
		{
			if (feature.type == MapLayer.FeatureType.Polygon)
				((Polygon)feature.mapObject).remove();
			else if (feature.type == MapLayer.FeatureType.Polyline)
				((Polyline)feature.mapObject).remove();
		}
	}

	@Override
	public void setLayerVisible(MapLayer layer, boolean visible)
	{
		for (MapLayer.MapFeature feature : layer.features)
		{
			if (feature.type == MapLayer.FeatureType.Polygon)
				((Polygon)feature.mapObject).setVisible(visible);
			else if (feature.type == MapLayer.FeatureType.Polyline)
				((Polyline)feature.mapObject).setVisible(visible);
		}
	}

	@Override
	public void adjustBoundsToLayer(MapLayer layer)
	{
		LatLngBounds.Builder builder = LatLngBounds.builder();
		{
			for (MapLayer.MapFeature feature : layer.features)
			{
				for (LatLng point : MapLocation.listToLatLng(feature.getPoints()))
					builder.include(point);
			}
		}

		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(builder.build(), Services.Device.dipsToPixels(GxMapViewCamera.CAMERA_MARGIN_DIPS));
		animateCamera(cameraUpdate);
	}
}
