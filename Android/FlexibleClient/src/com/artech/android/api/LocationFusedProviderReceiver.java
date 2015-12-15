package com.artech.android.api;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.artech.base.services.Services;
import com.google.android.gms.location.FusedLocationProviderApi;

public class LocationFusedProviderReceiver extends BroadcastReceiver {
 
	// static info for tracking.
	//public static boolean isTracking = false;
		
		
    @Override
    public void onReceive(Context context, Intent intent) {
 
        Location location = (Location) intent.getExtras().get(FusedLocationProviderApi.KEY_LOCATION_CHANGED);
        
    	Services.Log.info("LocationFusedProviderReceiver onReceive onLocationChanged", "Location: " + location); //$NON-NLS-1$ //$NON-NLS-2$
		
    	//Services.Log.info("LocationFusedProviderReceiver ", "isTracking : " + isTracking); //$NON-NLS-1$ //$NON-NLS-2$
		
        LocationHelper.onLocationChangeHelper(location); 
        
        
    }
    

}
