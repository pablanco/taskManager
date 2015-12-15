package com.artech.base.metadata.layout;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.enums.Alignment;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.PlatformHelper;
import com.artech.utils.Cast;

/**
 * Helper class for common functionality in UI action controls (buttons and action bar items).
 * @author matiash
 */
class LayoutActionDefinitionHelper
{
	public static boolean isVisible(ILayoutActionDefinition layoutData)
	{
		return getBooleanProperty(layoutData, "@visible", true); //$NON-NLS-1$
	}

	public static boolean isEnabled(ILayoutActionDefinition layoutData)
	{
		return getBooleanProperty(layoutData, "@enabled", true); //$NON-NLS-1$
	}

	public static ThemeClassDefinition getThemeClass(ILayoutActionDefinition layoutData)
	{
		return PlatformHelper.getThemeClass(getProperty(layoutData, "@class"));
	}

	public static String getCaption(ILayoutActionDefinition layoutData)
	{
		String caption = getProperty(layoutData, "@caption"); //$NON-NLS-1$
		return Services.Resources.getTranslation(caption);
	}

	public static String getImage(ILayoutActionDefinition layoutData)
	{
		return MetadataLoader.getObjectName(getProperty(layoutData, "@image")); //$NON-NLS-1$
	}

	public static String getDisabledImage(ILayoutActionDefinition layoutData)
	{
		return MetadataLoader.getObjectName(getProperty(layoutData, "@disabledImage")); //$NON-NLS-1$
	}

	public static String getHighlightedImage(ILayoutActionDefinition layoutData)
	{
		return MetadataLoader.getObjectName(getProperty(layoutData, "@highlightedImage")); //$NON-NLS-1$
	}

	public static int getImagePosition(ILayoutActionDefinition layoutData)
	{
		return Alignment.parseImagePosition(getProperty(layoutData, "@imagePosition")); //$NON-NLS-1$
	}

	public static String getProperty(ILayoutActionDefinition layoutAction, String propertyName)
	{
		// Read from control first, then from action definition.
		// Don't use optStringProperty(), because it returns empty string when the key is not present.
		// That is not what we want, because a property (e.g. @image) may be overriden with an empty value.
		String value = Cast.as(String.class, layoutAction.getProperty(propertyName));
		if (value == null)
		{
			ActionDefinition action = layoutAction.getEvent();
			if (action != null)
				value = action.optStringProperty(propertyName);
		}

		return value;
	}

	private static boolean getBooleanProperty(ILayoutActionDefinition layoutAction, String propertyName, boolean defaultValue)
	{
		return Services.Strings.tryParseBoolean(getProperty(layoutAction, propertyName), defaultValue);
	}
}
