package com.artech.controls;

import com.artech.base.metadata.theme.ThemeClassDefinition;

/*
 * This interface must be implemented for each control that wants to be "themeable"
 */
public interface IGxThemeable {
	/*
	 * Apply the given themeClass to associated control
	 */
	void setThemeClass(ThemeClassDefinition themeClass);
	/*
	 * Return the actual theme class for the associated control
	 */
	ThemeClassDefinition getThemeClass();
	
	/*
	 * Apply the given class to the control but it doesn't change the class for it.
	 */
	void applyClass(ThemeClassDefinition themeClass);
}
