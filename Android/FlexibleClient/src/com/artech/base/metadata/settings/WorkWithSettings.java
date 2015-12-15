package com.artech.base.metadata.settings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.artech.base.metadata.InstanceProperties;
import com.artech.base.metadata.enums.ImageUploadModes;
import com.artech.base.model.PropertiesObject;
import com.artech.base.serialization.INodeCollection;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;

public class WorkWithSettings implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final List<PlatformDefinition> mPlatforms;
	private boolean mUseUtcConversion = true;
	private String mIDEConnectionString = "";

	private final InstanceProperties mInstanceProperties = new InstanceProperties();
	private final PropertiesObject mModelProperties = new PropertiesObject();

	private WorkWithSettings()
	{
		mPlatforms = new ArrayList<PlatformDefinition>();
	}

	WorkWithSettings(INodeObject jsonData)
	{
		this();

		// For now, only read platforms (needed to select layouts in WW).
		INodeObject rootJson = jsonData.optNode("Config"); //$NON-NLS-1$
		if (rootJson == null)
			return;

		mUseUtcConversion = rootJson.optBoolean("@ConvertTimeFromUTC", true); //$NON-NLS-1$
		mIDEConnectionString = rootJson.optString("@IDE_ConnectionString", "");

		//Read Instance Properties
		INodeObject instancePropNode = rootJson.optNode("instanceProperties"); //$NON-NLS-1$
		if (instancePropNode!=null)
			mInstanceProperties.deserialize(instancePropNode);

		//Read Model Properties
		INodeObject modelPropNode = rootJson.optNode("ModelProperties"); //$NON-NLS-1$
		if (modelPropNode!=null)
			mModelProperties.deserialize(modelPropNode);
	    //"ModelProperties": {

		readPlatforms(rootJson);
	}

	public static WorkWithSettings empty()
	{
		return new WorkWithSettings();
	}

	public boolean useUtcConversion()
	{
		return mUseUtcConversion;
	}
	
	public String getIDEConnectionString()
	{
		return mIDEConnectionString;
	}

	public PlatformDefinition getPlatform(int index)
	{
		return mPlatforms.get(index);
	}

	public PlatformDefinition getPlatform(String name)
	{
		if (!Services.Strings.hasValue(name))
		{
			// Not specified! Search for the first ALL/ALL platform.
			// Previously this was the first one, but since "General" was renamed/moved this is no longer the case.
			for (int i = 0; i < mPlatforms.size(); i++)
			{
				PlatformDefinition platform = mPlatforms.get(i);
				if (platform.getOS() == PlatformDefinition.OS_ALL && platform.getSmallestWidthRange().isAll())
					return platform;
			}

			// If none are found, then return nothing.
			return PlatformDefinition.unknown();
		}

		for (int i = 0; i < mPlatforms.size(); i++)
			if (mPlatforms.get(i).getName().equalsIgnoreCase(name))
				return mPlatforms.get(i);

		return PlatformDefinition.unknown();
	}

	public List<PlatformDefinition> getPlatforms()
	{
		return mPlatforms;
	}

	private void readPlatforms(INodeObject rootJson)
	{
		INodeObject platformsJson = rootJson.optNode("Platforms"); //$NON-NLS-1$
		if (platformsJson == null)
			return;

		INodeCollection platformCollectionJson = platformsJson.optCollection("Platform"); //$NON-NLS-1$
		int collectionSize = platformCollectionJson.length();
		for (int i = 0; i < collectionSize; i++)
			readPlatform(platformCollectionJson.getNode(i));
	}

	private void readPlatform(INodeObject platformJson)
	{
		PlatformDefinition platform = new PlatformDefinition(platformJson);
		mPlatforms.add(platform);
	}

	public InstanceProperties getInstanceProperties() {
		return mInstanceProperties;
	}

	public PropertiesObject getModelProperties() {
		return mModelProperties;
	}

	public UploadSizeDefinition getUploadSize(int sizeMode)
	{
		if (sizeMode == ImageUploadModes.MEDIUM)
		{
			return new UploadSizeDefinition(sizeMode, mModelProperties.optStringProperty("@MediumImageUploadSize"));
		}
		else if (sizeMode == ImageUploadModes.SMALL)
		{
			return new UploadSizeDefinition(sizeMode, mModelProperties.optStringProperty("@SmallImageUploadSize"));
		}
		else
		{
			//Default large or actualsize.
			return new UploadSizeDefinition(sizeMode, mModelProperties.optStringProperty("@LargeImageUploadSize"));
		}
	}
}
