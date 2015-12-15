package com.artech.common;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Pair;

import com.artech.R;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class ServiceDataResult
{
	private JSONArray mData;
	private int mStatusCode;
	private Date mLastModified;

	private int mErrorType;
	private String mErrorMessage;

	private static final String HEADER_LAST_MODIFIED = "Last-Modified"; //$NON-NLS-1$

	private ServiceDataResult()
	{
		mData = new JSONArray();
		mLastModified = new Date(0);
		mErrorType = DataRequest.ERROR_NONE;
	}

	ServiceDataResult(HttpGet get, HttpResponse response, boolean isCollection)
	{
		this();
		mStatusCode = response.getStatusLine().getStatusCode();

		if (mStatusCode == HttpURLConnection.HTTP_NOT_FOUND || mStatusCode >= HttpURLConnection.HTTP_INTERNAL_ERROR)
		{
			// Don't even read JSON data in this case.
			String httpError = String.valueOf(mStatusCode) + " - " + response.getStatusLine().getReasonPhrase();
			setAppServerError(DataRequest.ERROR_SERVER, httpError);
			get.abort(); // to return connection to pool.
			return;
		}

		if (mStatusCode == HttpURLConnection.HTTP_UNAUTHORIZED || mStatusCode == HttpURLConnection.HTTP_FORBIDDEN)
		{
			// Not authenticated or authorized, see body for details.
			Pair<Integer, String> error = ServiceErrorParser.parse(get, response);
			setError(error.first, error.second);
			return;
		}

		// On 304 no data is returned.
		if (mStatusCode != HttpURLConnection.HTTP_NOT_MODIFIED)
		{
			// Read JSON data.
			readEntity(response, isCollection);
		}

		// Read last modified date, if present.
		Header[] lastModified = response.getHeaders(HEADER_LAST_MODIFIED);
		if (lastModified != null && lastModified.length != 0)
			mLastModified = StringUtil.dateFromHttpFormat(lastModified[0].getValue());
	}

	static ServiceDataResult error(int errorType, String errorMessage)
	{
		ServiceDataResult result = new ServiceDataResult();
		result.setError(errorType, errorMessage);
		return result;
	}

	static ServiceDataResult networkError(IOException exception)
	{
		return error(DataRequest.ERROR_NETWORK, Services.HttpService.getNetworkErrorMessage(exception));
	}

	// Status
	public boolean isOk() { return (mErrorType == DataRequest.ERROR_NONE); }
	public boolean isUpToDate() { return mStatusCode == HttpURLConnection.HTTP_NOT_MODIFIED; }
	public int getErrorType() { return mErrorType; }
	public String getErrorMessage() { return mErrorMessage; }

	// Results
	public JSONArray getData() { return mData; }
	public Date getLastModified() { return mLastModified; }

	public Iterable<JSONObject> getDataObjects()
	{
		List<JSONObject> objects = new ArrayList<JSONObject>();
		int count = mData.length();
		for (int i = 0; i < count; i++)
		{
			try
			{
				objects.add(mData.getJSONObject(i));
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		return objects;
	}

	private void setAppServerError(int errorType, String errorDetail)
	{
		String errorMessage = Services.Strings.getResource(R.string.GXM_ApplicationServerError, errorDetail);
		setError(errorType, errorMessage);
	}

	private void setError(int errorType, String errorMessage)
	{
		mErrorType = errorType;
		mErrorMessage = errorMessage;
	}

	private boolean readEntity(HttpResponse response, boolean isCollection)
	{
		try
		{
			// Read result from HTTP.
			HttpEntity entity = response.getEntity();
			String result = EntityUtils.toString(entity, HTTP.UTF_8);

			// Parse JSON result.
 			return readJson(result, isCollection);
		}
		catch (Exception ex)
		{
			Services.Log.Error("readEntity", ex); //$NON-NLS-1$
			setAppServerError(DataRequest.ERROR_DATA, ex.getClass().getName());
			return false;
		}
	}

	private boolean readJson(String str, boolean isCollection)
	{
		try
		{
			// Try to read as an array.
			mData = new JSONArray(str);
			return true;
		}
		catch (JSONException notAnArray)
		{
			// If that fails, read as an object (may also fail if it's not valid JSON at all).
			try
			{
				JSONObject jsonObject = new JSONObject(str);
				if (isCollection)
				{
					if (jsonObject.names() != null && jsonObject.names().length() > 0)
					{
						String elementName = jsonObject.names().getString(0);

						JSONArray jsonArray = jsonObject.optJSONArray(elementName);
						if (jsonArray!=null)
						{
							mData = jsonArray;
							return true;
						}

						// Try to read as list made of single element.
						JSONObject jsonObjArray = jsonObject.optJSONObject(elementName);
						if (jsonObjArray != null)
						{
							mData.put(jsonObjArray);
							return true;
						}
					}
				}

				mData.put(jsonObject);
				return true;
			}
			catch (JSONException notJson)
			{
				Services.Log.Error("readJson", notJson); //$NON-NLS-1$
				setError(DataRequest.ERROR_DATA, notJson.getMessage());
				return false;
			}
		}
	}

	static String parseRedirectOnHeader(HttpResponse response)
	{
		Header[] headers = response.getHeaders("Location"); //$NON-NLS-1$
		String newUrl = Strings.EMPTY;
		if (headers != null && headers.length != 0)
			newUrl = headers[headers.length - 1].getValue();

		return newUrl;
	}
}
