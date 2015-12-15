package com.artech.controls;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.model.EntityList;

public interface IGxGridControl extends IGxControl
{
	/**
	 * Gets the definition of the grid.
	 */
	GridDefinition getDefinition();

	/**
	 * Gets the list of entities currently displayed in the grid.
	 */
	EntityList getData();

	/**
	 * Starts or ends selection mode for the grid.
	 * When this mode is ended, selection is cleared.
	 * @param action Starts mode for a specific action (can be null).
	 */
	void setSelectionMode(boolean enabled, ActionDefinition action);
}
