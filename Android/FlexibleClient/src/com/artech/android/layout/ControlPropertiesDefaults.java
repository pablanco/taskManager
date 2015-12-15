package com.artech.android.layout;

import java.util.Map;

import com.artech.base.metadata.layout.LayoutDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.utils.Strings;
import com.artech.controls.grids.GridHelper;

public class ControlPropertiesDefaults extends ControlProperties
{
	private final LayoutDefinition mLayout;

	public ControlPropertiesDefaults(LayoutDefinition layout)
	{
		mLayout = layout;
	}

	/**
	 * Adds to this object the default values of the supplied properties.
	 * @return True if all default values were successfully calculated; false otherwise.
	 */
	public boolean putDefaultsFor(ControlProperties props)
	{
		boolean success = true;
		for (Map.Entry<String, Map<String, Object>> controlProperties : props.getProperties())
		{
			// Ignore properties for controls not on the screen (probably for another layout).
			LayoutItemDefinition control = mLayout.getControl(controlProperties.getKey());
			if (control != null)
			{
				if (!putDefaultsFor(control, controlProperties.getValue()))
					success = false; // Not all property defaults were obtained. Continue anyway to get as much as possible.
			}
		}

		return success;
	}

	private boolean putDefaultsFor(LayoutItemDefinition control, Map<String, Object> properties)
	{
		boolean success = true;
		for (Map.Entry<String, Object> property : properties.entrySet())
		{
			if (ControlHelper.PROPERTY_CLASS.equalsIgnoreCase(property.getKey()))
			{
				ThemeClassDefinition themeClass = control.getThemeClass();
				putProperty(control.getName(), ControlHelper.PROPERTY_CLASS, (themeClass != null ? themeClass.getName() : Strings.EMPTY));
			}
			else if (ControlHelper.PROPERTY_ENABLED.equalsIgnoreCase(property.getKey()))
			{
				boolean enabled = control.isEnabled();
				putProperty(control.getName(), ControlHelper.PROPERTY_ENABLED, Boolean.toString(enabled));
			}
			else if (ControlHelper.PROPERTY_CAPTION.equalsIgnoreCase(property.getKey()))
			{
				String caption = control.getCaption();
				putProperty(control.getName(), ControlHelper.PROPERTY_CAPTION, caption);
			}
			else if (ControlHelper.PROPERTY_VISIBLE.equalsIgnoreCase(property.getKey()))
			{
				boolean visible = control.isVisible();
				putProperty(control.getName(), ControlHelper.PROPERTY_VISIBLE, Boolean.toString(visible));
			}
			else if (GridHelper.PROPERTY_ITEM_LAYOUT.equalsIgnoreCase(property.getKey()) || GridHelper.PROPERTY_ITEM_SELECTED_LAYOUT.equalsIgnoreCase(property.getKey()))
			{
				// Ignore these special properties. They are not actually used via setProperty(), but processed later.
				// Therefore, custom values here do not need to be converted to defaults.
			}
			else
				success = false; // This property is not known, or doesn't have a logical default (e.g. Focus).
		}

		return success;
	}
}
