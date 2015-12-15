package com.artech.common;

import java.net.HttpURLConnection;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Pair;

import com.artech.R;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

class ServiceErrorParser
{
	public static Pair<Integer, String> parse(HttpRequestBase request, HttpResponse response)
	{
		// We handle only security errors here, return a generic response for any others.
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpURLConnection.HTTP_UNAUTHORIZED && statusCode != HttpURLConnection.HTTP_FORBIDDEN)
			return new Pair<>(DataRequest.ERROR_SERVER, response.getStatusLine().getReasonPhrase());

		Pair<Integer, String> result = null;
		try
		{
			// Look up in response body first.
			// May be empty, or fail with "chunked stream...", in some cases.
			HttpEntity responseEntity = response.getEntity();
			String content = EntityUtils.toString(responseEntity, HTTP.UTF_8);
			result = parseBody(content);
		}
		catch (Exception ex)
		{
			Services.Log.warning(ex.getMessage(), ex);
		}

		// Look in response header.
		if (result == null)
			result = parseHeader(response);

		// No error data at all? Assume generic authentication error on 401, generic authorization error on 403.
		if (result == null)
			result = buildGenericError(request, response);

		return result;
	}

	private static Pair<Integer, String> parseBody(String responseBody)
	{
		if (!Services.Strings.hasValue(responseBody))
			return null; // Nothing.

		// Get the error node (may be alone or in a collection).
		try
		{
			// Try to read as an object.
			JSONObject jsonResponse = new JSONObject(responseBody);
			JSONObject jsonError = jsonResponse.getJSONObject("error");
			return parse(jsonError);
		}
		catch (JSONException notAnObject)
		{
			try
			{
				// Try to read as an array.
				JSONArray jsonArray = new JSONArray(responseBody);
				if (jsonArray.length() > 0)
				{
					JSONObject jsonObject = (JSONObject)jsonArray.get(0);
					return parse(jsonObject);
				}
				else
					return null; // Empty array, no errors.
			}
			catch (JSONException notAnArray)
			{
				notAnArray.printStackTrace();
				return null;
			}
		}
	}

	private static Pair<Integer, String> parseHeader(HttpResponse response)
	{
		final String ERROR_DESCRIPTION_START = "error_description=\""; //$NON-NLS-1$

		//WWW-Authenticate	OAuth realm="apps.genexus.com", error="invalid_token", error_description="The access token is no longer valid."
		Header[] headers = response.getHeaders("WWW-Authenticate"); //$NON-NLS-1$
		String errorMessage = Strings.EMPTY;
		if (headers != null)
		{
			for (Header header : headers)
			{
				String value = header.getValue();
				errorMessage += value;
			}

			//extract error_description from message
			int start = errorMessage.indexOf(ERROR_DESCRIPTION_START);
			int end = errorMessage.lastIndexOf("\""); //$NON-NLS-1$
			if (start > 0 && end > (start + ERROR_DESCRIPTION_START.length()))
				errorMessage = errorMessage.substring(start + ERROR_DESCRIPTION_START.length(), end) + Strings.SPACE;
		}

		// Assume authentication error if any error occurred.
		if (errorMessage.length() != 0)
			return new Pair<>(DataRequest.ERROR_SECURITY_AUTHENTICATION, errorMessage);

		return null;
	}

	// Codes for errors on body.
	private static final int GAM_ERROR_PASSWORD_MUST_BE_CHANGED = 23;
	private static final int GAM_ERROR_PASSWORD_EXPIRED = 24;
	private static final int GAM_ERROR_TOKEN_EXPIRED = 103;
	private static final int GAM_ERROR_NOT_AUTHORIZED = 139;

	private static Pair<Integer, String> parse(JSONObject json)
	{
		String serverCode = json.optString("code");
		if (!Services.Strings.hasValue(serverCode))
			serverCode = json.optString("Code");

		String message = json.optString("message");
		if (!Services.Strings.hasValue(message))
			message = json.optString("Message");

		// Convert GAM code to internal code.
		int code = serverCodeToInternalCode(serverCode);
		return new Pair<>(code, message);
	}

	private static int serverCodeToInternalCode(String serverCode)
	{
		try
		{
			switch (Integer.parseInt(serverCode))
			{
				case GAM_ERROR_TOKEN_EXPIRED :
					return DataRequest.ERROR_SECURITY_AUTHENTICATION;

				case GAM_ERROR_NOT_AUTHORIZED :
					return DataRequest.ERROR_SECURITY_AUTHORIZATION;

				case GAM_ERROR_PASSWORD_EXPIRED :
				case GAM_ERROR_PASSWORD_MUST_BE_CHANGED :
					return DataRequest.ERROR_SECURITY_CHANGE_PASSWORD;
			}
		}
		catch (NumberFormatException ex)
		{
			Services.Log.warning("Non-numeric server code? This should never happen.");
		}

		Services.Log.warning(String.format("Unknown error code: %s", serverCode));
		return DataRequest.ERROR_SECURITY_AUTHENTICATION;
	}

	private static Pair<Integer, String> buildGenericError(HttpRequestBase request, HttpResponse response)
	{
		if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_FORBIDDEN)
		{
			// Generic AUTHORIZATION error.
			String message;
			String uri = (request != null ? request.getURI().toString() : null);
			if (request != null)
				message = String.format(Services.Strings.getResource(R.string.GXM_InvalidHttpMode), request.getMethod(), uri);
			else
				message = String.format(Services.Strings.getResource(R.string.GXM_InvalidHttpMode), "", "");

			return new Pair<>(DataRequest.ERROR_SECURITY_AUTHORIZATION, message);
		}
		else
		{
			// Generic AUTHENTICATION error.
			return new Pair<>(DataRequest.ERROR_SECURITY_AUTHENTICATION, response.getStatusLine().getReasonPhrase());
		}
	}
}
