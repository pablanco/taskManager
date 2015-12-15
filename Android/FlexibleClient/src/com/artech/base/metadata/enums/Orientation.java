package com.artech.base.metadata.enums;

import com.artech.base.utils.Strings;

public enum Orientation
{
	UNDEFINED,
	PORTRAIT,
	LANDSCAPE;

	@Override
	public String toString()
	{
		switch(this)
		{
			case PORTRAIT : return "Portrait"; //$NON-NLS-1$
			case LANDSCAPE : return "Landscape"; //$NON-NLS-1$
			default : return "Undefined"; //$NON-NLS-1$
		}
	}

	public static Orientation parse(String value)
	{
		if (Strings.hasValue(value))
		{
			if (value.equalsIgnoreCase("Portrait")) //$NON-NLS-1$
				return PORTRAIT;
			else if (value.equalsIgnoreCase("Landscape")) //$NON-NLS-1$
				return LANDSCAPE;
		}

		return UNDEFINED; // Empty or "Any".
	}

	public static Orientation opposite(Orientation orientation)
	{
		if (orientation == PORTRAIT)
			return LANDSCAPE;
		else if (orientation == LANDSCAPE)
			return PORTRAIT;
		else
			return UNDEFINED;
	}
}
