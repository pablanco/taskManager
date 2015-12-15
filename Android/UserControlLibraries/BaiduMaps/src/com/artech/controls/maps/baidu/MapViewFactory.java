package com.artech.controls.maps.baidu;

import android.app.Activity;
import android.content.Context;

import com.artech.application.MyApplication;
import com.artech.controls.maps.GxMapViewDefinition;
import com.artech.controls.maps.common.IGxMapView;
import com.artech.controls.maps.common.IMapViewFactory;
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.MKGeneralListener;

class MapViewFactory implements IMapViewFactory
{
	private static BMapManager sMapManager;

	@Override
	public IGxMapView createView(Activity activity, GxMapViewDefinition definition)
	{
		prepareMapManager();
		return new BaiduMapView(activity, definition);
	}

	static void prepareMapManager()
	{
		if (sMapManager == null)
		{
			Context context = MyApplication.getAppContext();
			String apiKey = context.getResources().getString(R.string.MapsApiKey);

			// Initialize Baidu MapManager.
			sMapManager = new BMapManager(context);
			sMapManager.init(apiKey, sGeneralListener);
			sMapManager.start();
		}
	}

	private static MKGeneralListener sGeneralListener = new MKGeneralListener()
	{
		@Override
		public void onGetPermissionState(int iError)
		{
			// TODO Auto-generated method stub
		}

		@Override
		public void onGetNetworkState(int iError)
		{
			// TODO Auto-generated method stub
		}
	};

	@Override
	public void afterAddView(IGxMapView view) { }
}
