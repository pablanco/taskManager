package com.artech.base.metadata.settings;

import java.io.Serializable;

import com.artech.base.metadata.enums.Orientation;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;
import com.artech.base.utils.Range;
import com.artech.base.utils.Strings;
import com.artech.base.utils.Version;

public class PlatformDefinition implements Serializable
{
	private static final long serialVersionUID = 1L;

	public static final int OS_ALL = 0;
	public static final int OS_ANDROID = 1;
	public static final int OS_BLACKBERRY = 2;
	public static final int OS_IOS = 3;
	public static final int OS_UNKNOWN = -1;

	public static final int NAVIGATION_DEFAULT = 0;
	public static final int NAVIGATION_FLIP = 1;
	public static final int NAVIGATION_SPLIT = 2;
	public static final int NAVIGATION_SLIDE = 3;
	public static final int NAVIGATION_UNKNOWN = -1;

	public static final int SMALLEST_WIDTH_DP_TABLET = 600;

	private String mName;
	private int mOS;
	private Range mSmallestWidthRange;
	private Version mOSVersion;
	private String mTheme;
	private int mNavigation;
	private Orientation mDefaultOrientation;

	private PlatformDefinition() { }

	public PlatformDefinition(INodeObject jsonData)
	{
		mName = jsonData.optString("@Name"); //$NON-NLS-1$
		mOS = Services.Strings.parseEnum(jsonData.optString("@OS"), "All", "Android", "Blackberry", "iOS");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		mSmallestWidthRange = parseSmallestWidthRange(jsonData);
		mOSVersion = new Version(jsonData.optString("@Version")); //$NON-NLS-1$
		mTheme = MetadataLoader.getAttributeName(jsonData.optString("@Theme")); //$NON-NLS-1$
		mNavigation = Services.Strings.parseEnum(jsonData.optString("@NavigationStyle"), "Default", "Flip", "Split", "Slide"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		mDefaultOrientation = Orientation.parse(jsonData.optString("@DefaultLayoutOrientation")); //$NON-NLS-1$
	}

	private static Range parseSmallestWidthRange(INodeObject jsonData)
	{
		if (jsonData.has("@MinimumShortestBound") || jsonData.has("@MaximumShortestBound")) //$NON-NLS-1$ //$NON-NLS-2$
		{
			int minimum = jsonData.optInt("@MinimumShortestBound"); //$NON-NLS-1$
			int maximum = jsonData.optInt("@MaximumShortestBound"); //$NON-NLS-1$
			return new Range(minimum != 0 ? minimum : null, maximum != 0 ? maximum : null);
		}
		else
		{
			// Calculate from old "Size" property.
			String size = jsonData.optString("@Size"); //$NON-NLS-1$
			if (Strings.hasValue(size))
			{
				if (size.equalsIgnoreCase("Small")) //$NON-NLS-1$
					return new Range(null, SMALLEST_WIDTH_DP_TABLET - 1);
				else if (size.equalsIgnoreCase("Medium")) //$NON-NLS-1$
					return new Range(SMALLEST_WIDTH_DP_TABLET, 719);
				else if (size.equalsIgnoreCase("Large")) //$NON-NLS-1$
					return new Range(720, null);
			}

			return new Range(null, null); // "All", unknown value, or no property.
		}
	}

	public static PlatformDefinition unknown()
	{
		PlatformDefinition unknown = new PlatformDefinition();
		unknown.mName = "Unknown"; //$NON-NLS-1$
		unknown.mOS = OS_UNKNOWN;
		unknown.mSmallestWidthRange = new Range(null, null);
		unknown.mTheme = Strings.EMPTY;
		unknown.mNavigation = NAVIGATION_UNKNOWN;
		unknown.mDefaultOrientation = Orientation.UNDEFINED;
		return unknown;
	}

	@Override
	public String toString()
	{
		return mName;
	}

	public String getName() { return mName; }
	public int getOS() { return mOS; }
	public Range getSmallestWidthRange() { return mSmallestWidthRange; }
	public Version getOSVersion() { return mOSVersion; }
	public String getTheme() { return mTheme; }
	public int getNavigation() { return mNavigation; }
	public Orientation getDefaultOrientation() { return mDefaultOrientation; }
}
