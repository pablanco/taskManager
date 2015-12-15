package com.artech.base.metadata.enums;

import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public enum ImageScaleType
{
	NO_SCALE,
	FILL,
	FILL_KEEPING_ASPECT,
	FIT,
	TILE;

	public static ImageScaleType parse(String str)
	{
		if (!Strings.hasValue(str))
			return FIT; // Default value.

		if (str.equalsIgnoreCase("originalSize"))
			return NO_SCALE;

		if (str.equalsIgnoreCase("scaleToFill"))
			return FILL;

		if (str.equalsIgnoreCase("scaleAspectFillKeepingAspect"))
			return FILL_KEEPING_ASPECT;

		if (str.equalsIgnoreCase("scaleToFitKeepingAspect"))
			return FIT;

		if (str.equalsIgnoreCase("tile"))
			return TILE;

		Services.Log.warning("Unknown ImageScaleType value: " + str);
		return FIT;
	}
}