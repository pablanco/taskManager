package com.artech.layers;

import com.artech.application.MyApplication;
import com.artech.base.application.IGxObject;
import com.artech.base.application.OutputResult;
import com.artech.base.metadata.DataProviderDefinition;
import com.artech.base.metadata.ObjectParameterDefinition;
import com.artech.base.model.PropertiesObject;
import com.artech.common.ServiceDataResult;
import com.artech.common.ServiceHelper;

class RemoteDataProvider implements IGxObject
{
	private final DataProviderDefinition mDefinition;

	public RemoteDataProvider(DataProviderDefinition definition)
	{
		mDefinition = definition;
	}

	@Override
	public OutputResult execute(PropertiesObject parameters)
	{
		// Call Data Provider via HTTP get.
		String uri = getUri(parameters);
		ServiceDataResult response = ServiceHelper.getDataFromProvider(uri, null, false);

		// Read the SDT output parameter.
		readOutput(response, parameters);

		// Return errors and/or messages, if any.
		return RemoteUtils.translateOutput(response);
	}

	private String getUri(PropertiesObject parameters)
	{
		return MyApplication.getApp().UriMaker.getObjectUri(mDefinition.getName(), parameters.getInternalProperties());
	}

	private void readOutput(ServiceDataResult response, PropertiesObject parameters)
	{
		if (mDefinition.getOutParameters().size() == 1)
		{
			ObjectParameterDefinition outParam = mDefinition.getOutParameters().get(0);
			parameters.setProperty(outParam.getName(), response.getData());
		}
	}
}
