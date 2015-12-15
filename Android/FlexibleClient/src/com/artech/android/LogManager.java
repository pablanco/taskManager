package com.artech.android;

import com.artech.base.services.ILog;

public class LogManager implements ILog
{
	public final static String APPLICATION_TAG = "GeneXusApplication";  //$NON-NLS-1$

	@Override
	public void Error(String tag, String msg)
	{
		android.util.Log.e(tag, msg);
	}

	@Override
	public void Error(String tag, String message, Throwable ex)
	{
		android.util.Log.e(tag, message, ex);
	}

	@Override
	public void Error(String message)
	{
		Error(APPLICATION_TAG, message);
	}

	@Override
	public void Error(String message, Throwable ex)
	{
		Error(APPLICATION_TAG, message, ex);
	}

	@Override
	public void error(Throwable ex)
	{
		Error("Logged exception", ex);
	}

	@Override
	public void warning(String tag, String msg)
	{
		android.util.Log.w(tag, msg);
	}

	@Override
	public void warning(String tag, String message, Throwable ex)
	{
		android.util.Log.w(tag, message, ex);
	}

	@Override
	public void warning(String message)
	{
		warning(APPLICATION_TAG, message);
	}

	@Override
	public void warning(String message, Throwable ex)
	{
		warning(APPLICATION_TAG, message, ex);
	}

	@Override
	public void info(String tag, String msg) {
		android.util.Log.i(tag, msg);
	}

	@Override
	public void info(String message)
	{
		info(APPLICATION_TAG, message);
	}

	@Override
	public void debug(String tag, String message)
	{
		android.util.Log.d(tag, message);
	}

	@Override
	public void debug(String tag, String message, Throwable ex)
	{
		android.util.Log.d(tag, message, ex);
	}

	@Override
	public void debug(String message)
	{
		debug(APPLICATION_TAG, message);
	}

	@Override
	public void debug(String message, Throwable ex)
	{
		debug(APPLICATION_TAG, message, ex);
	}
}
