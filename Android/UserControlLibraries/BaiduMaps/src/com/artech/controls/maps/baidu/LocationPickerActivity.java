package com.artech.controls.maps.baidu;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.LinearLayout;

import com.artech.activities.ActivityHelper;
import com.artech.controls.maps.common.LocationPickerHelper;
import com.baidu.mapapi.map.ItemizedOverlay;
import com.baidu.mapapi.map.MapView;
import com.baidu.platform.comapi.basestruct.GeoPoint;

public class LocationPickerActivity extends AppCompatActivity
{
	private LocationPickerHelper mHelper;
	private MapOverlay mOverlay;
	private MapView mMapView;

	@SuppressLint("InlinedApi")
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
    	ActivityHelper.onBeforeCreate(this);
		super.onCreate(savedInstanceState);
        ActivityHelper.initialize(this, savedInstanceState);

		setContentView(R.layout.map_layout);

		// set support toolbar
		Toolbar toolbar = (Toolbar)this.findViewById(R.id.toolbar);
		this.setSupportActionBar(toolbar);

		// Place the actual map view in the generic layout.
		mMapView = new MapView(this);
		LinearLayout mapContainer = (LinearLayout)findViewById(R.id.map_container);
		mapContainer.addView(mMapView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

		mMapView.setSatellite(false);
		mMapView.setBuiltInZoomControls(true);
		mMapView.setFocusable(true);
		mMapView.setEnabled(true);
		mMapView.setClickable(true);

		mHelper = new LocationPickerHelper(this, true);

		// Add overlay for location marker.
		mOverlay = new MapOverlay(mMapView);
		mMapView.getOverlays().add(mOverlay);
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		ActivityHelper.onNewIntent(this, intent);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		ActivityHelper.onResume(this);
		mMapView.onResume();
	}

	@Override
	protected void onPause()
	{
		mMapView.onPause();
		ActivityHelper.onPause(this);
		super.onPause();
	}

	@Override
	protected void onDestroy()
	{
		mMapView.destroy();
		ActivityHelper.onDestroy(this);
		super.onDestroy();
	}

	private class MapOverlay extends ItemizedOverlay<MapOverlayItem>
	{
		public MapOverlay(MapView mapView)
		{
			super(getResources().getDrawable(R.drawable.red_markers), mapView);
		}

		@Override
		public boolean onTap(GeoPoint point, MapView mapView)
		{
			mHelper.setPickedLocation(new MapLocation(point));
			return false;
		}
	}
}
