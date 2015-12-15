package com.artech.android.beacons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.altbeacon.beacon.Beacon;

public class GxBeaconState
{
	private GxBeaconInfo mBeacon;
	private double mDistance;
	private int mSignal;

	public static final int PROXIMITY_UNKNOWN = 0;
	public static final int PROXIMITY_IMMEDIATE = 1;
	public static final int PROXIMITY_NEAR = 2;
	public static final int PROXIMITY_FAR = 3;

	// Maximum distances (in meters) for each category
	private static final double MAX_DISTANCE_IMMEDIATE = 0.3;
	private static final double MAX_DISTANCE_NEAR = 3;

	public GxBeaconState(Beacon beacon)
	{
		mBeacon = new GxBeaconInfo(beacon);
		mDistance = beacon.getDistance();
		mSignal = beacon.getRssi();
	}

	public static List<GxBeaconState> newCollection(Collection<Beacon> beacons)
	{
		ArrayList<GxBeaconState> list = new ArrayList<GxBeaconState>();
		for (Beacon beacon : beacons)
			list.add(new GxBeaconState(beacon));

		return list;
	}

	public GxBeaconInfo getBeacon()
	{
		return mBeacon;
	}

	public double getDistance()
	{
		return mDistance;
	}
	
	public int getProximity()
	{
		if (mDistance <= 0)
			return PROXIMITY_UNKNOWN;
		else if (mDistance < MAX_DISTANCE_IMMEDIATE)
			return PROXIMITY_IMMEDIATE;
		else if (mDistance < MAX_DISTANCE_NEAR)
			return PROXIMITY_NEAR;
		else
			return PROXIMITY_FAR;
	}
	
	public int getSignal()
	{
		return mSignal;
	}
}
