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
import com.artech.base.metadata.GxObjectDefinition;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.model.Entity;
import com.artech.base.providers.GxUri;
import com.artech.base.providers.IApplicationServer;
import com.artech.base.providers.IDataSourceResult;
import com.artech.base.services.Services;
import com.artech.common.IProgressListener;
import com.artech.utils.Cast;

public class ApplicationServer implements IApplicationServer
{
	private static final String LOG_TAG = "ApplicationServer";
	private static final boolean LOG_ENABLED = false;

	private final RemoteApplicationServer mRemoteServer = new RemoteApplicationServer();
	private final LocalApplicationServer mLocalServer = new LocalApplicationServer();
	private final Connectivity mConnectivity;

	public ApplicationServer(Connectivity connectivity)
	{
		mConnectivity = connectivity;
	}

	private IApplicationServer getDefaultServer()
	{
		return (mConnectivity == Connectivity.Offline ? mLocalServer : mRemoteServer);
	}

	@Override
	public IBusinessComponent getBusinessComponent(String name)
	{
		StructureDefinition objDef = MyApplication.getInstance().getBusinessComponent(name);
		if (objDef != null && objDef.getConnectivitySupport() == Connectivity.Online)
			return mRemoteServer.getBusinessComponent(name);
		if (objDef != null && objDef.getConnectivitySupport() == Connectivity.Offline)
			return mLocalServer.getBusinessComponent(name);

		return getDefaultServer().getBusinessComponent(name);
	}

	@Override
	public IGxObject getGxObject(String name)
	{
		GxObjectDefinition objDef = MyApplication.getInstance().getGxObject(name);
		if (objDef != null && objDef.getConnectivitySupport() == Connectivity.Online)
			return mRemoteServer.getGxObject(name);
		if (objDef != null && objDef.getConnectivitySupport() == Connectivity.Offline)
			return mLocalServer.getGxObject(name);
		// Inherit
		return getDefaultServer().getGxObject(name);
	}

	@Override
	public IProcedure getProcedure(String name)
	{
		return Cast.as(IProcedure.class, getGxObject(name));
	}

	@Override
	public boolean supportsCaching()
	{
		return getDefaultServer().supportsCaching();
	}

	@Override
	public IDataSourceResult getData(GxUri uri, int sessionId, int start, int count, Date ifModifiedSince)
	{
		return getDefaultServer().getData(uri, sessionId, start, count, ifModifiedSince);
	}

	@Override
	public Entity getData(GxUri uri, int sessionId)
	{
		return getDefaultServer().getData(uri, sessionId);
	}

	@Override
	public List<String> getDependentValues(String service, Map<String, String> input)
	{
		return getDefaultServer().getDependentValues(service, input);
	}

	@Override
	public String uploadBinary(String fileExtension, String fileMimeType, InputStream data, long dataLength, IProgressListener progressListener)
	{
		return getDefaultServer().uploadBinary(fileExtension, fileMimeType, data, dataLength, progressListener);
	}

	@Override
	public Map<String, String> getDynamicComboValues(String serviceName, Map<String, String> inputValues)
	{
		if (LOG_ENABLED)
			Services.Log.info(LOG_TAG, String.format("getDynamicComboValues(%s, %s)", serviceName, inputValues));

		return getDefaultServer().getDynamicComboValues(serviceName, inputValues);
	}

	@Override
	public List<String> getSuggestions(String serviceName, Map<String, String> input)
	{
		return getDefaultServer().getSuggestions(serviceName, input);
	}

	@Override
	public MappedValue getMappedValue(String serviceName, Map<String, String> input)
	{
		return getDefaultServer().getMappedValue(serviceName, input);
	}
}
