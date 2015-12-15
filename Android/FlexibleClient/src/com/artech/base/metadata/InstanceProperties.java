package com.artech.base.metadata;

import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.model.PropertiesObject;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;
import com.artech.base.synchronization.SynchronizationHelper.DataSyncCriteria;
import com.artech.base.synchronization.SynchronizationHelper.LocalChangesProcessing;
import com.artech.base.utils.Version;

public class InstanceProperties extends PropertiesObject
{
	private static final long serialVersionUID = 1L;
	private static String SecurityNone = "SecurityNone"; //$NON-NLS-1$

	private Version mVersion;

	@Override
	public void deserialize(INodeObject obj)
	{
		super.deserialize(obj);
		mVersion = new Version(obj.optString("@Version"));
	}

	@Override
	public Object getProperty(String name)
	{
		// Check both with and without '@'.
		Object value = super.getProperty(name);
		if (value == null)
		{
			if (name.startsWith("@"))
				name = name.substring(1);
			else
				name = "@" + name;

			value = super.getProperty(name);
		}

		return value;
	}

	public String getIntegratedSecurityLevel()
	{
		return optStringProperty("@IntegratedSecurityLevel"); //$NON-NLS-1$
	}

	public boolean notSecureInstance()
	{
		return getIntegratedSecurityLevel().equalsIgnoreCase(SecurityNone);
	}

	public Version getDefinitionVersion()
	{
		return mVersion;
	}

	public Connectivity getConnectivitySupport()
	{
		String connectivity = optStringProperty("@idConnectivitySupport");
		if (Services.Strings.hasValue(connectivity)) {
			if (connectivity.equalsIgnoreCase("idOffline")) {
				return Connectivity.Offline;
			} else if (connectivity.equalsIgnoreCase("idInherit")) {
				return Connectivity.Inherit;
			}
		}
		return Connectivity.Online;
	}

	public boolean getShowLogoutButton() {
		return getBooleanProperty("@IntegratedSecurityShowLogoutButton", true); //$NON-NLS-1$;
	}

	public String getSynchronizer() {
		return MetadataLoader.getAttributeName( optStringProperty("@Synchronizer")); //$NON-NLS-1$;
	}

	public DataSyncCriteria getSynchronizerDataSyncCriteria()
	{
		String dataSyncCriteria = optStringProperty("@idDataSyncCriteria");
		if (Services.Strings.hasValue(dataSyncCriteria)) {
			if (dataSyncCriteria.equalsIgnoreCase("idAutomatic")) {
				return DataSyncCriteria.Automatic;
			} else if (dataSyncCriteria.equalsIgnoreCase("OnAppLaunch")) {
				return DataSyncCriteria.Automatic;
			}
			else if (dataSyncCriteria.equalsIgnoreCase("ElapsedTime")) {
				return DataSyncCriteria.AfterElapsedTime;
			}
			else if (dataSyncCriteria.equalsIgnoreCase("idManual")) {
				return DataSyncCriteria.Manual;
			}
		}
		return DataSyncCriteria.Manual;
	}

	public long getSynchronizerMinTimeBetweenSync()
	{
		return optLongProperty("@idMinTimeBetweenSync");
	}

	public LocalChangesProcessing getSendLocalChangesProcessing()
	{
		String localChangesProcessing = optStringProperty("@LocalChangesProcessing");
		if (Services.Strings.hasValue(localChangesProcessing)) {
			if (localChangesProcessing.equalsIgnoreCase("WhenConnected")) {
				return LocalChangesProcessing.WhenConnected;
			} else if (localChangesProcessing.equalsIgnoreCase("UserDefined")) {
				return LocalChangesProcessing.UserDefined;
			} else if (localChangesProcessing.equalsIgnoreCase("Never")) {
				return LocalChangesProcessing.Never;
			}
		}
		return LocalChangesProcessing.UserDefined;
	}
	
	public long getSynchronizerMinTimeBetweenSends()
	{
		return optLongProperty("@MinTimeBetweenSends");
	}


}
