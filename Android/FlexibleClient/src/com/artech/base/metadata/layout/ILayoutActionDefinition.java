package com.artech.base.metadata.layout;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;

/**
 * Interface for "action controls" in layout or action bar.
 * @author matiash
 *
 */
public interface ILayoutActionDefinition
{
	String getEventName();
	ActionDefinition getEvent();

	boolean isVisible();
	boolean isEnabled();
	ThemeClassDefinition getThemeClass();

	String getCaption();
	String getImage();
	String getDisabledImage();
	String getHighlightedImage();

	Object getProperty(String name);
}
