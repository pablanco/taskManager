package com.artech.android.beacons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;

import com.artech.base.model.Entity;
import com.artech.base.utils.Strings;
import com.artech.utils.Cast;

public class GxBeaconRegion
{
	private String mId;
	private GxBeaconInfo mBeaconMatch;

	public static final int STATE_UNKNOWN = 0;
	public static final int STATE_INSIDE = 1;
	public static final int STATE_OUTSIDE = 2;
	
	public GxBeaconRegion(String id, GxBeaconInfo beaconMatch)
	{
		mId = id;
		mBeaconMatch = beaconMatch;
	}

	public GxBeaconRegion(Region region)
	{
		mId = region.getUniqueId();
		
		String beaconUuid = (region.getId1() != null ? region.getId1().toString() : Strings.EMPTY);
		int beaconGroupId = (region.getId2() != null ? region.getId2().toInt() : 0);
		int beaconId =  (region.getId3() != null ? region.getId3().toInt() : 0);
		mBeaconMatch = new GxBeaconInfo(beaconUuid, beaconGroupId, beaconId);
	}

	public GxBeaconRegion(Entity sdt)
	{
		mId = sdt.optStringProperty("Identifier");
		mBeaconMatch = new GxBeaconInfo(Cast.as(Entity.class, sdt.getProperty("BeaconMatch")));
	}

	public static List<GxBeaconRegion> newCollection(Collection<Region> regions)
	{
		ArrayList<GxBeaconRegion> list = new ArrayList<GxBeaconRegion>();
		for (Region region : regions)
			list.add(new GxBeaconRegion(region));

		return list;
	}

	public String getId()
	{
		return mId;
	}

	public GxBeaconInfo getBeaconMatch()
	{
		return mBeaconMatch;
	}

	public Region toRegion()
	{
		Identifier id1 = null;
		if (Strings.hasValue(mBeaconMatch.getUuid()))
			id1 = Identifier.parse(mBeaconMatch.getUuid());

		Identifier id2 = null;
		if (mBeaconMatch.getGroupId() != 0)
			id2 = Identifier.fromInt(mBeaconMatch.getGroupId());

		Identifier id3 = null;
		if (mBeaconMatch.getId() != 0)
			id3 = Identifier.fromInt(mBeaconMatch.getId());

		return new Region(mId, id1, id2, id3);
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof GxBeaconRegion))
			return false;

		GxBeaconRegion other = (GxBeaconRegion)o;
		return mId.equals(other.mId);
	}

	@Override
	public int hashCode()
	{
		return mId.hashCode();
	}
}
