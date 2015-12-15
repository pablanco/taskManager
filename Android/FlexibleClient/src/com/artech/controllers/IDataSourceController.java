package com.artech.controllers;

import com.artech.base.metadata.IDataSourceDefinition;

public interface IDataSourceController
{
	int getId();
	String getName();
	IDataViewController getParent();
	IDataSourceDefinition getDefinition();
	DataSourceModel getModel();

	void onRequestMoreData();
}
