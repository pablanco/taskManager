package com.artech.controllers;

import com.artech.base.metadata.IDataSourceDefinition;

public interface IDataSourceBoundView
{
	String getDataSourceId();
	IDataSourceDefinition getDataSource();
	String getDataSourceMember();
	int getDataSourceRowsPerPage();

	void setController(IDataSourceController controller);

	boolean isActive();
	void update(ViewData data);
	boolean needsMoreData();
}
