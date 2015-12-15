package com.artech.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;

import com.artech.application.MyApplication;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.metadata.LevelDefinition;
import com.artech.base.metadata.StructureDataType;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.model.Entity;
import com.artech.base.model.PropertiesObject;
import com.artech.base.services.IAndroidSession;
import com.artech.base.services.IContext;
import com.artech.base.services.IEntity;
import com.artech.base.services.IPropertiesObject;
import com.artech.base.services.Services;
import com.artech.base.synchronization.SynchronizationHelper;
import com.artech.base.utils.Strings;
import com.artech.common.ImageHelper;
import com.artech.common.ServiceHelper;
import com.artech.common.StorageHelper;
import com.artech.synchronization.ISynchronizationHelper;

public class ContextImpl implements IContext
{
	private Context mContext;

	public ContextImpl(Context context) {
		if (context == null)
			mContext = MyApplication.getInstance().getBaseContext();
		else
			mContext = context;
	}

	public Context getContext() {
		return mContext;
	}

	@Override
	public void saveMinorVersion(String prefFileName,  long value) {
		SharedPreferences settings = mContext.getSharedPreferences(prefFileName, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putLong("MinorVersion", value); //$NON-NLS-1$
		editor.commit();
	}

	@Override
	public long getMinorVersion(String prefFileName, long def)
	{
		SharedPreferences settings = mContext.getSharedPreferences(prefFileName, 0);
		// SharedPreferences.Editor editor = settings.edit();
		return settings.getLong("MinorVersion", def); //$NON-NLS-1$
	}

	@Override
	public int getResource(String data, String namespace) {
		return mContext.getResources().getIdentifier(data, namespace, mContext.getPackageName());
		//return 0;
	}

	@Override
	public int getDataImageResourceId(String imageUri)
	{
		return ImageHelper.getDataImageResourceId(imageUri);
	}
	
	@Override
	public InputStream openFileInput(String name)
	{
		try
		{
			return mContext.openFileInput(name);
		}
		catch (FileNotFoundException e)
		{
			return null;
		}
	}

	@Override
	public InputStream openRawResource(int arg0) {
		return mContext.getResources().openRawResource(arg0);
	}

	@Override
	public InputStream getResourceStream(String data, String namespace) {
		int resId = getResource(data, namespace);
		if (resId!=0)
			return openRawResource(resId);
		return null;
	}

	@Override
	public String getDataBaseFilePath()
	{
		File dbDirectory = StorageHelper.getStorageDirectory("db", useExternalStorage());
		String oldPath = dbDirectory.getAbsolutePath() + "/" + MyApplication.getApp().getName() + ".sqlite";
		// if exists use old name
		File oldFile = new File(oldPath);
		if (oldFile.exists())
			return oldPath;
		// new name
		return dbDirectory.getAbsolutePath() + "/" + MyApplication.getApp().getAppEntry().toLowerCase(Locale.US) + ".sqlite";
	}

	@Override
	public String getDataBaseSyncFilePath()
	{
		File dbDirectory = StorageHelper.getStorageDirectory("db", useExternalStorage());
		return dbDirectory.getAbsolutePath() + "/" + MyApplication.getApp().getAppEntry().toLowerCase(Locale.US) + "_sync" + ".data";
	}

	@Override
	public String getDataBaseSyncHashesFilePath()
	{
		File dbDirectory = StorageHelper.getStorageDirectory("db", useExternalStorage());
		return dbDirectory.getAbsolutePath() + "/" + MyApplication.getApp().getAppEntry().toLowerCase(Locale.US) + "_hashes" + ".json";
	}

	@Override
	public String getFilesSubApplicationDirectory(String directoryName)
	{
		File blobsUploadDirectory = StorageHelper.getStorageDirectory(directoryName, useExternalStorage());
		return blobsUploadDirectory.getAbsolutePath();
	}
	
	@Override
	public String getFilesBlobsApplicationDirectory()
	{
		File blobsDirectory = StorageHelper.getStorageDirectory("blobs", useExternalStorage());
		return blobsDirectory.getAbsolutePath();
	}

	@Override
	public String getApplicationDataPath()
	{
		File appDirectory = StorageHelper.getStorageDirectory(false);
		return appDirectory.getAbsolutePath();
	}
	
	@Override
	public String getTemporaryFilesPath()
	{
		File tempDirectory = StorageHelper.getCacheStorageDirectory(false);
		return tempDirectory.getAbsolutePath();
	}
	
	@Override
	public String getExternalFilesPath()
	{
		File appExtDirectory = StorageHelper.getStorageDirectory(true);
		return appExtDirectory.getAbsolutePath();
	}
	
	private static boolean useExternalStorage()
	{
		return (!MyApplication.getApp().getUseInternalStorageForDatabase());
	}

	@Override
	public IPropertiesObject getEmptyPropertiesObject()
	{
		return new PropertiesObject();
	}

	@Override
	public IEntity createEntity(String module, String name, IEntity parent)
	{
		return createEntity(module, name);
	}

	private Entity createEntity(String module, String name)
	{
		// "name" can be a DP, BC or SDT structure name, or an inner part thereof.
		String[] nameComponents = Services.Strings.split(name, Strings.DOT);
		String rootName = nameComponents[0];

		if (Strings.hasValue(module))
		{
			rootName = module + "." + rootName;
		}
		
		// Get the main definition first...
		StructureDefinition structure = getStructureDefinition(rootName);
		if (structure != null)
		{
			if (nameComponents.length > 1)
			{
				// ... then look up the inner part (e.g. SDT subitem, BC level).
				String[] innerNames = new String[nameComponents.length - 1];
				System.arraycopy(nameComponents, 1, innerNames, 0, nameComponents.length - 1);

				LevelDefinition level = structure.Root;
				for (int i = 0; level != null && i < innerNames.length ; i++)
					level = level.getLevel(innerNames[i]);

				if (level != null)
					return new Entity(structure, level, null); // Inner-level structure.

			}
			else
				return new Entity(structure); // Root-level structure.
		}

		Services.Log.Error(String.format("Name '%s' not found as a structure. Returning generic entity.", name));
		return new Entity(StructureDefinition.EMPTY);
	}

	private StructureDefinition getStructureDefinition(String name)
	{
		IDataSourceDefinition dataSource = MyApplication.getInstance().getDataSource(name);
		if (dataSource != null)
			return dataSource.getStructure();

		StructureDefinition bc = MyApplication.getInstance().getBusinessComponent(name);
		if (bc != null)
			return bc;

		StructureDataType sdt = MyApplication.getInstance().getSDT(name);
		if (sdt != null)
			return sdt.getStructure();

		return null;
	}

	private static AndroidSession androidSession = new AndroidSession();

	@Override
	public IAndroidSession getAndroidSession()
	{
		return androidSession;
	}

	@Override
	public boolean getUseUtcConversion()
	{
		if (Services.Application != null && Services.Application.getPatternSettings() != null)
			return Services.Application.getPatternSettings().useUtcConversion();
		else
			return true;
	}

	@Override
	public int getRemoteHandle()
	{
		return MyApplication.getApp().getRemoteHandle();
	}

	@Override
	public String makeImagePath(String imagePartialPath)
	{
		return MyApplication.getApp().UriMaker.MakeImagePath(imagePartialPath);
	}

	@Override
	public String getBaseImagesUri()
	{
		return MyApplication.getApp().UriMaker.getBaseImagesUri();
	}

	@Override
	public String getRootUri()
	{
		return MyApplication.getApp().UriMaker.getRootUri();
	}
	
	@Override
	public boolean getSynchronizerSavePendingEvents()
	{
		return MyApplication.getApp().getSynchronizerSavePendingEvents();
	}

	@Override
	public String getLanguageName()
	{
		return Services.Resources.getCurrentLanguage();
	}

	@Override
	public ISynchronizationHelper getSynchronizationHelper() {
		return new SynchronizationHelper();
	}

	@Override
	public void addSDHeaders(String host, String baseUrl, Hashtable<String, String> headerToSend) {
		ServiceHelper.Headers.addSDHeader(host, baseUrl, headerToSend);
	}

}
