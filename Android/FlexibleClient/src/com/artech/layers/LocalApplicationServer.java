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
import com.artech.base.model.Entity;
import com.artech.base.providers.GxUri;
import com.artech.base.providers.IApplicationServer;
import com.artech.base.providers.IDataSourceResult;
import com.artech.common.IProgressListener;

class LocalApplicationServer implements IApplicationServer
{
	@Override
	public boolean supportsCaching()
	{
		// Local DP do not process, or return, "Last-Modified" / "If-Modified-Since" tags.
		return false;
	}

	@Override
	public IBusinessComponent getBusinessComponent(String name)
	{
		return new LocalBusinessComponent(name);
	}

	@Override
	public IGxObject getGxObject(String name)
	{
		GxObjectDefinition gxObjectDefinition = MyApplication.getInstance().getGxObject(name);
		if (gxObjectDefinition != null && gxObjectDefinition instanceof ProcedureDefinition)
			return new LocalProcedure(name);
		else if (gxObjectDefinition != null && gxObjectDefinition instanceof DataProviderDefinition)
			return new LocalDataProvider(name);
		else
			return new DummyObject(name);
	}

	@Override
	public IProcedure getProcedure(String name)
	{
		return new LocalProcedure(name);
	}

	@Override
	public IDataSourceResult getData(GxUri uri, int sessionId, int start, int count, Date ifModifiedSince)
	{
		LocalDataSource ds = new LocalDataSource(uri.getDataSource());
		return ds.execute(uri, sessionId, start, count);
	}

	@Override
	public Entity getData(GxUri uri, int sessionId)
	{
		return CommonUtils.getDataSingle(this, uri, sessionId);
	}

	@Override
	public String uploadBinary(String fileExtension, String fileMimeType, InputStream data, long dataLength, IProgressListener progressListener)
	{
		return LocalBinaryHelper.upload(fileExtension, fileMimeType, data, dataLength);
	}

	@Override
	public List<String> getDependentValues(String service, Map<String, String> input)
	{
		return LocalServices.getDependentValues(service, input);
	}

	@Override
	public Map<String, String> getDynamicComboValues(String serviceName, Map<String, String> input)
	{
		return LocalServices.getDynamicComboValues(serviceName, input);
	}

	@Override
	public List<String> getSuggestions(String serviceName, Map<String, String> input)
	{
		return LocalServices.getSuggestions(serviceName, input);
	}

	@Override
	public MappedValue getMappedValue(String serviceName, Map<String, String> input)
	{
		return LocalServices.getMappedValue(serviceName, input);
	}
}
