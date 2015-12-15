package com.artech.android.api;

import java.net.HttpURLConnection;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.support.annotation.NonNull;

import com.artech.base.services.Services;
import com.artech.common.DataRequest;
import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;

/**
 * This class allow access to HTTP services from Device.
 * @author CMurialdo
 *
 */
public class SDHttpAPI extends ExternalApi
{
	private int mErrorType;
	private String mErrorMessage;

	@Override
	public @NonNull ExternalApiResult execute(String method, List<Object> parameters)
	{
		List<String> parameterValues = toString(parameters);
		final HttpClient client = getThreadSafeClient();
		setTimeout(0, client);
		HttpRequestBase request = null;

		if (parameterValues.size() > 0 )
		{
			String uri = parameterValues.get(0);

			try
			{
				if (method.equalsIgnoreCase("get"))
					request = new HttpGet(uri);
				else if (method.equalsIgnoreCase("head"))
					request = new HttpHead(uri);
				else
					request = new HttpPost(uri);

				if (request instanceof HttpEntityEnclosingRequest && parameterValues.size() > 1)
				{
					String content = parameterValues.get(1);
					StringEntity requestEntity = new StringEntity(content, HTTP.UTF_8);
					requestEntity.setContentType("application/x-www-form-urlencoded");
					((HttpEntityEnclosingRequest)request).setEntity(requestEntity);
				}
				HttpResponse response = client.execute(request);

				int mStatusCode = response.getStatusLine().getStatusCode();
				if (mStatusCode != HttpURLConnection.HTTP_OK)
				{
					setError(mStatusCode, response.getStatusLine().getReasonPhrase());
					request.abort(); // to return connection to pool.
					return ExternalApiResult.success(getError());
				}
				else
				{
					return ExternalApiResult.success(readEntity(response));
				}
			}
			catch (Exception ex)
			{
				setError(-1, ex.getMessage());
				if (request!=null) request.abort();
				return ExternalApiResult.success(getError());
			}
		}
		else
		{
			setError(-1, "Invalid uri");
			return ExternalApiResult.success(getError());
		}
	}
	private String getError()
	{
		JSONObject jsonProperty = new JSONObject();
		try
		{
			jsonProperty.put("code", String.valueOf(getErrorType())); //$NON-NLS-1$
			jsonProperty.put("message", getErrorMessage()); //$NON-NLS-1$
		}
		catch (JSONException e)
		{
			android.util.Log.v("GeneXusApplication", "SDHttpAPI errorToJson:"+ e.getMessage());
		}
		return jsonProperty.toString();
	}
	private int getErrorType() { return mErrorType; }
	private String getErrorMessage() { return mErrorMessage; }

	private static DefaultHttpClient httpClient = null;

	private static DefaultHttpClient getThreadSafeClient()
	{
		if (httpClient == null)
		{
			DefaultHttpClient client = new DefaultHttpClient();
			ClientConnectionManager mgr = client.getConnectionManager();
			HttpParams params = client.getParams();

			httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(params, mgr.getSchemeRegistry()), params);
		}

		return httpClient;
	}

	private static void setTimeout(int timeout, final HttpClient client)
	{
		if (timeout>=0)
		{
			HttpParams httpParameters =  client.getParams();
			// Set the timeout in milliseconds until a connection is established.
			int timeoutConnection = timeout;
			HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
			// Set the default socket timeout (SO_TIMEOUT)
			// in milliseconds which is the timeout for waiting for data.
			int timeoutSocket = timeout;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
		}
	}

	private void setError(int errorType, String errorMessage)
	{
		android.util.Log.v("GeneXusApplication", "SDHttpAPI error:"+ errorType + " " + errorMessage);
		mErrorType = errorType;
		mErrorMessage = errorMessage;
	}

	private String readEntity(HttpResponse response)
	{
		try
		{
			// Read result from HTTP.
			HttpEntity entity = response.getEntity();
			if (entity!=null)
				return EntityUtils.toString(entity, HTTP.UTF_8);
			else
				return "";
		}
		catch (Exception ex)
		{
			Services.Log.Error("readEntity", ex); //$NON-NLS-1$
			setError(DataRequest.ERROR_DATA, ex.getMessage());
			return null;
		}
	}
}
