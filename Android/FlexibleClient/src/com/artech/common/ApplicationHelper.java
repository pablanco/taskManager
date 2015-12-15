package com.artech.common;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import com.artech.application.MyApplication;
import com.artech.base.services.Services;

public class ApplicationHelper {
	private static String TAG = "ApplicationHelper"; //$NON-NLS-1$

	/**
	 * Sends a HTTP request to retrieve the appid.json containing the name of the Knowledge Base and then compares it to the one stored in the app.
	 * @return true if all the steps succeed, false otherwise.
	 */
	public static boolean checkApplicationUri(String appUri) {
		InputStream inputStream = null;
		String result = null;

		try {
			DefaultHttpClient httpClient = new DefaultHttpClient(new BasicHttpParams());

			String url = Uri.withAppendedPath(Uri.parse(appUri), "gxmetadata/appid.json").toString(); //$NON-NLS-1$

			HttpGet httpGet = new HttpGet(url);
			httpGet.setHeader("Content-type", "application/json"); //$NON-NLS-1$ $NON-NLS-2$

			HttpResponse response = httpClient.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();

			if (statusLine.getStatusCode() != 200) {
				return false;
			}

			HttpEntity entity = response.getEntity();
			inputStream = entity.getContent();

			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, HTTP.UTF_8), 8);
			StringBuilder stringBuilder = new StringBuilder();

			String line;
			while ((line = reader.readLine()) != null)
				stringBuilder.append(line).append("\n");

			result = stringBuilder.toString();
		} catch (Exception e) {
			Services.Log.Error(TAG, "Error while downloading appid.json from server."); //$NON-NLS-1$
			return false;
		} finally {
			IOUtils.closeQuietly(inputStream);
		}

		String kbName;

		try {
			JSONObject jsonObject = new JSONObject(result);
			kbName = jsonObject.getString("id"); //$NON-NLS-1$
		} catch (JSONException e) {
			Services.Log.Error(TAG, "Error while parsing the appid.json."); //$NON-NLS-1$
			return false;
		}

		if (kbName == null || !kbName.equalsIgnoreCase(MyApplication.getApp().getName())) {
			Services.Log.Error(TAG, "AppId names don't match."); //$NON-NLS-1$
			return false;
		}

		return true;
	}
}
