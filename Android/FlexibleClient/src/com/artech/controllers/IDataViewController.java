package com.artech.controllers;

import com.artech.actions.UIContext;
import com.artech.activities.ActivityController;
import com.artech.app.ComponentId;
import com.artech.app.ComponentParameters;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.model.Entity;
import com.artech.fragments.IDataView;

public interface IDataViewController
{
	ComponentId getId();

	ActivityController getParent();
	DataViewModel getModel();
	Iterable<IDataSourceController> getDataSources();
	IDataSourceController getDataSource(int id);

	IDataViewDefinition getDefinition();
	ComponentParameters getComponentParams();

	void onFragmentStart(IDataView dataView);
	void attachDataController(IDataSourceBoundView view);

	void runAction(UIContext context, ActionDefinition action, Entity data);
	boolean handleSelection(Entity entity);
}
