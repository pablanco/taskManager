package com.artech.controls.maps.baidu;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;

import com.artech.android.api.LocationHelper;
import com.artech.base.controls.IGxControlNotifyEvents;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.services.Services;
import com.artech.common.ImageHelper;
import com.artech.controllers.ViewData;
import com.artech.controls.grids.GridAdapter;
import com.artech.controls.grids.GridHelper;
import com.artech.controls.maps.GxMapViewDefinition;
import com.artech.controls.maps.common.IGxMapView;
import com.baidu.mapapi.map.MapController;
import com.baidu.mapapi.map.MapView;
import com.baidu.platform.comapi.basestruct.GeoPoint;

@SuppressLint("ViewConstructor")
public class BaiduMapView extends MapView implements IGxMapView, IGxControlNotifyEvents
{
	private final GxMapViewDefinition mDefinition;
	private final MapUtils mUtils;

	private GridHelper mHelper;
	private GridAdapter mAdapter;

	private List<MapOverlayItem> mItems;
	private MapItemizedOverlay mItemizedOverlay;
	private LoadMarkersTask mLoadMarkersTask;
	private GeoPoint mAutoCenter;

	private Location mMyLocation;
	private GeoPoint mCustomCenterLocation;
	private Double mCustomZoomRadius;

	public BaiduMapView(Context context, GxMapViewDefinition definition)
	{
		super(context);
		mDefinition = definition;
		mUtils = new MapUtils(definition);
		initialize();
	}

	private void initialize()
	{
		mHelper = new GridHelper(this, mDefinition.getGrid());
		mAdapter = new GridAdapter(getContext(), mHelper, mDefinition.getGrid());

		setBuiltInZoomControls(true);
		setFocusable(true);
		setEnabled(true);
		setClickable(true);

		if (Services.Strings.hasValue(mDefinition.getMapType()))
			setMapType(mDefinition.getMapType());

		mItemizedOverlay = new MapItemizedOverlay(this, mDefinition.getPinImage());
		mItemizedOverlay.setAdapter(mAdapter);
		mItems = new ArrayList<MapOverlayItem>();
	}

	@Override
	public void notifyEvent(EventType type)
	{
		switch (type)
		{
			case ACTIVITY_RESUMED : onResume(); break;
			case ACTIVITY_PAUSED : onPause(); break;
			case ACTIVITY_DESTROYED : destroy(); break;
			default: break;
		}
	}

	GridHelper getHelper() { return mHelper; }

	@Override
	public void addListener(GridEventsListener listener)
	{
		mHelper.setListener(listener);
	}

	@Override
	public void update(ViewData data)
	{
		mAdapter.setData(data);
		updateMap(data.getEntities());
	}

	private void updateMap(EntityList data)
	{
		mItems.clear();
		getOverlays().clear();
		mItemizedOverlay.removeAll();
		mAutoCenter = null;
		mCustomCenterLocation = null;
		mCustomZoomRadius = null;

		if (mLoadMarkersTask != null)
		{
			mLoadMarkersTask.cancel(true);
			mLoadMarkersTask = null;
		}

		// Obtain location if it's shown or if it participates in center / zoom calculations.
		if (mDefinition.needsUserLocation())
			mMyLocation = LocationHelper.getLastKnownLocation();

		if (Services.Strings.hasValue(mDefinition.getGeoLocationExpression()))
		{
			for (int i = 0; i < data.size() ; i++)
			{
				Entity entity = data.get(i);
				MapOverlayItem mapItem = MapOverlayItem.from(mDefinition, entity, i);
				if (mapItem != null)
					mItems.add(mapItem);

				if (mDefinition.getInitialCenter() == GxMapViewDefinition.INITIAL_CENTER_CUSTOM &&
					Services.Strings.hasValue(mDefinition.getCustomCenterExpression()))
				{
					String customCenter = entity.optStringProperty(mDefinition.getCustomCenterExpression());
					mCustomCenterLocation = MapUtils.stringToGeoPoint(customCenter);
				}

				if (mDefinition.getInitialZoom() == GxMapViewDefinition.INITIAL_ZOOM_RADIUS &&
					Services.Strings.hasValue(mDefinition.getZoomRadiusExpression()))
				{
					String customZoomRadius = entity.optStringProperty(mDefinition.getZoomRadiusExpression());
					mCustomZoomRadius = Services.Strings.tryParseDouble(customZoomRadius);
				}
			}
		}
		else
			Services.Log.warning("No geolocation attribute found in definition"); //$NON-NLS-1$

		// Items whose images are not ready will be shown later.
		List<MapOverlayItem> pendingItems = new ArrayList<MapOverlayItem>();
		for (MapOverlayItem item : mItems)
		{
			if (item.getPendingImage() == null)
				mItemizedOverlay.addItem(item);
			else
				pendingItems.add(item);
		}

		if (mDefinition.getShowMyLocation() && mMyLocation != null)
			mItemizedOverlay.addItem(MapOverlayItem.custom(mMyLocation, mDefinition.getMyLocationImage()));

		autoZoom(mItems);
		getOverlays().add(mItemizedOverlay);

		refresh();

		if (pendingItems.size() != 0)
		{
			// Start a thread to load marker images from data.
			mLoadMarkersTask = new LoadMarkersTask(mItemizedOverlay, pendingItems);
			mLoadMarkersTask.execute();
		}
	}

	private class LoadMarkersTask extends AsyncTask<Void, MapOverlayItem, Void>
	{
		private static final int REDRAW_AFTER_ITEMS = 5;

		private final MapItemizedOverlay mOverlay;
		private final List<MapOverlayItem> mItems;
		private int mDone = 0;

		public LoadMarkersTask(MapItemizedOverlay overlay, List<MapOverlayItem> items)
		{
			mOverlay = overlay;
			mItems = items;
		}

		@Override
		protected Void doInBackground(Void... params)
		{
			for (MapOverlayItem item : mItems)
			{
				if (isCancelled())
					break;

				Drawable itemImage = ImageHelper.getDrawableValue(item.getPendingImage());
				if (itemImage != null)
					item.setMarker(itemImage);

				publishProgress(item);
			}

			return null;
		}


		@Override
		protected void onProgressUpdate(MapOverlayItem... values)
		{
			MapOverlayItem ready = values[0];
			mOverlay.addItem(ready);
			mDone++;

			if ((mDone % REDRAW_AFTER_ITEMS) == 0)
				redraw();
		}

		@Override
		protected void onPostExecute(Void result)
		{
			redraw();
		}

		private void redraw()
		{
			refresh();
			// mOverlay.displayItems();

			if (mAutoCenter != null)
				getController().animateTo(mAutoCenter);
		}
	}

	private void autoZoom(List<MapOverlayItem> items)
	{
		final MapLocationBounds bounds = mUtils.calculateBounds(toMapLocations(items), MapLocation.from(mCustomCenterLocation), mCustomZoomRadius);

		if (bounds != null)
		{
			post(new Runnable()
			{
				@Override
				public void run()
				{
					// Apply calculated zoom and center, if any.
					MapController controller = getController();
					controller.zoomToSpan(bounds.getLatitudeSpan(), bounds.getLongitudeSpan());

					mAutoCenter = bounds.getCenter();
					controller.animateTo(mAutoCenter);
				}
			});
		}
	}

	private static List<MapLocation> toMapLocations(List<MapOverlayItem> items)
	{
		ArrayList<MapLocation> points = new ArrayList<MapLocation>();
		for (MapOverlayItem item : items)
			points.add(new MapLocation(item.getPoint()));

		return points;
	}

	@Override
	public String getMapType()
	{
		if (isSatellite())
			return GxMapViewDefinition.MAP_TYPE_SATELLITE;
		else
			return GxMapViewDefinition.MAP_TYPE_STANDARD;
	}

	@Override
	public void setMapType(String type)
	{
		boolean isSatellite = (type.equalsIgnoreCase(GxMapViewDefinition.MAP_TYPE_SATELLITE) || type.equalsIgnoreCase(GxMapViewDefinition.MAP_TYPE_HYBRID));
		setSatellite(isSatellite);
	}
}
