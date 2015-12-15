package com.artech.android.api;

import java.util.List;

import android.content.Context;
import android.support.annotation.NonNull;

import com.artech.actions.ExternalObjectEvent;
import com.artech.android.beacons.GxBeaconInfo;
import com.artech.android.beacons.GxBeaconManager;
import com.artech.android.beacons.GxBeaconManager.OnChangeBeaconsListener;
import com.artech.android.beacons.GxBeaconManager.OnChangeRegionListener;
import com.artech.android.beacons.GxBeaconProximityAlert;
import com.artech.android.beacons.GxBeaconRegion;
import com.artech.android.beacons.GxBeaconState;
import com.artech.application.MyApplication;
import com.artech.base.metadata.StructureDataType;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.utils.ListUtils;
import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;

public class LocationApi extends ExternalApi
{
	public static final String NAME = "LocationAPI";
	
	private static final String METHOD_ADD_PROXIMITY_ALERT = "AddBeaconProximityAlert";
	private static final String METHOD_ADD_PROXIMITY_ALERTS = "AddBeaconProximityAlerts";
	private static final String METHOD_GET_PROXIMITY_ALERTS = "GetBeaconProximityAlerts";
	private static final String METHOD_REMOVE_PROXIMITY_ALERT = "RemoveBeaconProximityAlert";
	private static final String METHOD_CLEAR_PROXIMITY_ALERTS = "ClearBeaconProximityAlerts";
	private static final String METHOD_GET_REGION_STATE = "GetBeaconRegionState";
	private static final String METHOD_START_RANGING_REGION = "StartRangingBeaconRegion";
	private static final String METHOD_GET_RANGING_REGIONS = "GetRangedBeaconRegions";
	private static final String METHOD_STOP_RANGING_REGION = "StopRangingBeaconRegion";
	private static final String METHOD_GET_BEACONS_IN_RANGE = "GetBeaconsInRange";
	private static final String METHOD_START_AS_BEACON = "StartAsBeacon";
	private static final String METHOD_STOP_AS_BEACON = "StopAsBeacon";
	
	private static final String EVENT_ENTER_BEACON_REGION = "EnterBeaconRegion";
	private static final String EVENT_EXIT_BEACON_REGION = "ExitBeaconRegion";
	private static final String EVENT_CHANGE_BEACONS_IN_REGION = "ChangeBeaconsInRange";

	private static boolean sInitialized;
	
	public static void initialize(Context context)
	{
		if (sInitialized)
			return;
		
		GxBeaconManager manager = GxBeaconManager.getInstance(context);
		final ExternalObjectEvent enterBeaconRegion = new ExternalObjectEvent(NAME, EVENT_ENTER_BEACON_REGION);
		final ExternalObjectEvent exitBeaconRegion = new ExternalObjectEvent(NAME, EVENT_EXIT_BEACON_REGION);
		final ExternalObjectEvent changeBeaconsInRange = new ExternalObjectEvent(NAME, EVENT_CHANGE_BEACONS_IN_REGION);
		
		manager.setOnEnterRegionListener(new OnChangeRegionListener()
		{
			@Override
			public void onChangeRegion(GxBeaconRegion region)
			{
				enterBeaconRegion.fire(ListUtils.<Object>listOf(regionToEntity(region)));
			}
		});
		
		manager.setOnExitRegionListener(new OnChangeRegionListener()
		{
			@Override
			public void onChangeRegion(GxBeaconRegion region)
			{
				exitBeaconRegion.fire(ListUtils.<Object>listOf(regionToEntity(region)));
			}
		});
		
		manager.setOnChangeBeaconsInRangeListener(new OnChangeBeaconsListener()
		{
			@Override
			public void onChangeBeacons(GxBeaconRegion region, List<GxBeaconState> beacons)
			{
				changeBeaconsInRange.fire(ListUtils.<Object>listOf(regionToEntity(region), beaconStatesToEntityList(beacons)));
			}
		});

		manager.restoreSavedProximityAlerts();
		sInitialized = true;
	}
	
	@Override
	public @NonNull ExternalApiResult execute(String method, List<Object> parameters)
	{
		if (!sInitialized)
			throw new IllegalStateException("You need to call initialize() first.");
		
		GxBeaconManager locationApi = GxBeaconManager.getInstance(getContext());
		
		if (METHOD_ADD_PROXIMITY_ALERT.equalsIgnoreCase(method) && parameters.size() >= 1)
		{
			GxBeaconProximityAlert alert = new GxBeaconProximityAlert((Entity)parameters.get(0));
			boolean result = locationApi.addProximityAlert(alert);
			return ExternalApiResult.success(result);
		}
		else if (METHOD_ADD_PROXIMITY_ALERTS.equalsIgnoreCase(method) && parameters.size() >= 1)
		{
			List<GxBeaconProximityAlert> alerts = GxBeaconProximityAlert.newCollection((EntityList)parameters.get(0));
			boolean result = locationApi.addProximityAlerts(alerts);
			return ExternalApiResult.success(result);
		}
		else if (METHOD_GET_PROXIMITY_ALERTS.equalsIgnoreCase(method) && parameters.size() >= 0)
		{
			List<GxBeaconProximityAlert> alerts = locationApi.getProximityAlerts();
			EntityList result = proximityAlertsToEntityList(alerts);
			return ExternalApiResult.success(result);
		}
		else if (METHOD_REMOVE_PROXIMITY_ALERT.equalsIgnoreCase(method) && parameters.size() >= 1)
		{
			String regionId = parameters.get(0).toString();
			locationApi.removeProximityAlert(regionId);
			return ExternalApiResult.SUCCESS_CONTINUE;
		}
		else if (METHOD_CLEAR_PROXIMITY_ALERTS.equalsIgnoreCase(method) && parameters.size() >= 0)
		{
			locationApi.clearProximityAlerts();
			return ExternalApiResult.SUCCESS_CONTINUE;
		}
		else if (METHOD_GET_REGION_STATE.equalsIgnoreCase(method) && parameters.size() >= 1)
		{
			String regionId = parameters.get(0).toString();
			int result = locationApi.getRegionState(regionId);
			return ExternalApiResult.success(result);
		}
		else if (METHOD_START_RANGING_REGION.equalsIgnoreCase(method) && parameters.size() >= 1)
		{
			GxBeaconRegion region = new GxBeaconRegion((Entity)parameters.get(0));
			boolean result = locationApi.startRangingRegion(region);
			return ExternalApiResult.success(result);
		}
		else if (METHOD_GET_RANGING_REGIONS.equalsIgnoreCase(method) && parameters.size() >= 0)
		{
			List<GxBeaconRegion> regions = locationApi.getRangedRegions();
			EntityList result = regionsToEntityList(regions);
			return ExternalApiResult.success(result);
		} 
		else if (METHOD_STOP_RANGING_REGION.equalsIgnoreCase(method) && parameters.size() >= 1)
		{
			String regionId = parameters.get(0).toString();
			locationApi.stopRangingRegion(regionId);
			return ExternalApiResult.SUCCESS_CONTINUE;
		} 
		else if (METHOD_GET_BEACONS_IN_RANGE.equalsIgnoreCase(method) && parameters.size() >= 1)
		{
			String regionId = parameters.get(0).toString();
			EntityList result = beaconStatesToEntityList(locationApi.getBeaconsInRange(regionId));
			return ExternalApiResult.success(result);
		}
		else if (METHOD_START_AS_BEACON.equalsIgnoreCase(method) && parameters.size() >= 1)
		{
			GxBeaconInfo beaconInfo = new GxBeaconInfo((Entity)parameters.get(0));
			boolean result = locationApi.startAsBeacon(beaconInfo);
			return ExternalApiResult.success(result);
		}
		else if (METHOD_STOP_AS_BEACON.equalsIgnoreCase(method) && parameters.size() >= 0)
		{
			boolean result = locationApi.stopAsBeacon();
			return ExternalApiResult.success(result);
		}
		else
			return ExternalApiResult.failureUnknownMethod(this, method);
	}
	
	private static final String TYPE_PROXIMITY_ALERT = "BeaconProximityAlert";
	private static final String PROP_PROXIMITY_ALERT_REGION = "BeaconRegion";
	private static final String PROP_PROXIMITY_ALERT_NOTIFY_ENTRY = "NotifyOnEntry";
	private static final String PROP_PROXIMITY_ALERT_NOTIFY_EXIT = "NotifyOnExit";
	
	private static EntityList proximityAlertsToEntityList(List<GxBeaconProximityAlert> alerts)
	{
		EntityList list = new EntityList();
		for (GxBeaconProximityAlert alert : alerts)
			list.add(proximityAlertToEntity(alert));
		
		return list;
	}
	
	private static Entity proximityAlertToEntity(GxBeaconProximityAlert alert)
	{
		Entity entity = createSdt(TYPE_PROXIMITY_ALERT);
		entity.setProperty(PROP_PROXIMITY_ALERT_REGION, regionToEntity(alert.getRegion()));
		entity.setProperty(PROP_PROXIMITY_ALERT_NOTIFY_ENTRY, alert.shouldNotifyOnEntry());
		entity.setProperty(PROP_PROXIMITY_ALERT_NOTIFY_EXIT, alert.shouldNotifyOnExit());
		return entity;
	}
	
	private static EntityList regionsToEntityList(List<GxBeaconRegion> regions)
	{
		EntityList list = new EntityList();
		for (GxBeaconRegion region : regions)
			list.add(regionToEntity(region));
		
		return list;
	}
	
	private static Entity regionToEntity(GxBeaconRegion region)
	{
		Entity entity = createSdt("BeaconRegion");
		entity.setProperty("Identifier", region.getId());
		entity.setProperty("BeaconMatch", beaconToEntity(region.getBeaconMatch()));
		return entity;
	}
	
	private static EntityList beaconStatesToEntityList(List<GxBeaconState> states)
	{
		EntityList list = new EntityList();
		for (GxBeaconState state : states)
			list.add(beaconStateToEntity(state));
		
		return list;
	}
	
	private static Entity beaconStateToEntity(GxBeaconState state)
	{
		Entity entity = createSdt("BeaconState");
		entity.setProperty("Beacon", beaconToEntity(state.getBeacon()));
		entity.setProperty("Proximity", state.getProximity());
		entity.setProperty("Distance", state.getDistance());
		entity.setProperty("Signal", state.getSignal());
		return entity;
	}
	
	private static Entity beaconToEntity(GxBeaconInfo beacon)
	{
		Entity entity = createSdt("BeaconInfo");
		entity.setProperty("UUID", beacon.getUuid());
		entity.setProperty("GroupId", beacon.getGroupId());
		entity.setProperty("Id", beacon.getId());
		return entity;
	}

	private static Entity createSdt(String sdtTypeName)
	{
		StructureDataType sdtDefinition = MyApplication.getApp().getDefinition().getSDT(sdtTypeName);
		if (sdtDefinition == null)
			throw new IllegalArgumentException(String.format("SDT definition for '%s' is missing in the application.", sdtTypeName));
		
		return new Entity(sdtDefinition.getStructure());
	}
}
