package com.artech.base.metadata.loader;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import android.content.SharedPreferences;

import com.artech.application.MyApplication;
import com.artech.base.metadata.IPatternMetadata;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.IContext;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.base.utils.Version;


public abstract class MetadataLoader
{
	public static String REMOTE_VERSION_URL = Strings.EMPTY;
	public static long REMOTE_VERSION = 1;
	public static int REMOTE_MAJOR_VERSION;
	public static int REMOTE_MINOR_VERSION;
	public static boolean MUST_RELOAD_METADATA = false;
	public static boolean MUST_RELOAD_APP = false;
	public static boolean READ_RESOURCES = true;
	public static boolean FILES_IN_RAW = true;

	private static final int GUID_LENGTH = 36;
	private static final String GUID_REGEX = "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}";

	public static  String getPrefsName()
	{
		return "Metadata-" + Services.Application.getName() + "-" + Services.Application.getAppEntry() + "-"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public static InputStream getFromResources(IContext context, String data)
	{
		int id = context.getResource(data, "raw"); //$NON-NLS-1$
		if (id != 0)
			return context.openRawResource(id);
		return context.getResourceStream(data, "raw"); //$NON-NLS-1$
	}

	public static String getObjectName(String guidName)
	{
		return getAttributeName(guidName);
	}

	public static String getObjectType(String guidName)
	{
		if (guidName != null && hasGuidPrefix(guidName))
			return guidName.substring(0, GUID_LENGTH);
		else
			return Strings.EMPTY;
	}

	public static String getAttributeName(String guidName)
	{
		if (guidName != null && hasGuidPrefix(guidName))
			return guidName.substring(GUID_LENGTH + 1);
		else
			return guidName;
	}

	private static boolean hasGuidPrefix(String str)
	{
		return (str != null && str.length() > 36 && Strings.toLowerCase(str.substring(0, 36)).matches(GUID_REGEX));
	}

	public abstract IPatternMetadata load(IContext context, String metadata);

	public static long getLocalVersion(IContext context)
	{
		InputStream stream = getFromResources(context, "gxversion"); //$NON-NLS-1$
		String dataInfo = Strings.EMPTY;
		try
		{
			dataInfo = Services.Strings.convertStreamToString(stream);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		if (Services.Strings.hasValue(dataInfo))
		{
			INodeObject obj = null;
			try
			{
				obj = Services.Serializer.createNode(dataInfo);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			if (obj != null)
	    		return Services.Strings.valueOf(obj.optString("version")); //$NON-NLS-1$
		}
		return -1;
	}

	public static boolean getHasAppIdInRaw(IContext context)
	{
		InputStream stream = getFromResources(context, "appid"); //$NON-NLS-1$
		String dataInfo = Strings.EMPTY;
		try
		{
			dataInfo = Services.Strings.convertStreamToString(stream);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return Services.Strings.hasValue(dataInfo);
	}

	public static INodeObject getDefinition(IContext context, String data)
	{
		InputStream stream = READ_RESOURCES ? getFromResources(context, Strings.toLowerCase(data).replace('.', '_')) : null;

		// Check if the local metadata version is older than the remote version
		// if it is older so we have to try to load the remote version.
		// This could fail due to connectivity problems so that we have to be defensive
		// and try to load the local version if something goes wrong.
		String dataFile = Services.Application.getName() + data;

		// Try to get previously downloaded resource.
		try
		{
			// get last downloaded zip version.
			SharedPreferences settings2 = MyApplication.getAppContext().getSharedPreferences(MetadataLoader.getPrefsName(), 0);
			String currentDownloadVersion = settings2.getString("DOWNLOADED_ZIP_VERSION", ""); //$NON-NLS-1$
			Version downloadVersion = new Version(currentDownloadVersion);
			Version currentVersion = new Version(MyApplication.getApp().getMajorVersion()+ "." + MyApplication.getApp().getMinorVersion());
			
			// get the files from greater version, raw or zip
			if (downloadVersion.isGreaterThan(currentVersion))
			{
				// read from previous downloaded file
				if (stream == null)
					stream = context.openFileInput(dataFile);
				//	defensing programming, if all fails read from raw
				if (stream == null)
					stream = getFromResources(context, Strings.toLowerCase(data).replace('.', '_'));
			}
			else
			{
				// read from raw
				if (stream == null)
					stream = getFromResources(context, Strings.toLowerCase(data).replace('.', '_'));
				// defensing programming, if all fails read from previous downloaded file
				if (stream == null)
					stream = context.openFileInput(dataFile);
			}
				
			String dataInfo = Services.Strings.convertStreamToString(stream);

			if (Strings.hasValue(dataInfo))
				return Services.Serializer.createNode(dataInfo);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (stream != null)
				IOUtils.closeQuietly(stream);
		}
		return null;
	}


}
