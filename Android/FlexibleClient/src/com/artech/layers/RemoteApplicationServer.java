package com.artech.layers;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.artech.application.MyApplication;
import com.artech.base.application.IBusinessComponent;
import com.artech.base.application.IGxObject;
import com.artech.base.application.IProcedure;
import com.artech.base.controls.MappedValue;
import com.artech.base.metadata.DataProviderDefinition;
import com.artech.base.metadata.GxObjectDefinition;
import com.artech.base.metadata.ProcedureDefinition;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.model.Entity;
import com.artech.base.providers.GxUri;
import com.artech.base.providers.IApplicationServer;
import com.artech.base.providers.IDataSourceResult;
import com.artech.common.IProgressListener;

class RemoteApplicationServer implements IApplicationServer
{
	@Override
	public boolean supportsCaching()
	{
		// Server DPs support the "Last-Modified" / "If-Modified-Since" tags.
		return true;
	}

	@Override
	public IBusinessComponent getBusinessComponent(String name)
	{
		StructureDefinition bcDefinition = MyApplication.getInstance().getBusinessComponent(name);
		return new RemoteBusinessComponent(name, bcDefinition);
	}

	@Override
	public IGxObject getGxObject(String name)
	{
		GxObjectDefinition gxObjectDefinition = MyApplication.getInstance().getGxObject(name);
		if (gxObjectDefinition != null && gxObjectDefinition instanceof ProcedureDefinition)
			return new RemoteProcedure(name, (ProcedureDefinition)gxObjectDefinition);
		else if (gxObjectDefinition != null && gxObjectDefinition instanceof DataProviderDefinition)
			return new RemoteDataProvider((DataProviderDefinition)gxObjectDefinition);
		else
			return new DummyObject(name);
	}

	@Override
	public IProcedure getProcedure(String name)
	{
		ProcedureDefinition procDefinition = MyApplication.getInstance().getProcedure(name);
		return new RemoteProcedure(name, procDefinition);
	}

	@Override
	public IDataSourceResult getData(GxUri uri, int sessionId, int start, int count, Date ifModifiedSince)
	{
		RemoteDataSource ds = new RemoteDataSource();
		return ds.execute(uri, sessionId, start, count, ifModifiedSince);
	}

	@Override
	public Entity getData(GxUri uri, int sessionId)
	{
		return CommonUtils.getDataSingle(this, uri, sessionId);
	}

	@Override
	public List<String> getDependentValues(String service, Map<String, String> input)
	{
		return RemoteServices.getDependentValues(service, input);
	}

	@Override
	public String uploadBinary(String fileExtension, String fileMimeType, InputStream data, long dataLength, IProgressListener progressListener)
	{
		return RemoteBinaryHelper.upload(fileExtension, fileMimeType, data, dataLength, progressListener);
	}

	@Override
	public Map<String, String> getDynamicComboValues(String serviceName, Map<String, String> inputValues)
	{
		return RemoteServices.getDynamicComboValues(serviceName, inputValues);
	}

	@Override
	public List<String> getSuggestions(String serviceName, Map<String, String> input)
	{
		return RemoteServices.getSuggestions(serviceName, input);
	}

	@Override
	public MappedValue getMappedValue(String serviceName, Map<String, String> input)
	{
		return RemoteServices.getMappedValue(serviceName, input);
	}
}
