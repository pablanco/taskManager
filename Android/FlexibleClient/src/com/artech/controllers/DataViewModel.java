package com.artech.controllers;

import java.util.HashMap;

import com.artech.app.ComponentParameters;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.providers.GxUri;

public class DataViewModel
{
	private final ComponentParameters mParams;
	private final HashMap<IDataSourceDefinition, DataSourceModel> mDataSources;
	private final Connectivity mConnectivity;

	public DataViewModel(ComponentParameters params, Connectivity connectivity)
	{
		mParams = params;
		mDataSources = new HashMap<IDataSourceDefinition, DataSourceModel>();
		mConnectivity = connectivity;

		if (params.Object instanceof IDataViewDefinition)
		{
			// Fill with default URIs.
			for (IDataSourceDefinition dataSource : ((IDataViewDefinition)params.Object).getDataSources())
			{
				GxUri uri = new GxUri(dataSource).setParameters(mParams.Parameters);
				setUri(dataSource, uri);
			}
		}
	}
	
	public ComponentParameters getParams()
	{
		return mParams;
	}
	
	public IDataViewDefinition getDefinition()
	{
		return (IDataViewDefinition)mParams.Object;
	}

	Connectivity getConnectivity()
	{
		return mConnectivity;
	}

	Iterable<DataSourceModel> getDataSources()
	{
		return mDataSources.values();
	}

	DataSourceModel getDataSource(IDataSourceDefinition dataSource)
	{
		return mDataSources.get(dataSource);
	}

	private void setUri(IDataSourceDefinition dataSource, GxUri uri)
	{
		if (dataSource.getParent() != mParams.Object)
			throw new IllegalArgumentException(String.format("Data source '%s' does not belong to this data view (%s)", dataSource.getName(), mParams.Object.getName())); //$NON-NLS-1$

		if (dataSource != uri.getDataSource())
			throw new IllegalArgumentException(String.format("Uri data source does not match model data source (%s =/= %s).", uri.getDataSource(), dataSource)); //$NON-NLS-1$

		DataSourceModel model = getDataSource(dataSource);
		if (model == null)
		{
			// Initialize new model.
			model = new DataSourceModel(this, dataSource, uri);
			mDataSources.put(dataSource, model);
		}
		else
			model.setUri(uri);
	}
}
