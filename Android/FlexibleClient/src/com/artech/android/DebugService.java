package com.artech.android;

import java.io.IOException;
import java.util.Locale;
import java.util.Random;

import org.apache.http.client.methods.HttpRequestBase;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

import com.artech.base.services.Services;
import com.artech.base.utils.MathUtils;

public class DebugService
{
	private static Locale sLocale = null;
	private static double sNetworkDelayProbability = 0;
	private static int sNetworkDelayTime = 0;
	private static double sNetworkErrorProbability = 0;
	private static boolean sNetworkOffline = false;

	private static Random sRandom = new Random();

	/**
	 * Forces the application to use a specific locale instead of the system one.
	 * @param locale
	 */
	public static void setLocale(Locale locale)
	{
		sLocale = locale;
	}

	/**
	 * Forces a delay in random requests to server.
	 * @param probability Probability that the delay will occur (0 to 1.0).
	 * @param delay Delay in milliseconds.
	 */
	public static void setNetworkDelay(double probability, int delay)
	{
		sNetworkDelayProbability = MathUtils.constrain(probability, 0.0, 1.0);
		sNetworkDelayTime = Math.max(0, delay);
	}

	/**
	 * Forces random requests to the server to fail.
	 * @param probability Probability that the error will occur (0 to 1.0).
	 */
	public static void setNetworkError(double probability)
	{
		sNetworkErrorProbability = probability;
	}

	/**
	 * Forces the application to think there is no internet connection.
	 */
	public static void setNetworkOffline(boolean offline)
	{
		sNetworkOffline = offline;
	}

	public static void onCreate(Application application)
	{
		// setContextLocale(application);
		// setContextLocale(application.getBaseContext());
	}

	public static void onConfigurationChanged(Configuration newConfig)
	{
		if (sLocale != null)
		{
			Locale.setDefault(sLocale);
			newConfig.locale = sLocale;
		}
	}

	public static void onCreate(Activity activity)
	{
		// setContextLocale(activity);
		setContextLocale(activity.getBaseContext());
	}

	public static void onResume(Activity activity)
	{
		// setContextLocale(activity);
		setContextLocale(activity.getBaseContext());
	}

	private static void setContextLocale(Context context)
	{
		if (sLocale != null)
		{
			Locale.setDefault(sLocale);
			Configuration config = new Configuration();
			config.locale = sLocale;
			context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
		}
	}

	public static void onHttpRequest(HttpRequestBase request) throws IOException
	{
		if (sNetworkDelayProbability > 0 && sNetworkDelayTime > 0)
		{
			try
			{
				// Chance to add a delay to the request.
				double roll = sRandom.nextDouble();
				if (roll < sNetworkDelayProbability)
					Thread.sleep(sNetworkDelayTime);
			}
			catch (InterruptedException e) { }
		}

		if (sNetworkErrorProbability > 0)
		{
			double roll = sRandom.nextDouble();
			if (roll < sNetworkErrorProbability)
				throw new IOException("DebugService: Random exception forced on network request."); //$NON-NLS-1$
		}
	}

	public static boolean isNetworkOffline()
	{
		if (sNetworkOffline)
			Services.Log.debug("DebugService: Reporting network as offline"); //$NON-NLS-1$

		return sNetworkOffline;
	}
}
