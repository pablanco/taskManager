package com.artech.android.api;

import android.content.Context;
import android.net.ConnectivityManager;
import android.support.v4.net.ConnectivityManagerCompat;

import com.artech.application.MyApplication;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

/**
 * This class allow access to device information.
 * @author GMilano
 *
 */
public class Network
{
	public static final String OSName = "Android"; //$NON-NLS-1$

	//Properties

	/***
	 * Return a value that identify the server uri.
	 * @return
	 */
	public static String applicationServerUrl()
	{
		return MyApplication.getApp().getAPIUri();
	}


	//Methods

	/***
	 * Returns if app server url is available
	 * @return
	 */
	public static boolean isServerAvailable()
	{
		return isServerAvailable(null);
	}

	/***
	 * Returns if url is available
	 * @return
	 */
	public static boolean isServerAvailable(String url)
	{
		if (!Strings.hasValue(url))
			url = MyApplication.getApp().UriMaker.getRootUri();

		return Services.HttpService.isReachable(url);
	}

	/***
	 * Returns type of connection
	 * @return
	 */
	public static int type()
	{
		return Services.HttpService.connectionType();
	}

	/***
	 * Returns type of connection to url
	 * @return
	 */
	public static int type(String url)
	{
		return Services.HttpService.connectionType();
	}

	/***
	 * Returns if traffic cost
	 * @return
	 */
	public static boolean trafficBasedCost()
	{
		ConnectivityManager connManager = (ConnectivityManager)MyApplication.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		return ConnectivityManagerCompat.isActiveNetworkMetered(connManager);
	}

	/***
	 * Returns if url traffic cost
	 * @return
	 */
	public static boolean trafficBasedCost(String url)
	{
		// We have no distinction for URL here.
		return trafficBasedCost();
	}
}
