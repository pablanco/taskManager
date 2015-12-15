package com.artech.common;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.artech.base.services.Services;

public class NetworkLogger
{
	public enum Level { NONE, BASIC, DETAILED }

	private static Level sLevel = Level.BASIC;
	private static final String LOG_TAG = "Genexus-HTTP"; //$NON-NLS-1$

	/**
	 * Configures the logging level for network calls.
	 * @param level
	 */
	public static void setLevel(Level level)
	{
		sLevel = level;
	}

	public static Level getLevel()
	{
		return sLevel;
	}

	static void logRequest(HttpUriRequest request)
	{
		if (sLevel == Level.NONE)
			return;

		String uri = request.getURI().toString();
		String method = request.getMethod();

		if (sLevel == Level.BASIC)
		{
			// Just log request URL.
			log(String.format("Request (%s) to %s ", method, uri), false); //$NON-NLS-1$
			return;
		}

		try
		{
			JSONObject jsonRequest = new JSONObject();
			jsonRequest.put("url", uri); //$NON-NLS-1$
			jsonRequest.put("method", method); //$NON-NLS-1$

			// Headers
			addHeaders(jsonRequest, request.getAllHeaders());

			// Body
			if (request instanceof HttpEntityEnclosingRequest)
			{
				try
				{
					HttpEntity entity = ((HttpEntityEnclosingRequest)request).getEntity();
					if (entity instanceof StringEntity)
					{
						// Since entities of other classes (in particular InputStreamEntity) might not be repeatable-readable,
						// calling toString() may "break" them. For safety, only do this for StringEntity.
						jsonRequest.put("body", EntityUtils.toString(entity, HTTP.UTF_8)); //$NON-NLS-1$
					}
					else
						jsonRequest.put("body", (entity != null ? String.format("<STREAM::%s>", entity.getClass().getSimpleName()) : "<NULL>"));
				}
				catch (ParseException e) { }
				catch (IOException e) { }
			}

			log("request", jsonRequest, false); //$NON-NLS-1$
		}
		catch (JSONException e) { }
	}

	static void logResponse(HttpUriRequest request, GxHttpResponse response)
	{
		if (sLevel == Level.NONE)
			return;

		String uri = request.getURI().toString();
		int statusCode = response.getStatusLine().getStatusCode();

		if (sLevel == Level.BASIC)
		{
			// Just log request URL.
			log(String.format("Response (%s) from %s", statusCode, uri), isHttpError(statusCode)); //$NON-NLS-1$
			return;
		}

		try
		{
			JSONObject jsonResponse = new JSONObject();
			jsonResponse.put("url", uri); //$NON-NLS-1$
			jsonResponse.put("statusCode", statusCode); //$NON-NLS-1$

			// Headers
			addHeaders(jsonResponse, response.getAllHeaders());

			// Body
			try
			{
				HttpEntity entity = response.getEntity();
				jsonResponse.put("bytes", entity.getContentLength()); //$NON-NLS-1$
				jsonResponse.put("data", EntityUtils.toString(entity, HTTP.UTF_8)); //$NON-NLS-1$
			}
			catch (ParseException e) { }
			catch (IOException e) { }

			log("response", jsonResponse, isHttpError(statusCode)); //$NON-NLS-1$
		}
		catch (JSONException e) { }
	}

	static void logException(HttpUriRequest request, IOException exception)
	{
		if (sLevel == Level.NONE)
			return;

		String uri = request.getURI().toString();
		String exceptionClass = exception.getClass().getName();

		if (sLevel == Level.BASIC)
		{
			// Just log request URL.
			String logMessage = String.format("Error (%s) from %s", exceptionClass, uri);
			Services.Log.Error(LOG_TAG, logMessage, exception);
			return;
		}

		try
		{
			JSONObject jsonException = new JSONObject();
			jsonException.put("url", request.getURI().toString()); //$NON-NLS-1$

			// Exception detail.
			JSONObject jsonError = new JSONObject();
			jsonError.put("class", exceptionClass); //$NON-NLS-1$
			jsonError.put("message", exception.getMessage()); //$NON-NLS-1$
			jsonException.put("error", jsonError); //$NON-NLS-1$

			jsonException.put("localizedDescription", exception.getLocalizedMessage()); //$NON-NLS-1$

			log("requestFail", jsonException, true); //$NON-NLS-1$
		}
		catch (JSONException e) { }
	}

	private static void log(String name, JSONObject entry, boolean isError)
	{
		try
		{
			JSONObject enclosing = new JSONObject();
			enclosing.put(name, entry);
			log(enclosing.toString(), isError);
		}
		catch (JSONException e) { }
	}

	private static void log(String text, boolean isError)
	{
		if (isError)
			Services.Log.Error(LOG_TAG, text);
		else
			Services.Log.debug(LOG_TAG, text);
	}

	private static void addHeaders(JSONObject entry, Header[] headers) throws JSONException
	{
		// Headers
		JSONObject jsonHeaders = new JSONObject();
		for (Header header : headers)
			jsonHeaders.put(header.getName(), header.getValue());

		entry.put("headers", jsonHeaders); //$NON-NLS-1$
	}

	private static boolean isHttpError(int statusCode)
	{
		// Take anything as "unexpected" except for the following "normal" statuses.
		return !(statusCode == HttpURLConnection.HTTP_OK || statusCode == HttpURLConnection.HTTP_CREATED || statusCode == HttpURLConnection.HTTP_NOT_MODIFIED || statusCode == HttpURLConnection.HTTP_SEE_OTHER);

	}
}
