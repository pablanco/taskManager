package com.artech.base.metadata.loader;

import com.artech.android.json.NodeCollection;
import com.artech.base.serialization.INodeCollection;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.IContext;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

class MetadataFile
{
	private final IContext mContext;
	private final INodeObject mFile;

	public MetadataFile(IContext context, String name)
	{
		mContext = context;
		mFile = MetadataLoader.getDefinition(context, Strings.toLowerCase(name) + ".gxapp");
	}

	public IContext getContext()
	{
		return mContext;
	}

	public INodeCollection getAttributes()
	{
		return getMetadataComponent("atts", "atts", false);
	}

	public INodeCollection getSDTs()
	{
		return getMetadataComponent("SDTs", "sdts", false);
	}

	public INodeCollection getPatternInstances()
	{
		return getMetadataComponent("Patterns", "patterns", false);
	}

	public INodeCollection getBCs()
	{
		// This one is a bit different, we want to return null if the bc_list.json file is missing.
		return getMetadataComponent("BusinessComponentList", "bc_list", true);
	}

	public INodeCollection getProcedures()
	{
		return getMetadataComponent("procs", "procs", false);
	}

	private INodeCollection getMetadataComponent(String collectionName, String oldFilename, boolean returnNullIfMissing)
	{
		if (mFile != null)
		{
			// New format, read from inside this file.
			INodeCollection members = mFile.getCollection("app");
			for (INodeObject member : members)
			{
				if (member.has(collectionName))
					return member.getCollection(collectionName);
			}

			// Should never happen.
			Services.Log.Error(String.format("Could not read '%s' node from project file.", collectionName));
		}

		// Old format, read the old file and the collection inside it.
		INodeObject oldFile = MetadataLoader.getDefinition(mContext, oldFilename);
		if (oldFile != null)
			return oldFile.getCollection(collectionName);

		// Neither new nor old.
		Services.Log.warning(String.format("Could not read '%s' from metadata, in either new or old format!", collectionName));

		if (!returnNullIfMissing)
			return new NodeCollection();
		else
			return null;
	}
}
