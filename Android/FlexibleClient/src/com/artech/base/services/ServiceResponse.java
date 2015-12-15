package com.artech.base.services;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;

import com.artech.R;
import com.artech.base.serialization.INodeObject;
import com.artech.common.DataRequest;

public class ServiceResponse
{
	public int HttpCode = 0;
	public String Message = null;
	public InputStream Stream = null;
	public INodeObject Data = null;

	public int StatusCode = DataRequest.ERROR_NONE;
	public String ErrorMessage = null;
	public String WarningMessage = null;

	public ServiceResponse() { }

	public ServiceResponse(JSONException ex)
	{
		HttpCode = -1;
		ErrorMessage = Services.Strings.getResource(R.string.GXM_ApplicationServerError, JSONException.class.getName());
	}

	public ServiceResponse(IOException ex)
	{
		HttpCode = -1;
		ErrorMessage = Services.HttpService.getNetworkErrorMessage(ex);
	}

	public boolean getResponseOk()
	{
		return getResponseOk(HttpCode);
	}

	private static boolean getResponseOk(int code)
	{
		return (code >= 200 && code < 300);
	}

	public String get(String name)
	{
		return (Data != null ? Data.optString(name) : null);
	}
}