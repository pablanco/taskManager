package com.artech.android.api;

import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;

import com.artech.application.MyApplication;
import com.artech.base.services.Services;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;


public class LocationFusedProviderHelper implements ConnectionCallbacks, OnConnectionFailedListener
{

	private GoogleApiClient  mGoogleApiClient;
	private LocationRequest mLocationRequest;
	
	private int mMinAccuracyRequest = 0;
	private int mMinTimeRequest = 0;
	private int mMinDistanceRequest = 0;
	
	private LocationListener mLocationListener;
	private boolean mRequestLocationUpdatesPending = false;
	private boolean mIsBackgroundRequest = false;
	
	public LocationFusedProviderHelper()
	{
		/**
			* LocationClient(arg1, arg2 , arg3)
			* arg1 :Context
			* arg2 : ConnectionCallbacks
			* arg3 :OnConnectionFailedListener
		**/
		init();
	}
	
	public void init()
	{
		buildGoogleApiClient();
		//mLocationClient = new LocationClient(MyApplication.getAppContext(), this , this);
		//mLocationClient.connect();
		
		/**
		 * Note: connect() method will take some time. So for getting the
		 * current location or getting location update at particular interval,
		 * we have to wait for the connection established.
		**/
		mGoogleApiClient.connect();
	}
	
	private synchronized void buildGoogleApiClient()
	{
	    mGoogleApiClient = new GoogleApiClient.Builder(MyApplication.getAppContext())
	        .addConnectionCallbacks(this)
	        .addOnConnectionFailedListener(this)
	        .addApi(LocationServices.API)
	        .build();
	}

	public boolean isLocationClientConnected()
	{
		if (mGoogleApiClient!=null)
			return mGoogleApiClient.isConnected();
		return false;
	}
	
	public void disconnectLocationClient()
	{
		if (mGoogleApiClient!=null)
			mGoogleApiClient.disconnect();
	}
	
	public Location getLastLocation()
	{
		if (mGoogleApiClient!=null)
		{
			if (mGoogleApiClient.isConnected())
				return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
			else
				Services.Log.warning("Fused getLastLocation, not connected."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}
	
	
	
	public void requestLocationUpdates(LocationListener listener, int minAccuracy)
	{
		initLocationRequest(1000, 0, minAccuracy);
		
		if (mGoogleApiClient!=null)
		{
			if (mGoogleApiClient.isConnected())
			{
				LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, listener);
			}
			else
			{
				Services.Log.warning("Fused requestLocationUpdates, not connected."); //$NON-NLS-1$ //$NON-NLS-2$
				storeParameters(1000, 0, minAccuracy);
				mIsBackgroundRequest = false;
				mLocationListener = listener;
				mRequestLocationUpdatesPending = true;
			}
		}
		
		
	}

	public void requestLocationUpdatesBackground(int minTime, int minDistance, int trackingAccuracy)
	{
		initLocationRequest(minTime, minDistance, trackingAccuracy);
		
		if (mGoogleApiClient!=null)
		{
			if (mGoogleApiClient.isConnected())
			{
				//request location update in background
				requestLocationUpdateBackgroundToReceiver();
			}
			else
			{
				Services.Log.warning("Fused requestLocationUpdatesBackground, not connected."); //$NON-NLS-1$ //$NON-NLS-2$
				mIsBackgroundRequest = true;
				storeParameters(minTime, minDistance, trackingAccuracy);
				mRequestLocationUpdatesPending = true;
			}
		}
		
		
	}

	private void requestLocationUpdateBackgroundToReceiver() 
	{
		PendingIntent locationIntent = getLocationPendingIntent();
		LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, locationIntent);
	}

	private PendingIntent getLocationPendingIntent() 
	{
		//request location update in background
		Intent intent = new Intent(MyApplication.getAppContext(), LocationFusedProviderReceiver.class);
		return PendingIntent.getBroadcast(MyApplication.getAppContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}
	
	private void storeParameters(int minTime, int minDistance, int minAccuracy)
	{
		mMinTimeRequest = minTime;
		mMinDistanceRequest = minDistance;
		mMinAccuracyRequest = minAccuracy;
	}
	
	
	private void initLocationRequest(long updateInteval, int minDistance , int minAccuracy) 
	{
		// Create a new global location parameters object
		mLocationRequest = LocationRequest.create();
				 
		//Set the update interval, only override when use tracking., if not update each 1 seconds
		mLocationRequest.setInterval(updateInteval);
		mLocationRequest.setFastestInterval(updateInteval);
				
		if (minDistance>0)
			mLocationRequest.setSmallestDisplacement(minDistance);
		
		// from http://www.intelligrape.com/blog/googles-fused-location-api-for-android/
		// and https://developer.android.com/training/location/receive-location-updates.html
		if (minAccuracy<= 0)
			mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
		else if (minAccuracy<= 20)
			// 	Use high accuracy
			mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		else if (minAccuracy<= 100)
			mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
		else 
			mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
		
		//  includeHeadingAndSpeed ?
		
	}
	
	
	public void removeLocationUpdates(LocationListener listener)
	{
		if (mGoogleApiClient!=null)
		{
			if (mGoogleApiClient.isConnected())
			{
				LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, listener);
				
				LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, getLocationPendingIntent());
			}
			else
			{
				Services.Log.warning("Fused removeLocationUpdates, not connected."); //$NON-NLS-1$ //$NON-NLS-2$
				mRequestLocationUpdatesPending = false;
			}
		}
		
	}
	
	
	
	@Override
	public void onConnected(Bundle arg0) 
	{
		Services.Log.debug("GooglePlayServicesClient onConnected!!!");
		
		if (mRequestLocationUpdatesPending)
		{
			if (mGoogleApiClient!=null)
			{
				Services.Log.debug("Fused requestLocationUpdates, after connected."); //$NON-NLS-1$ //$NON-NLS-2$
				initLocationRequest(mMinTimeRequest, mMinDistanceRequest, mMinAccuracyRequest);
				if (mIsBackgroundRequest)
				{
					//request location update in background
					requestLocationUpdateBackgroundToReceiver();
				}
				else
				{
					LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, mLocationListener);
				}
			}
		}
		mRequestLocationUpdatesPending = false;
	}

	
	@Override
	public void onConnectionFailed(ConnectionResult arg0) 
	{
		Services.Log.debug("GooglePlayServicesClient onConnectionFailed");
	}

	@Override
	public void onConnectionSuspended(int arg0) 
	{
		Services.Log.debug("GooglePlayServicesClient onConnectionSuspended");
		
	}
	
}
