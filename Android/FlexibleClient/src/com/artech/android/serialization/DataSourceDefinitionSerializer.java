package com.artech.android.serialization;

import org.json.JSONException;
import org.json.JSONObject;

import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.services.Services;

public class DataSourceDefinitionSerializer implements ISerializer<IDataSourceDefinition>
{
	private static final String NAME = "DataSource"; //$NON-NLS-1$
	
	@Override
	public JSONObject serialize(IDataSourceDefinition data) throws JSONException
	{
		JSONObject json = new JSONObject();
		json.put(NAME, data.getName());
		return json;
	}

	@Override
	public IDataSourceDefinition deserialize(JSONObject json) throws JSONException
	{
		String name = json.getString(NAME);
		return Services.Application.getDataSource(name);
	}
}
