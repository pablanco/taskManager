package com.artech.common;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;

import com.artech.application.MyApplication;

public class StorageHelper
{
	private static final String CAMERA_DIRECTORY = ".camera";
	private static final String IMAGES_DIRECTORY = "images";
	/**
	 * Returns the external files directory if the external storage is mounted
	 * or the internal files directory otherwise.
	 */
	public static File getStorageDirectory()
	{
		return getStorageDirectory(true);
	}

	/**
	 * Returns the external files directory (normally /mnt/sdcard/Android/data/<package>/files)
	 * if the external storage is mounted and the allowExternal parameter is true,
	 * or the internal files directory otherwise.
	 */
	@SuppressLint("NewApi")
	public static File getStorageDirectory(boolean allowExternal)
	{
		File directory;
		Context context = MyApplication.getAppContext();

		if (allowExternal && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
		{
			// Use getExternalFilesDir().
			// In particular in Android 4.2, this will return different folders per user.
			directory = MyApplication.getAppContext().getExternalFilesDir(null);
		}
		else
		{
			// No external storage. Use private one.
			directory = context.getFilesDir();
		}

		// if null use getFilesDir, avoid crash for not getting external directory.
		if (directory==null)
			directory = context.getFilesDir();

        if (!directory.exists())
        	directory.mkdirs();

		return directory;
	}

	/**
	 * Returns the path of a directory with the specified name under the application's storage directory
	 * (external files directory if the external storage is mounted or the internal files directory otherwise).
	 */
	public static File getStorageDirectory(String name)
	{
		return getStorageDirectory(name, true);
	}

	/**
	 * Returns the path of a directory with the specified name under the application's storage directory.
	 */
	public static File getStorageDirectory(String name, boolean allowExternal)
	{
		File base = getStorageDirectory(allowExternal);
		File sub = new File(base, name);

		if (!sub.exists())
			sub.mkdirs();

		return sub;
	}

	public static File getCacheStorageDirectory(boolean allowExternal)
	{
		File directory;
		Context context = MyApplication.getAppContext();
		if (allowExternal)
		{
			directory = context.getExternalCacheDir();
			if (directory==null)
			{
				directory = context.getCacheDir();
			}
		}
		else
			directory = context.getCacheDir();

		return directory;

	}

	public static File getNewCameraFile(String extension) {
		File cameraDirectory = getStorageDirectory(CAMERA_DIRECTORY);
    	String timestamp = new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss-SSS", Locale.US).format(new Date());
		return new File(cameraDirectory, timestamp + "." + extension);
	}

	public static File getImagesDirectory() {
		return StorageHelper.getStorageDirectory(IMAGES_DIRECTORY);
	}

	@SuppressLint("SdCardPath")
	public static boolean isLocalFile(String uri)
	{
		// Approximate.
		return (uri != null && (uri.startsWith("file://") || uri.startsWith("/mnt/") || uri.startsWith("/sdcard/")  || uri.startsWith("/data/") || uri.startsWith("/storage/")));
	}
}
