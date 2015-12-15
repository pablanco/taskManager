package com.artech.base.metadata.theme;

import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

/**
 * Theme class for TabControl control.
 * Created by matiash on 29/07/2015.
 */
public class TabControlThemeClassDefinition extends ThemeClassDefinition
{
	final static String CLASS_NAME = "Tab"; //$NON-NLS-1$

	public static final int TAB_STRIP_POSITION_TOP = 0;
	public static final int TAB_STRIP_POSITION_BOTTOM = 1;

	public TabControlThemeClassDefinition(ThemeDefinition theme, ThemeClassDefinition parentClass)
	{
		super(theme, parentClass);
	}

	public int getTabStripPosition()
	{
		String tabPosition = optStringProperty("tabs_position");
		if (Strings.hasValue(tabPosition) && tabPosition.equalsIgnoreCase("bottom"))
			return TAB_STRIP_POSITION_BOTTOM;
		else
			return TAB_STRIP_POSITION_TOP; // Default
	}

	public String getTabStripColor()
	{
		return optStringProperty("tab_strip_background_color");
	}

	public Integer getTabStripElevation()
	{
		Integer elevation = Services.Strings.tryParseInt(optStringProperty("tab_strip_elevation"));
		if (elevation != null)
			return Services.Device.dipsToPixels(elevation);
		else
			return null;
	}

	public String getIndicatorColor()
	{
		return optStringProperty("tab_strip_indicator_color");
	}

	public ThemeClassDefinition getSelectedPageClass()
	{
		return getRelatedClass("ThemeSelectedTabPageClassReference"); //$NON-NLS-1$
	}

	public ThemeClassDefinition getUnselectedPageClass()
	{
		return getRelatedClass("ThemeUnselectedTabPageClassReference"); //$NON-NLS-1$
	}
}
