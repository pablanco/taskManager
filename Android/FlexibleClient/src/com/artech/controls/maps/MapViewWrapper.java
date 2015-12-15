package com.artech.controls.maps;

import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.artech.actions.ICustomMenuManager;
import com.artech.android.layout.GridContext;
import com.artech.base.controls.IGxControlPreserveState;
import com.artech.base.controls.IGxControlRuntime;
import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.NameMap;
import com.artech.base.utils.Strings;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.controllers.ViewData;
import com.artech.controls.GxListView;
import com.artech.controls.IGridView;
import com.artech.controls.maps.common.IGxMapView;
import com.artech.controls.maps.common.IMapViewFactory;
import com.artech.controls.maps.common.MapViewHelper;
import com.artech.controls.maps.common.kml.IGxMapViewSupportLayers;
import com.artech.controls.maps.common.kml.KmlReaderAsyncTask;
import com.artech.controls.maps.common.kml.MapLayer;
import com.artech.ui.Coordinator;
import com.artech.utils.Cast;

public class MapViewWrapper extends LinearLayout implements IGridView, ICustomMenuManager, IGxControlRuntime, IGxControlPreserveState
{
	private final IMapViewFactory mFactory;
	@SuppressWarnings("unused")
	private final Coordinator mCoordinator;
	private final GxMapViewDefinition mDefinition;
	private IGridView mView;
	
	private NameMap<MapLayer> mLayers;

	@SuppressLint("InlinedApi")
	public MapViewWrapper(Context context, Coordinator coordinator, LayoutItemDefinition layoutDefinition)
	{
		super(context);
		mFactory = Maps.getMapViewFactory(context);
		mCoordinator = coordinator;
		mDefinition = new GxMapViewDefinition(context, (GridDefinition)layoutDefinition);
		mLayers = new NameMap<MapLayer>();

		if (mFactory != null)
			mView = mFactory.createView(getActivity(), mDefinition);

		if (mView == null)
		{
			// We couldn't create a MapView, probably because the device doesn't have API support.
			// Use a normal ListView in that case.
			mView = new GxListView(context, (GridDefinition)layoutDefinition);
		}

		// Unlink from a possible parent before adding here.
		View view = (View)mView;
		if (view.getParent() != null)
			((ViewGroup)view.getParent()).removeView(view);

		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		addView(view, lp);

		if (mFactory != null && mView != null && mView instanceof IGxMapView)
			mFactory.afterAddView((IGxMapView)mView);
	}

	private Activity getActivity()
	{
		return (Activity)((GridContext)getContext()).getBaseContext();
	}

	@Override
	public void addListener(GridEventsListener listener)
	{
		mView.addListener(listener);
	}

	@Override
	public void update(ViewData data)
	{
		mView.update(data);
	}

	public String getControlName()
	{
		return mDefinition.getGrid().getName();
	}

	@Override
	public void onCustomCreateOptionsMenu(Menu menu)
	{
		if (mView instanceof IGxMapView && mDefinition.getCanChooseMapType())
			MapViewHelper.addMapTypeMenu((IGxMapView)mView, menu);
	}

	public MapViewWrapper(Context context)
	{
		super(context);
		throw new UnsupportedOperationException("Unsupported constructor."); //$NON-NLS-1$
	}

	@Override
	public Object getProperty(String name)
	{
		Services.Log.warning("Unsupported map property: " + name);
		return null;
	}
	
	@Override
	public void setProperty(String name, Object value)
	{
		Services.Log.warning("Unsupported map property: " + name);
	}

	private static final String PROPERTY_MAP_TYPE = "MapType";
	private static final String METHOD_LOAD_KML = "LoadKML";
	private static final String METHOD_SET_LAYER_VISIBLE = "SetLayerVisible";
	private static final String METHOD_ADJUST_VISIBLE_AREA_TO_LAYER = "AdjustVisibleAreaToLayer";
	
	@Override
	public void runMethod(String name, List<Object> parameters)
	{
		final IGxMapViewSupportLayers mapView = Cast.as(IGxMapViewSupportLayers.class, mView);
		if (mapView == null)
			return; // Maps must implement the interface to support these methods.

		if (METHOD_LOAD_KML.equalsIgnoreCase(name) && parameters.size() == 3)
		{
			final String layerId = String.valueOf(parameters.get(0));
			final String kmlString = String.valueOf(parameters.get(1));
			final boolean visible = Services.Strings.tryParseBoolean(String.valueOf(parameters.get(2)), true);

			// Remove previous layer with same id, if any.
			MapLayer previousLayer = mLayers.get(layerId);
			if (previousLayer != null)
				mapView.removeLayer(previousLayer);

			// Deserialize and add new layer.
			// NOTE: This is asynchronous because the file may be on the server (and even if not, KML parsing is expensive).
			CompatibilityHelper.executeAsyncTask(new KmlReaderAsyncTask(Maps.getProvider(getContext()))
			{
				@Override
				protected void onPostExecute(MapLayer result)
				{
					if (result != null)
					{
						result.id = layerId;
						mLayers.put(layerId, result);
						
						mapView.addLayer(result);
						
						if (!visible)
							mapView.setLayerVisible(result, false);
					}
				}
			}, kmlString);
		}
		else if (METHOD_SET_LAYER_VISIBLE.equalsIgnoreCase(name) && parameters.size() == 2)
		{
			MapLayer layer = mLayers.get(String.valueOf(parameters.get(0)));
			if (layer != null)
				mapView.setLayerVisible(layer, Services.Strings.tryParseBoolean(String.valueOf(parameters.get(1)), false));
		}
		else if (METHOD_ADJUST_VISIBLE_AREA_TO_LAYER.equalsIgnoreCase(name) && parameters.size() == 1)
		{
			MapLayer layer = mLayers.get(String.valueOf(parameters.get(0)));
			if (layer != null)
				mapView.adjustBoundsToLayer(layer);
		}
	}

	@Override
	public String getControlId()
	{
		return mDefinition.getGrid().getName();
	}
	
	@Override
	public void saveState(Map<String, Object> state)
	{
		if (mView instanceof IGxMapView)
		{
			String mapType = ((IGxMapView)mView).getMapType();
			state.put(PROPERTY_MAP_TYPE, mapType);
		}
	}
	
	@Override
	public void restoreState(Map<String, Object> state)
	{
		if (mView instanceof IGxMapView)
		{
			String mapType = (String)state.get(PROPERTY_MAP_TYPE);
			if (Strings.hasValue(mapType))
				((IGxMapView)mView).setMapType(mapType);
		}
	}
}
