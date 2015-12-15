package com.artech.controls;

import com.artech.actions.UIContext;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.model.Entity;
import com.artech.controllers.ViewData;
import com.artech.usercontrols.IGxUserControl;

/**
 * This interface should be implemented for each user control that can be used in a Grid.
 * @author GMilano
 *
 */
public interface IGridView extends IGxUserControl
{
	void addListener(GridEventsListener listener);
	void update(ViewData data);

	interface GridEventsListener
	{
		UIContext getHostUIContext();
		void requestMoreData();
		boolean runAction(UIContext context, ActionDefinition action, Entity entity);
		boolean runDefaultAction(UIContext context, Entity entity);
	}
}
