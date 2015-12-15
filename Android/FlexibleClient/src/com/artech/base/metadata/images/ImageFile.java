package com.artech.base.metadata.images;

import java.io.Serializable;

import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class ImageFile implements Serializable
{
	private static final long serialVersionUID = 1L;

	public static final int TYPE_INTERNAL = 0;
	public static final int TYPE_EXTERNAL = 1;

	private final ImageCollection mParent;
	private final String mName;
	private final int mType;
	private final String mLocation;

	private String mUri;
	private String mResourceName;

	public ImageFile(ImageCollection parent, String name, int type, String location)
	{
		mParent = parent;
		mName = name;
		mType = type;
		mLocation = location;
	}

	public String getName() { return mName; }
	public int getType() { return mType; }

	public String getUri()
	{
		if (mUri == null)
		{
			if (mType == TYPE_INTERNAL)
			{
				// Relative, for internal images.
				StringBuilder sb = new StringBuilder();
				sb.append(Services.Application.getUriMaker().getBaseImagesUri());
				sb.append("/"); //$NON-NLS-1$

				if (Services.Strings.hasValue(mParent.getBaseDirectory()))
				{
					sb.append(mParent.getBaseDirectory());
					sb.append("/"); //$NON-NLS-1$
				}

				sb.append(mLocation);
				mUri = sb.toString();
			}
			else
			{
				// Absolute, for external images.
				mUri = mLocation;
			}
		}

		return mUri;
	}

	public void setUri(String uri) {
		mUri = uri;
	}

	private static final String RESOURCE_CHAR_REPLACER = "_";

	public String getResourceName()
	{
		if (mType == TYPE_EXTERNAL)
			return null; // External images cannot have been embedded as resources.

		if (mResourceName == null)
		{
			mResourceName = Strings.toLowerCase(mLocation);
			mResourceName = mResourceName.replace(".", RESOURCE_CHAR_REPLACER).replace("/", RESOURCE_CHAR_REPLACER).replace("\\", RESOURCE_CHAR_REPLACER); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		return mResourceName;
	}
}
