package com.artech.controls.maps.googlev2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.artech.R;
import com.artech.activities.ActivityHelper;
import com.artech.android.api.LocationHelper;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.UIActionHelper;
import com.artech.controls.maps.common.LocationPickerHelper;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class LocationPickerActivity extends AppCompatActivity implements OnMapClickListener, OnMarkerDragListener
{
	private LocationPickerHelper mHelper;
	private MapView mMapView;
	private Marker mSelectionMarker;

	private int menuSelect = 2;
	private int menuCancel = 3;

	@SuppressLint("InlinedApi")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
    	ActivityHelper.onBeforeCreate(this);
		super.onCreate(savedInstanceState);
        ActivityHelper.initialize(this, savedInstanceState);

		setContentView(R.layout.map_layout);

		// set support toolbar
		Toolbar toolbar = (Toolbar)this.findViewById(R.id.toolbar);
		this.setSupportActionBar(toolbar);

		mHelper = new LocationPickerHelper(this, false);

        if (GoogleMapsHelper.checkGoogleMapsV2(this))
        {
			MapsInitializer.initialize(this);

			mMapView = MapViewFactory.createStandardMapView(this, new GoogleMapOptions());
			if (mMapView != null)
			{
				LinearLayout container = (LinearLayout)findViewById(R.id.map_container);
				container.addView(mMapView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

				mMapView.onCreate(savedInstanceState);
				mMapView.getMap().setOnMapClickListener(LocationPickerActivity.this);
				mMapView.getMap().setOnMarkerDragListener(LocationPickerActivity.this);

				mMapView.getMap().setMyLocationEnabled(true);
				//mMapView.getMap().getUiSettings().setMyLocationButtonEnabled(true);

				initializeFromIntent();
			}

        }
	}


	private void initializeFromIntent()
	{
		// Map type (standard, satellite, hybrid).
		String mapType = getIntent().getStringExtra(LocationPickerHelper.EXTRA_MAP_TYPE);
		mMapView.getMap().setMapType(GoogleMapsHelper.mapTypeToGoogleMapType(mapType));

		// Map position. If no value is set, center on current location.
		String mapLocation = getIntent().getStringExtra(LocationPickerHelper.EXTRA_LOCATION);
		if (!Strings.hasValue(mapLocation))
			mapLocation = LocationHelper.getLocationString(LocationHelper.getLastKnownLocation());

		LatLng latlng = MapUtils.stringToLatLng(mapLocation);
		if (latlng != null)
		{
			// Set marker.
			onMapClick(latlng);

			// Set center and radius.
			MapLocationBounds bounds = new MapUtils(null).getDefaultBoundingBox(new MapLocation(latlng));
			final CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds.getLatLngBounds(), Services.Device.dipsToPixels(GxMapViewCamera.CAMERA_MARGIN_DIPS));

			mMapView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener()
			{
				@Override
				@SuppressWarnings("deprecation")
				public void onGlobalLayout()
				{
					// Important: this must be done AFTER layout because the camera update needs the map view to have been measured to work.
					mMapView.getMap().moveCamera(cameraUpdate);
					mMapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}
			});
		}
	}

	@Override
	public void onMapClick(LatLng point)
	{
		GoogleMap map = mMapView.getMap();
		if (mSelectionMarker != null)
			map.clear();

		MarkerOptions markerOptions = new MarkerOptions();
		markerOptions.position(point);
		markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
		markerOptions.draggable(true);

		mSelectionMarker = map.addMarker(markerOptions);
		setPickedLocation(point);
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

		if (mMapView != null)
			mMapView.onResume();
	}

	@Override
	protected void onPause()
	{
		if (mMapView != null)
			mMapView.onPause();

		ActivityHelper.onPause(this);
		super.onPause();
	}

	@Override
	protected void onDestroy()
	{
		if (mMapView != null)
			mMapView.onDestroy();

		ActivityHelper.onDestroy(this);
		super.onDestroy();
	}

	@Override
	public void onMarkerDragStart(Marker marker)
	{
		setPickedLocation(marker.getPosition());
	}

	@Override
	public void onMarkerDrag(Marker marker)
	{
		setPickedLocation(marker.getPosition());
	}

	@Override
	public void onMarkerDragEnd(Marker marker)
	{
		setPickedLocation(marker.getPosition());
	}

	private void setPickedLocation(LatLng position)
	{
		mHelper.setPickedLocation(new MapLocation(position));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuItem itemSelect = menu.add(Menu.NONE, menuSelect, Menu.NONE, R.string.GX_BtnSelect);
		UIActionHelper.setStandardMenuItemImage(this, itemSelect, ActionDefinition.STANDARD_ACTION.SAVE);
		MenuItemCompat.setShowAsAction(itemSelect, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

		MenuItem itemCancel = menu.add(Menu.NONE, menuCancel, Menu.NONE, R.string.GXM_cancel);
		UIActionHelper.setStandardMenuItemImage(this, itemCancel, ActionDefinition.STANDARD_ACTION.CANCEL);
		MenuItemCompat.setShowAsAction(itemCancel, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();

		if (itemId == menuSelect)
		{
			mHelper.selectLocation();
		}
		else if (itemId == menuCancel)
		{
			mHelper.cancelSelect();
		}
		return super.onOptionsItemSelected(item);
	}

}
