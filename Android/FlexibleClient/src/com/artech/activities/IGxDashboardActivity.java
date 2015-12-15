package com.artech.activities;

import com.artech.actions.UIContext;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.model.Entity;

public interface IGxDashboardActivity
{
	UIContext getUIContext();
	IViewDefinition getDashboardDefinition();
	Entity getData();
}
