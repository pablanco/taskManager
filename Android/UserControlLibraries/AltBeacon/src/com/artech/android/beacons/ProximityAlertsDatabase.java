package com.artech.android.beacons;

import java.util.ArrayList;
import java.util.List;

import com.artech.base.utils.NameMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class ProximityAlertsDatabase
{
	private static final int VERSION = 1;
	private SQLiteDatabase mDatabase;
	private NameMap<GxBeaconProximityAlert> mCache;

	private static interface PROXIMITY_ALERTS
	{
		static final String REGION_ID = "ProximityAlertId";
		static final String BEACON_UUID = "ProximityAlertRegionUuid";
		static final String BEACON_GROUP_ID = "ProximityAlertRegionGroupId";
		static final String BEACON_ID = "ProximityAlertRegionId";
		static final String NOTIFY_ENTER = "ProximityAlertNotifyOnEnter";
		static final String NOTIFY_EXIT = "ProximityAlertNotifyOnExit";

		static final String TABLE = "ProximityAlerts";
		static final String[] COLUMNS = new String[] { REGION_ID, BEACON_UUID, BEACON_GROUP_ID, BEACON_ID, NOTIFY_ENTER, NOTIFY_EXIT };
	}

	private static class DatabaseOpenHelper extends SQLiteOpenHelper
	{
		public DatabaseOpenHelper(Context context)
		{
			super(context, "gxbeacons.db", null, VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE " + PROXIMITY_ALERTS.TABLE + " (" +
						PROXIMITY_ALERTS.REGION_ID + " CHARACTER, " +
						PROXIMITY_ALERTS.BEACON_UUID + " CHARACTER, " +
						PROXIMITY_ALERTS.BEACON_GROUP_ID + " INTEGER, " +
						PROXIMITY_ALERTS.BEACON_ID + " INTEGER, " +
						PROXIMITY_ALERTS.NOTIFY_ENTER + " INTEGER, " + // BOOLEAN
						PROXIMITY_ALERTS.NOTIFY_EXIT + " INTEGER, " +  // BOOLEAN
						"PRIMARY KEY (" + PROXIMITY_ALERTS.REGION_ID + "))");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			// Change this if we update VERSION!
			db.execSQL("DROP TABLE IF EXISTS " + PROXIMITY_ALERTS.TABLE);
			onCreate(db);
		}
	}

	public ProximityAlertsDatabase(Context context)
	{
		DatabaseOpenHelper helper = new DatabaseOpenHelper(context);
		mDatabase = helper.getWritableDatabase();
	}

	public void addProximityAlert(GxBeaconProximityAlert alert)
	{
		ContentValues values = new ContentValues();
		values.put(PROXIMITY_ALERTS.REGION_ID, alert.getRegionId());
		values.put(PROXIMITY_ALERTS.BEACON_UUID, alert.getRegion().getBeaconMatch().getUuid());
		values.put(PROXIMITY_ALERTS.BEACON_GROUP_ID, alert.getRegion().getBeaconMatch().getGroupId());
		values.put(PROXIMITY_ALERTS.BEACON_ID, alert.getRegion().getBeaconMatch().getId());
		values.put(PROXIMITY_ALERTS.NOTIFY_ENTER, alert.shouldNotifyOnEntry() ? 1 : 0);
		values.put(PROXIMITY_ALERTS.NOTIFY_EXIT, alert.shouldNotifyOnExit() ? 1 : 0);

		invalidateCache();
		mDatabase.insertWithOnConflict(PROXIMITY_ALERTS.TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
	}

	public void addProximityAlerts(List<GxBeaconProximityAlert> alerts)
	{
		mDatabase.beginTransaction();
		try
		{
			for (GxBeaconProximityAlert alert : alerts)
				addProximityAlert(alert);

			mDatabase.setTransactionSuccessful();
		}
		finally
		{
			mDatabase.endTransaction();
		}
	}

	public List<GxBeaconProximityAlert> getProximityAlerts()
	{
		return new ArrayList<GxBeaconProximityAlert>(getCache().values());
	}

	public GxBeaconProximityAlert getProximityAlert(String regionId)
	{
		return getCache().get(regionId);
	}

	public void removeProximityAlert(String regionId)
	{
		invalidateCache();
		mDatabase.delete(PROXIMITY_ALERTS.TABLE, String.format("%s = ?", PROXIMITY_ALERTS.REGION_ID), new String[] { regionId });
	}

	public void clearProximityAlerts()
	{
		invalidateCache();
		mDatabase.delete(PROXIMITY_ALERTS.TABLE, null, null);
	}
	
	private synchronized NameMap<GxBeaconProximityAlert> getCache()
	{
		if (mCache == null)
		{
			NameMap<GxBeaconProximityAlert> cache = new NameMap<GxBeaconProximityAlert>();
			Cursor cursor = mDatabase.query(PROXIMITY_ALERTS.TABLE, PROXIMITY_ALERTS.COLUMNS, null, null, null, null, null);
			try
			{
				int colRegionId = cursor.getColumnIndexOrThrow(PROXIMITY_ALERTS.REGION_ID);
				int colBeaconUuid = cursor.getColumnIndexOrThrow(PROXIMITY_ALERTS.BEACON_UUID);
				int colBeaconGroupId = cursor.getColumnIndexOrThrow(PROXIMITY_ALERTS.BEACON_GROUP_ID);
				int colBeaconId = cursor.getColumnIndexOrThrow(PROXIMITY_ALERTS.BEACON_ID);
				int colNotifyEnter = cursor.getColumnIndexOrThrow(PROXIMITY_ALERTS.NOTIFY_ENTER);
				int colNotifyExit = cursor.getColumnIndexOrThrow(PROXIMITY_ALERTS.NOTIFY_EXIT);
	
				while (cursor.moveToNext())
				{
					String regionId = cursor.getString(colRegionId);
					String beaconUuid = cursor.getString(colBeaconUuid);
					int beaconGroupId = cursor.getInt(colBeaconGroupId);
					int beaconId = cursor.getInt(colBeaconId);
					boolean notifyEnter = (cursor.getInt(colNotifyEnter) != 0);
					boolean notifyExit = (cursor.getInt(colNotifyExit) != 0);
	
					GxBeaconRegion region = new GxBeaconRegion(regionId, new GxBeaconInfo(beaconUuid, beaconGroupId, beaconId));
					GxBeaconProximityAlert alert = new GxBeaconProximityAlert(region, notifyEnter, notifyExit);
	
					cache.put(alert.getRegionId(), alert);
				}
				
				mCache = cache;
			}
			finally
			{
				cursor.close();
			}
		}
		
		return mCache;
	}
	
	private synchronized void invalidateCache()
	{
		mCache = null;
	}
}
