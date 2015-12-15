package com.artech.android.audio;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.ComponentName;
import android.media.AudioManager;
import android.util.Log;

/**
 * Class that assists with handling new media button APIs available in API level 8.
 */
class MediaButtonHelper
{
	// Backwards compatibility code (methods available as of API Level 8)
	private static final String TAG = "MediaButtonHelper";

	static
	{
		initializeStaticCompatMethods();
	}

	private static Method sMethodRegisterMediaButtonEventReceiver;
	private static Method sMethodUnregisterMediaButtonEventReceiver;

	private static void initializeStaticCompatMethods()
	{
		try
		{
			sMethodRegisterMediaButtonEventReceiver = AudioManager.class.getMethod("registerMediaButtonEventReceiver", ComponentName.class);
			sMethodUnregisterMediaButtonEventReceiver = AudioManager.class.getMethod("unregisterMediaButtonEventReceiver", ComponentName.class);
		}
		catch (NoSuchMethodException e)
		{
			// Silently fail when running on an OS before API level 8.
		}
	}

	public static void registerMediaButtonEventReceiverCompat(AudioManager audioManager, ComponentName receiver)
	{
		if (sMethodRegisterMediaButtonEventReceiver == null)
			return;

		try
		{
			sMethodRegisterMediaButtonEventReceiver.invoke(audioManager, receiver);
		}
		catch (InvocationTargetException e)
		{
			// Unpack original exception when possible
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException)
				throw (RuntimeException) cause;
			else if (cause instanceof Error)
				throw (Error) cause;
			else
				throw new RuntimeException(e); // Unexpected checked exception; wrap and re-throw
		}
		catch (IllegalAccessException e)
		{
			Log.e(TAG, "IllegalAccessException invoking registerMediaButtonEventReceiver.");
			e.printStackTrace();
		}
	}

	public static void unregisterMediaButtonEventReceiverCompat(AudioManager audioManager, ComponentName receiver)
	{
		if (sMethodUnregisterMediaButtonEventReceiver == null)
			return;

		try
		{
			sMethodUnregisterMediaButtonEventReceiver.invoke(audioManager, receiver);
		}
		catch (InvocationTargetException e)
		{
			// Unpack original exception when possible
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException)
				throw (RuntimeException) cause;
			else if (cause instanceof Error)
				throw (Error) cause;
			else
				throw new RuntimeException(e); // Unexpected checked exception; wrap and re-throw
		}
		catch (IllegalAccessException e)
		{
			Log.e(TAG, "IllegalAccessException invoking unregisterMediaButtonEventReceiver.");
			e.printStackTrace();
		}
	}
}