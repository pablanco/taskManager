package com.artech.fragments;

import com.artech.actions.UIContext;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.layout.LayoutDefinition;
import com.artech.base.model.Entity;
import com.artech.controllers.IDataViewController;
import com.artech.ui.Anchor;

public interface IDataView
{
	/**
	 * Gets the definition of the Data View.
	 */
	IViewDefinition getDefinition();

	/**
	 * Gets the current mode of the Data View.
	 */
	short getMode();

	/**
	 * Returns the layout definition of the Data View.
	 */
	LayoutDefinition getLayout();

	/**
	 * Gets the controller associated to this data view.
	 */
	IDataViewController getController();

	/**
	 * Instructs the data view to run the specified action (with its own context and data).
	 */
	void runAction(ActionDefinition action, Anchor anchor);

	/**
	 * Gets the UIContext associated to the data view (used to execute actions).
	 */
	UIContext getUIContext();

	/**
	 * Gets the entity corresponding to the Data View data.
	 * @return
	 */
	Entity getContextEntity();

	/**
	 * Gets whether the data view is "active" (e.g. not on a hidden tab).
	 */
	boolean isActive();
	
	/**
	 * Sets whether the data view is "active" (e.g. not on a hidden tab).
	 */
	void setActive(boolean value);
	
	/**
	 * Gets whether the data view is "active" (e.g. not on a hidden tab).
	 */
	boolean isDataReady();

	/**
	 * Refreshes the data associated to this component.
	 */
	void refreshData(boolean keepPosition);
}
