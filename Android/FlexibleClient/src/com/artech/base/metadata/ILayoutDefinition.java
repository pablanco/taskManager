package com.artech.base.metadata;

import com.artech.base.metadata.theme.ThemeApplicationBarClassDefinition;

/**
 * Base interface for layouts
 * @author matiash
 */
public interface ILayoutDefinition
{
	/**
	 * View should display the application bar.
	 */
	boolean getShowApplicationBar();

	/**
	 * Enable Header Row Pattern.
	 */
	boolean getEnableHeaderRowPattern();

	/**
	 * Theme class for the application bar when use Header Row.
	 */
	ThemeApplicationBarClassDefinition getHeaderRowApplicationBarClass();

	/**
	 * Theme class for the application bar.
	 */
	ThemeApplicationBarClassDefinition getApplicationBarClass();
}
