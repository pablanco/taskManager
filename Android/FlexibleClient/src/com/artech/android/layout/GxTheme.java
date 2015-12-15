package com.artech.android.layout;

import android.view.View;

import com.artech.android.animations.Transformations;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.utils.PlatformHelper;
import com.artech.controls.IGxThemeable;
import com.artech.utils.Cast;

public class GxTheme
{
	public static void applyStyle(IGxThemeable gxThemeable, String className)
	{
		applyStyle(gxThemeable, PlatformHelper.getThemeClass(className));
	}

	public static void applyStyle(IGxThemeable gxThemeable, ThemeClassDefinition themeClass)
	{
		applyStyle(gxThemeable, themeClass, false);
	}

	public static void applyStyle(IGxThemeable gxThemeable, ThemeClassDefinition themeClass, boolean allowReapply)
	{
		if (gxThemeable != null && themeClass != null)
		{
			if (allowReapply || gxThemeable.getThemeClass() != themeClass)
			{
				// Apply style
				gxThemeable.setThemeClass(themeClass);

				// Apply transformation
				Transformations.apply(Cast.as(View.class, gxThemeable), themeClass);

				// Bind the theme class so it can be accessed later.
				if (gxThemeable instanceof View)
					((View)gxThemeable).setTag(LayoutTag.CONTROL_THEME_CLASS, themeClass);
			}
		}
	}
}
