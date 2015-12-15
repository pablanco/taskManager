package com.artech.android.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;

import com.artech.actions.Action;
import com.artech.actions.ActionExecution;
import com.artech.actions.ActionFactory;
import com.artech.actions.ActionParameters;
import com.artech.actions.UIContext;
import com.artech.android.GooglePlayServicesHelper;
import com.artech.application.MyApplication;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.DashboardItem;
import com.artech.base.metadata.DashboardMetadata;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.base.utils.ThreadUtils;

@SuppressWarnings("ResourceType") // This is to avoid the "missing FINE_LOCATION permission in manifest" lint warning.
public class LocationHelper
{
	private static Location newCurrentLocation = null;
	private static Integer secondsforOldLocation = 20;
	
	// Helper for fused provider.
	public static LocationFusedProviderHelper fusedHelper = null;
		
	public static Location getLastKnownLocation()
	{
		Location locationResult = null;
		if (fusedHelper!= null)
		{
			locationResult = fusedHelper.getLastLocation();
		}
		if (locationResult!=null)
			return locationResult;
		return getLastKnownLocationFromProviders();
	}

	private static Location getLastKnownLocationFromProviders()
	{
		LocationManager aLocationManager = (LocationManager) MyApplication.getInstance().getSystemService(Context.LOCATION_SERVICE);
		if (aLocationManager != null) {
			// Should get the last in time location, comparing location.getTime() ?
			Location gpsLocation = aLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			Location networkLocation = aLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			Location location = getLastLocationFromProviders(gpsLocation, networkLocation);
			if (location != null)
			{
				Services.Log.info("getLastKnownLocation", "get location from GPS_PROVIDER or NETWORK_PROVIDER "); //$NON-NLS-1$ //$NON-NLS-2$ 
				return location;
			}
			else 
			{
				//try get last location from passive location provider
				Location passiveLocation = aLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
				if (passiveLocation != null)
				{
					Services.Log.info("getLastKnownLocation", "get location from PASSIVE_PROVIDER"); //$NON-NLS-1$ //$NON-NLS-2$ 
					return passiveLocation;
				}
			}

			Criteria crit = new Criteria();
			crit.setAccuracy(Criteria.ACCURACY_FINE);
			String provider = aLocationManager.getBestProvider(crit, true);
			if (provider == null)
				return null;
			location = aLocationManager.getLastKnownLocation(provider);
			if (location != null)
				return location;

		}
		return null;
	}

	private static Location getLastLocationFromProviders(Location gpsLocation,
			Location networkLocation) 
	{
		/*
		//get the last location by time, not working in some cases
		if (gpsLocation!=null && networkLocation!=null)
		{
			if (gpsLocation.getTime()>networkLocation.getTime())
				return gpsLocation;
			else
				return networkLocation;
		}
		*/
		if (gpsLocation!=null)
			return gpsLocation;
		return networkLocation;
	}

		
	public static void createFusedLocationHelper()
	{
		if (GooglePlayServicesHelper.isPlayServicesAvailable(MyApplication.getAppContext()) )
		{
			Services.Log.info("createFusedLocationHelper", "Use fused Helper, get."); //$NON-NLS-1$ //$NON-NLS-2$
			if (LocationHelper.fusedHelper==null)
			{
				Services.Log.info("createFusedLocationHelper", "Use fused Helper, create."); //$NON-NLS-1$ //$NON-NLS-2$
				LocationHelper.fusedHelper =  new LocationFusedProviderHelper();
			}
			else if (!LocationHelper.fusedHelper.isLocationClientConnected())
			{
				Services.Log.info("createFusedLocationHelper", "Use fused Helper, reconnecting."); //$NON-NLS-1$ //$NON-NLS-2$
				LocationHelper.fusedHelper.init();
			}
		}
	}
	
	public static void requestLocationUpdates(Integer minTime, Integer minDistance, boolean includeHeadingAndSpeed,
			String action, Integer actionInterval)
	{
		LocationManager locationManager = (LocationManager) MyApplication.getInstance().getSystemService(Context.LOCATION_SERVICE);

		String provider = getBestProviderFromCriteria(includeHeadingAndSpeed,
				locationManager);

		Services.Log.info("requestLocationUpdates", "minTime: " + String.valueOf(minTime) + " minDistance " + String.valueOf(minDistance)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		lastLocationActionTime = new Date();
		mAction = action ;
		mActionInterval = actionInterval;
		
		
		if (provider!=null)
		{
			Services.Log.info("requestLocationUpdates", "using provider: " + provider); //$NON-NLS-1$ //$NON-NLS-2$	
			locationManager.requestLocationUpdates(provider, minTime, minDistance, locationListener);
		}
	}

	public static void requestLocationUpdates(LocationFusedProviderHelper fusedHelper,Integer minDistance)
	{
		Services.Log.info("requestLocationUpdates", "using fusedHelper."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		fusedHelper.requestLocationUpdates(locationListenerFused, minDistance);
	}
	
	public static void requestLocationUpdatesTracking(Integer minTime, Integer minDistance, boolean includeHeadingAndSpeed,
			String action, Integer actionInterval, Integer trackingAccuracy)
	{
		Services.Log.info("requestLocationUpdates", "using fusedHelper for tracking."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	
		lastLocationActionTime = new Date();
		mAction = action ;
		mActionInterval = actionInterval;
		
		fusedHelper.requestLocationUpdatesBackground( minTime, minDistance, trackingAccuracy);
	}
	
	public static Location getLocationGeoLocationInfo(Integer minAccuracy, Integer timeout, boolean includeHeadingAndSpeed)
	{
		Date startTime = new Date();

		//default to return
		Location location = null;

		LocationManager locationManager = (LocationManager) MyApplication.getInstance().getSystemService(Context.LOCATION_SERVICE);
		String provider = getBestProviderFromCriteria(includeHeadingAndSpeed, locationManager);

		if (fusedHelper!= null)
		{
			if (fusedHelper.isLocationClientConnected())
			{
				location = fusedHelper.getLastLocation();
			}
			else
			{
				//wait until connected at least 1 seconds.
				if (timeout<1)
					timeout =1;
			}
		}
		if (provider!=null && location==null)
		{
			Services.Log.info("getLocationGeoLocationInfo", "provider 1: " + provider); //$NON-NLS-1$ //$NON-NLS-2$
			location = locationManager.getLastKnownLocation(provider);
		}

		long difLocInSeconds = 0;
		if (location!=null)
		{
			difLocInSeconds = getDifInSeconds(location.getTime(), startTime.getTime());
			Services.Log.info("getLocationGeoLocationInfo", "getLastKnownLocation 11 "+ location.toString() ); //$NON-NLS-1$ //$NON-NLS-2$
			
		}

		boolean isvalidLocation = true;
		while(location==null //has no location
				|| (minAccuracy!=0 && (!location.hasAccuracy() || location.getAccuracy()> minAccuracy)) //has not accuracy
				|| difLocInSeconds > secondsforOldLocation // is old location 
				|| !isvalidLocation) //is different for last know location
		{
			//wait one sec to new location to arrive
			ThreadUtils.sleep(1000);
			Services.Log.info("getLocationGeoLocationInfo", "wait one sec to new location to arrive 2"); //$NON-NLS-1$ //$NON-NLS-2$
			
			if (newCurrentLocation!=null)
			{
				Location lastLocation = null;
				if (location!=null)
				{
					if (lastLocation==null)
						lastLocation = location;
					if (lastLocation.getTime()==newCurrentLocation.getTime())
					{
						isvalidLocation = false;
						Services.Log.info("getLocationGeoLocationInfo", "obtain new location 3a same time "); //$NON-NLS-1$ //$NON-NLS-2$
					}
					else
					{
						isvalidLocation = true;
						Services.Log.info("getLocationGeoLocationInfo", "obtain new location 3b different time "); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
				location = newCurrentLocation;
				Services.Log.info("getLocationGeoLocationInfo", "obtain new location 3 "+ newCurrentLocation.toString() ); //$NON-NLS-1$ //$NON-NLS-2$
				// discard last know location, only new one is returned
				if (lastLocation!=null)
					difLocInSeconds = getDifInSeconds(location.getTime(), lastLocation.getTime());
				else
					difLocInSeconds = getDifInSeconds(location.getTime(), startTime.getTime());
			}
			Date endTime = new Date();
			long difInSeconds = getDifInSeconds(startTime.getTime(), endTime.getTime());
			if (difInSeconds>timeout)
			{
				Services.Log.info("getLocationGeoLocationInfo", "break for timeout location 4 " ); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			}
		}

		//return other location if is it old one. Not working in some cases
		// or null , should call getLastBestLocation( System.currentTimeMillis()- (tenMinutes*1000) (10 min))
		/*
		if (location!=null && difLocInSeconds>tenMinutes) //is an old location.
		{
			Location lastKnowLocation = getLastKnownLocation();
			long difLocLastKnowInSeconds = getDifInSeconds(lastKnowLocation.getTime() , startTime.getTime());
			if (difLocLastKnowInSeconds<tenMinutes)
				location = lastKnowLocation;
		}
		*/

		//default to return
		if(location==null)
		{
			Services.Log.info("getLocationGeoLocationInfo", "location null 5 " ); //$NON-NLS-1$ //$NON-NLS-2$
			location = getLastKnownLocation();
			if (location!=null)
				Services.Log.info("getLocationGeoLocationInfo", "get last know location 6 " + location.toString() ); //$NON-NLS-1$ //$NON-NLS-2$
			else
				Services.Log.info("getLocationGeoLocationInfo", "get last know location 7 null " ); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return location;

	}


	//get better last location of all providers in the 10 min time interval
	// minTime = System.currentTimeMillis()-PlacesConstants.MAX_TIME(10 min)
	 public Location getLastBestLocation( long minTime) 
	 {
		 Location bestResult = null;
		 float bestAccuracy = Float.MAX_VALUE;
		 long bestTime = Long.MIN_VALUE;
		 LocationManager locationManager = (LocationManager) MyApplication.getInstance().getSystemService(Context.LOCATION_SERVICE);

		 // Iterate through all the providers on the system, keeping
		 // note of the most accurate result within the acceptable time limit.
		 // If no result is found within maxTime, return the newest Location.
		 List<String> matchingProviders = locationManager.getAllProviders();
		 for (String provider: matchingProviders) {
			 Location location = locationManager.getLastKnownLocation(provider);
			 if (location != null) {
				 float accuracy = location.getAccuracy();
				 long time = location.getTime();
		        
				 if ((time > minTime && accuracy < bestAccuracy)) {
					 bestResult = location;
					 bestAccuracy = accuracy;
					 bestTime = time;
				 }
				 else if (time < minTime && bestAccuracy == Float.MAX_VALUE && time > bestTime) {
					 bestResult = location;
					 bestTime = time;
				 }
			 }
		 }
		 return bestResult;
	 }
	
	
	public static JSONObject getLocationJsonGeoLocationInfo(Integer minAccuracy, Integer timeout, boolean includeHeadingAndSpeed)
	{
		Location location = getLocationGeoLocationInfo(minAccuracy, timeout, includeHeadingAndSpeed);
		//return result
		JSONObject result = null;
		if (location!=null)
		{
			result = locationToJson(location);
			Services.Log.info("getLocationInfo", "Location: " + location.toString()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return result;

	}

	private static long getDifInSeconds(long startTime, long endTime) {
		long dif = endTime - startTime;
		long difInSeconds = dif / 1000;
		return difInSeconds;
	}

	public static void removeLocationUpdates()
	{
		LocationManager locationManager = (LocationManager) MyApplication.getInstance().getSystemService(Context.LOCATION_SERVICE);
		Services.Log.info("removeLocationUpdates", "using locationManager."); //$NON-NLS-1$ //$NON-NLS-2$
		locationManager.removeUpdates( locationListener);
	}

	public static void removeLocationUpdates(LocationFusedProviderHelper fusedHelper)
	{
		Services.Log.info("removeLocationUpdates", "using fusedHelper."); //$NON-NLS-1$ //$NON-NLS-2$
		fusedHelper.removeLocationUpdates(locationListenerFused);
	}
	
	
	
	private static String getBestProviderFromCriteria(
			boolean includeHeadingAndSpeed, LocationManager locationManager) {
		//Calculate new location with the criteria.
		Criteria crit = new Criteria();
		crit.setAccuracy(Criteria.ACCURACY_FINE);
		crit.setAltitudeRequired(false);
		crit.setBearingRequired(includeHeadingAndSpeed);
		crit.setCostAllowed(true);
		String provider = locationManager.getBestProvider(crit, true);
		/*
		// Not strict needed 
		if (provider==null)
		{
			//fall back , get at least one enabled provider.
			List<String> providers = locationManager.getProviders(true);
			if (providers!=null && providers.size()>0)
			{
				provider = providers.get(0);
			}
		}
		*/
		return provider;
	}

	/* SDT GeoLocationInfo
	{"Parametro1":{"Location":"15,15", "Description":"desc" ,"Time":"Item1Prop2", "Precision":"Item1Value2",
		"Heading":"", "Speed":""}
	}
	 */
	private static JSONObject locationToJson(Location location) {

		JSONObject jsonProperty = new JSONObject();
		try {
			jsonProperty.put("Location", String.valueOf(location.getLatitude()) + Strings.COMMA + String.valueOf(location.getLongitude())); //$NON-NLS-1$

			jsonProperty.put("Description", "LocationInfo (" + location.getProvider() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			Date date = new Date();
			//Services.Log.info("locationToJson", "Location Time: " + location.getTime()); //$NON-NLS-1$ //$NON-NLS-2$
			date.setTime(location.getTime());
			//Services.Log.info("locationToJson", "Location Date: " + date.toString()); //$NON-NLS-1$ //$NON-NLS-2$
			jsonProperty.put("Time", Services.Strings.getDateTimeStringForServer(date) ); //$NON-NLS-1$
			Services.Log.info("locationToJson", "Location Server: " + Services.Strings.getDateTimeStringForServer(date)); //$NON-NLS-1$ //$NON-NLS-2$
			
			jsonProperty.put("Precision", String.valueOf(location.getAccuracy()) ); //$NON-NLS-1$

			if (location.hasBearing())
				jsonProperty.put("Heading", String.valueOf(location.getBearing())); //$NON-NLS-1$
			else
				jsonProperty.put("Heading", String.valueOf(-1)); //$NON-NLS-1$

			if (location.hasSpeed())
				jsonProperty.put("Speed", String.valueOf(location.getSpeed())); //$NON-NLS-1$
			else
				jsonProperty.put("Speed", String.valueOf(-1)); //$NON-NLS-1$

		} catch (JSONException e) {
			e.printStackTrace();
			Services.Log.Error("locationToJson", "Exception in JSONObject.put()", e);  //$NON-NLS-1$ //$NON-NLS-2$
		}
		return jsonProperty;
	}

	public static String getLocationString(Location myLocation)
	{
		if (myLocation!=null)
			return String.valueOf(myLocation.getLatitude()) + Strings.COMMA + String.valueOf(myLocation.getLongitude() );
		return Strings.EMPTY;
	}

	// static info for tracking.
	public static boolean isTracking = false;
	public static ArrayList<Location> locationsArray = new ArrayList<Location>();
	
	//public static Date startTrackingTime = new Date();
	public static Date lastLocationActionTime = new Date();
	public static String mAction = "";
	public static Integer mActionInterval = 0;
	
	
	private static final LocationListener locationListener = new LocationListener() 
	{

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) 
		{
			Services.Log.info("onStatusChanged", "Provider: " + provider + "Status: " + String.valueOf(status) ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			switch (status) {
            case LocationProvider.AVAILABLE:
            	//MyApplication.getInstance().showMessage("GPS available again\n");
                break;
            case LocationProvider.OUT_OF_SERVICE:
            	//MyApplication.getInstance().showMessage("GPS out of service\n");
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
            	//MyApplication.getInstance().showMessage("GPS temporarily unavailable\n");
                break;
            }
		}

		@Override
		public void onProviderEnabled(String provider) 
		{
			//MyApplication.getInstance().showMessage("Provider: " + provider + " enabled");
        	Services.Log.info("onProviderEnabled", "Provider: " + provider ); //$NON-NLS-1$ //$NON-NLS-2$
		}

		@Override
		public void onProviderDisabled(String provider) 
		{
			//MyApplication.getInstance().showMessage("Provider: " + provider + " disabled");
			Services.Log.info("onProviderDisabled", "Provider: " + provider ); //$NON-NLS-1$ //$NON-NLS-2$
		}

		@Override
		public void onLocationChanged(Location location) 
		{
			onLocationChangeHelper(location);
		}
	};

	public static void onLocationChangeHelper(Location location) 
	{
		if (location!=null)
		{
			//	update my location
			Services.Log.info("onLocationChanged", "Location: " + location.toString()); //$NON-NLS-1$ //$NON-NLS-2$
			newCurrentLocation = location;
		
			// Keep location change in tracking array if were are tracking. 
			if (LocationHelper.isTracking)
			{
				//MyApplication.getInstance().showMessage("Location change traking : " + location.toString()); //$NON-NLS-1$
				Services.Log.info("onLocationChanged", "Add location to tracking : " + location.toString() );
				LocationHelper.locationsArray.add(location);
			
				//If have a tracking action and action interval has been complete, raise the action.
				
				if (Services.Strings.hasValue(mAction))
				{
					Services.Log.debug("has an action an new location");
					
					Date nowTime = new Date();
					long difLocInSeconds = getDifInSeconds(lastLocationActionTime.getTime(), nowTime.getTime());
					
					Services.Log.debug("dif in seconds " + difLocInSeconds);
					
					if (difLocInSeconds> mActionInterval)
					{
						Services.Log.debug("time elapsed , raise new action " + mAction);
											
						// get the action and raise it.
						raiseAction(MyApplication.getAppContext(), mAction);
						
						Services.Log.debug("reset last location action time");
						lastLocationActionTime = new Date();
					}
				}
				
				
			}
		}
	}

	private static final com.google.android.gms.location.LocationListener locationListenerFused = new com.google.android.gms.location.LocationListener() 
	{

		@Override
		public void onLocationChanged(Location location) 
		{
			//	update my location
			Services.Log.info("onLocationChanged", "Location: " + location.toString()); //$NON-NLS-1$ //$NON-NLS-2$
			newCurrentLocation = location;
			
			// TODO, call to onLocationChangeHelper(location);
		}
	};
	
	private static void raiseAction(Context context, String mAction) 
	{
		Entity entityParam = new Entity(StructureDefinition.EMPTY);

		Connectivity connectivity = Connectivity.Online;
		// get connectivity from definition:
		
		ActionDefinition action = null;
		// get action from definition.
		
		// Get Main Definition, action and connectivity
		if (MyApplication.getApp().getMain() instanceof IDataViewDefinition)
		{
			IDataViewDefinition dataViewDef = (IDataViewDefinition)MyApplication.getApp().getMain();
			action = dataViewDef.getEvent(mAction);
			if (dataViewDef.getMainDataSource()!=null)
				entityParam = new Entity(dataViewDef.getMainDataSource().getStructure());
			entityParam.setExtraMembers(dataViewDef.getVariables());
			connectivity = dataViewDef.getConnectivitySupport();
		}
		else if (MyApplication.getApp().getMain() instanceof DashboardMetadata)
		{
			DashboardMetadata dashboardMetadata = ((DashboardMetadata)MyApplication.getApp().getMain());
			DashboardItem dashboardItem = dashboardMetadata.getNotificationActions().get(mAction);
			if (dashboardItem!=null)
				action = dashboardItem.getActionDefinition();
			entityParam.setExtraMembers(dashboardMetadata.getVariables());
			connectivity = dashboardMetadata.getConnectivitySupport();
		}
				
		// Warning UIContext with out activity.
		UIContext UIcontext = new UIContext(context, connectivity);
		
		if (action!= null)
		{
			Action doAction = ActionFactory.getAction(UIcontext, action, new ActionParameters(entityParam));
			ActionExecution exec = new ActionExecution(doAction);
			exec.executeAction();
		}
		else
		{
			Services.Log.Error("Tracking Action failed. Action is null");
		}
	
		
	}

	
	public static void clearLocationHistory() {
		locationsArray.clear();
		
	}

	public static JSONArray getLocationHistory(Date startDate) {
		Services.Log.info("getLocationHistory start"); //$NON-NLS-1$
		
		JSONArray arrayResult = new JSONArray();
		if (locationsArray!=null && locationsArray.size()>0)
		{
			Services.Log.info("getLocationHistory locationsArray size " + String.valueOf(locationsArray.size())); //$NON-NLS-1$
			for (int j =0; j< locationsArray.size(); j++)
			{
				Location location = locationsArray.get(j);
				if (location!=null)
				{		
					if (startDate==null || location.getTime()>startDate.getTime())
					{
						JSONObject jsonObject = LocationHelper.locationToJson(location);
						arrayResult.put(jsonObject);
						if (startDate==null)
							Services.Log.info("getLocationHistory add location date is null"); //$NON-NLS-1$
					}
					else
					{
						Services.Log.info("getLocationHistory not add location for time restriction", location.toString()); //$NON-NLS-1$
					}
				}
			}
			Services.Log.info("getLocationHistory", arrayResult.toString()); //$NON-NLS-1$
		}
		else
		{
			if (locationsArray==null)
				Services.Log.info("getLocationHistory locationsArray null"); //$NON-NLS-1$
			else
				Services.Log.info("getLocationHistory locationsArray empty"); //$NON-NLS-1$
		}
		return arrayResult;
	}

	public static Object isLocationServiceEnabled() {
		LocationManager aLocationManager = (LocationManager) MyApplication.getInstance().getSystemService(Context.LOCATION_SERVICE);
		return aLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || aLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	}
	
}
