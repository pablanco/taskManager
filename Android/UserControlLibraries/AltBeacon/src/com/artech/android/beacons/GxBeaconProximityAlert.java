package com.artech.android.beacons;

import java.util.ArrayList;
import java.util.List;

import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.utils.Cast;

public class GxBeaconProximityAlert
{
	private GxBeaconRegion mRegion;
	private boolean mNotifyOnEntry;
	private boolean mNotifyOnExit;

	public GxBeaconProximityAlert(GxBeaconRegion region, boolean notifyEntry, boolean notifyExit)
	{
		mRegion = region;
		mNotifyOnEntry = notifyEntry;
		mNotifyOnExit = notifyExit;
	}

	public GxBeaconProximityAlert(Entity sdt)
	{
		mRegion = new GxBeaconRegion(Cast.as(Entity.class, sdt.getProperty("BeaconRegion")));
		mNotifyOnEntry = sdt.optBooleanProperty("NotifyOnEntry");
		mNotifyOnExit = sdt.optBooleanProperty("NotifyOnExit");
	}

	public static List<GxBeaconProximityAlert> newCollection(EntityList collection)
	{
		ArrayList<GxBeaconProximityAlert> list = new ArrayList<GxBeaconProximityAlert>();
		for (Entity item : collection)
			list.add(new GxBeaconProximityAlert(item));
		
		return list;
	}
	
	public String getRegionId()
	{
		return mRegion.getId();
	}
	
	public GxBeaconRegion getRegion()
	{
		return mRegion;
	}

	public boolean shouldNotifyOnEntry()
	{
		return mNotifyOnEntry;
	}

	public boolean shouldNotifyOnExit()
	{
		return mNotifyOnExit;
	}
}
