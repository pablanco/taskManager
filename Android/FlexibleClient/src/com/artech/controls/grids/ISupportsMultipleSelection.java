package com.artech.controls.grids;

import com.artech.base.metadata.ActionDefinition;

public interface ISupportsMultipleSelection
{
	/**
	 * Starts or ends selection mode for the grid.
	 * @param forAction Starts mode for a specific action (can be null).
	 */
	void setSelectionMode(boolean enabled, ActionDefinition forAction);

	/**
	 * Selects or deselects the specified item.
	 */
	void setItemSelected(int position, boolean selected);
}
