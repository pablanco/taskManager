package com.artech.android.beacons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.RemoteException;

import com.artech.base.utils.MultiMap;
import com.artech.base.utils.NameMap;
import com.artech.base.utils.Strings;

public class GxBeaconManager
{
	private Context mContext;
	private BeaconManager mBeaconManager;
	private BackgroundPowerSaver mPowerSaver;
	private static GxBeaconManager sInstance;

	// Configuration
	private Boolean mIsAvailable;
	private ProximityAlertsDatabase mProximityAlerts;

	// Events
	private OnChangeRegionListener mEnterRegionListener;
	private OnChangeRegionListener mExitRegionListener;
	private OnChangeBeaconsListener mChangeBeaconsInRangeListener;
	
	// Cached results
	private NameMap<Integer> mRegionStates;
	private MultiMap<String, GxBeaconState> mCurrentBeacons;

	/**
	 * Get access to the single instance of this class.
	 */
	public static synchronized GxBeaconManager getInstance(Context context)
	{
		if (sInstance == null)
			sInstance = new GxBeaconManager(context);
		
		return sInstance;
	}
	
	private GxBeaconManager(Context context)
	{
		mContext = context.getApplicationContext();
		mBeaconManager = BeaconManager.getInstanceForApplication(mContext);

		// For debugging
		// mBeaconManager.setDebug(true);
		
		mProximityAlerts = new ProximityAlertsDatabase(mContext);
		mRegionStates = new NameMap<Integer>();
		mCurrentBeacons = new MultiMap<String, GxBeaconState>();
		
		// Add a BeaconParser for the iBeacon BLE layout.
		final String IBEACON_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
		mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_LAYOUT));

		// Add listeners.
		mBeaconManager.setMonitorNotifier(mMonitorNotifier);
		mBeaconManager.setRangeNotifier(mRangeNotifier);
		mBeaconManager.bind(mBeaconConsumer);

		// Add power saver so that scan period is longer when the app is in background.
		mPowerSaver = new BackgroundPowerSaver(mContext);
		mPowerSaver.toString(); // Just to prevent the "not used" warning.
	}

	public void setOnEnterRegionListener(OnChangeRegionListener listener)
	{
		mEnterRegionListener = listener;
	}
	
	public void setOnExitRegionListener(OnChangeRegionListener listener)
	{
		mExitRegionListener = listener;
	}
	
	public void setOnChangeBeaconsInRangeListener(OnChangeBeaconsListener listener)
	{
		mChangeBeaconsInRangeListener = listener;
	}
	
	public boolean isAvailable()
	{
		if (mIsAvailable == null)
		{
			// BLE was introduced in Android 4.3.
			mIsAvailable = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 &&
							BluetoothAdapter.getDefaultAdapter() != null);
		}

		return mIsAvailable;
	}

	public boolean addProximityAlert(GxBeaconProximityAlert alert)
	{
		ArrayList<GxBeaconProximityAlert> alerts = new ArrayList<GxBeaconProximityAlert>();
		alerts.add(alert);
		return addProximityAlerts(alerts);
	}

	public boolean addProximityAlerts(List<GxBeaconProximityAlert> alerts)
	{
		try
		{
			for (GxBeaconProximityAlert alert : alerts)
			{
				mProximityAlerts.addProximityAlert(alert);
				mBeaconManager.startMonitoringBeaconsInRegion(alert.getRegion().toRegion());
			}

			return true;
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public List<GxBeaconProximityAlert> getProximityAlerts()
	{
		return mProximityAlerts.getProximityAlerts();
	}

	public void removeProximityAlert(String regionId)
	{
		try
		{
			mProximityAlerts.removeProximityAlert(regionId);
			mBeaconManager.stopMonitoringBeaconsInRegion(new Region(regionId, null, null, null));
			mRegionStates.remove(regionId);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
	}

	public void clearProximityAlerts()
	{
		for (Region region : mBeaconManager.getMonitoredRegions())
			removeProximityAlert(region.getUniqueId());
		
		mProximityAlerts.clearProximityAlerts();
		mRegionStates.clear();
	}

	public int getRegionState(String regionId)
	{
		Integer state = mRegionStates.get(regionId);
		if (state != null)
			return state;
		else
			return GxBeaconRegion.STATE_UNKNOWN;
	}
	
	public boolean startRangingRegion(GxBeaconRegion region)
	{
		try
		{
			mBeaconManager.startRangingBeaconsInRegion(region.toRegion());
			return true;
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public void stopRangingRegion(String regionId)
	{
		try
		{
			Region region = new Region(regionId, null, null, null);
			mBeaconManager.stopRangingBeaconsInRegion(region);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
	}

	public List<GxBeaconRegion> getRangedRegions()
	{
		return GxBeaconRegion.newCollection(mBeaconManager.getRangedRegions());
	}

	public List<GxBeaconState> getBeaconsInRange(String regionId)
	{
		ArrayList<GxBeaconState> beacons = new ArrayList<GxBeaconState>();

		if (Strings.hasValue(regionId))
			beacons.addAll(mCurrentBeacons.get(regionId));
		else
			beacons.addAll(mCurrentBeacons.values());

		return beacons;
	}

	public boolean startAsBeacon(GxBeaconInfo beaconInfo)
	{
		return false; // Not supported.
	}

	public boolean stopAsBeacon()
	{
		return false; // Not supported.
	}

	private BeaconConsumer mBeaconConsumer = new BeaconConsumer()
	{
		@Override
		public void onBeaconServiceConnect()
		{
			// Here WHAT ?!?
			// TODO Auto-generated method stub
		}

		@Override
		public Context getApplicationContext()
		{
			return mContext.getApplicationContext();
		}

		@Override
		public void unbindService(ServiceConnection connection)
		{
			mContext.unbindService(connection);
		}

		@Override
		public boolean bindService(Intent intent, ServiceConnection connection, int mode)
		{
			return mContext.bindService(intent, connection, mode);
		}
	};

	private final MonitorNotifier mMonitorNotifier = new MonitorNotifier()
	{
		@Override
		public void didEnterRegion(Region region)
		{
			GxBeaconProximityAlert alert = mProximityAlerts.getProximityAlert(region.getUniqueId());
			if (alert != null && alert.shouldNotifyOnEntry())
			{
				if (mEnterRegionListener != null)
					mEnterRegionListener.onChangeRegion(new GxBeaconRegion(region));
			}
		}

		@Override
		public void didExitRegion(Region region)
		{
			GxBeaconProximityAlert alert = mProximityAlerts.getProximityAlert(region.getUniqueId());
			if (alert != null && alert.shouldNotifyOnExit())
			{
				if (mExitRegionListener != null)
					mExitRegionListener.onChangeRegion(new GxBeaconRegion(region));
				
			}
		}

		@Override
		public void didDetermineStateForRegion(int state, Region region)
		{
			int gxState = GxBeaconRegion.STATE_UNKNOWN;
			if (state == MonitorNotifier.INSIDE)
				gxState = GxBeaconRegion.STATE_INSIDE;
			else if (state == MonitorNotifier.OUTSIDE)
				gxState = GxBeaconRegion.STATE_OUTSIDE;
			
			mRegionStates.put(region.getUniqueId(), gxState);
		}
	};

	private final RangeNotifier mRangeNotifier = new RangeNotifier()
	{
		@Override
		public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region)
		{
			GxBeaconRegion gxRegion = new GxBeaconRegion(region);

			// Special case: Although the base API reports on every scan when ranging is enabled,
			// we DON'T fire events whenever we had 0 beacons in range and we still have 0.
			// This is done (according to the spec) to prevent "useless" events.
			if (beacons.size() == 0 && mCurrentBeacons.get(gxRegion.getId()).size() == 0)
				return;
			
			List<GxBeaconState> gxBeacons = GxBeaconState.newCollection(beacons);

			mCurrentBeacons.clear(gxRegion.getId());
			mCurrentBeacons.putAll(gxRegion.getId(), gxBeacons);

			if (mChangeBeaconsInRangeListener != null)
				mChangeBeaconsInRangeListener.onChangeBeacons(gxRegion, gxBeacons);
		}
	};
	
	public interface OnChangeRegionListener
	{
		void onChangeRegion(GxBeaconRegion region);
	}
	
	public interface OnChangeBeaconsListener
	{
		void onChangeBeacons(GxBeaconRegion region, List<GxBeaconState> beacons);
	}

	/**
	 * Loads all proximity alerts stored in the database and starts monitoring them.
	 * Should be called on app startup (or device boot).
	 */
	public void restoreSavedProximityAlerts()
	{
		List<GxBeaconProximityAlert> savedAlerts = mProximityAlerts.getProximityAlerts();
		addProximityAlerts(savedAlerts);
	}
}
