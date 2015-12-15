package com.artech.android.beacons;

import org.altbeacon.beacon.Beacon;

import com.artech.base.model.Entity;

public class GxBeaconInfo
{
	private String mUuid;
	private int mGroupId;
	private int mId;

	public GxBeaconInfo()
	{
		mUuid = "";
		mGroupId = 0;
		mId = 0;
	}

	public GxBeaconInfo(String uuid, int groupId, int id)
	{
		mUuid = uuid;
		mGroupId = groupId;
		mId = id;
	}

	public GxBeaconInfo(Beacon beacon)
	{
		mUuid = beacon.getId1().toString();
		mGroupId = beacon.getId2().toInt();
		mId = beacon.getId3().toInt();
	}

	public GxBeaconInfo(Entity sdt)
	{
		mUuid = sdt.optStringProperty("UUID");
		mGroupId = sdt.optIntProperty("GroupId");
		mId = sdt.optIntProperty("Id");
	}

	public String getUuid()
	{
		return mUuid;
	}

	public int getGroupId()
	{
		return mGroupId;
	}

	public int getId()
	{
		return mId;
	}
}
