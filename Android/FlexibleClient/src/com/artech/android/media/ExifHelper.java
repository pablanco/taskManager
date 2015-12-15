package com.artech.android.media;

import java.io.File;
import java.io.IOException;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.SparseIntArray;

import com.artech.application.MyApplication;
import com.artech.base.services.Services;
import com.artech.base.utils.ListUtils;
import com.artech.base.utils.Strings;

public class ExifHelper
{
	private final static List<String> EXIF_TAGS;
	private final static SparseIntArray DEGREES_TO_EXIF_ORIENTATION;

	static
	{
		EXIF_TAGS = initializeExifTags();
		DEGREES_TO_EXIF_ORIENTATION = initializeDegreesToExifOrientation();
	}

	@SuppressLint("InlinedApi")
	private static List<String> initializeExifTags()
	{
		// We want to preserve all EXIF tags EXCEPT ImageWidth and ImageLength.
		List<String> tags = ListUtils.listOf(
			ExifInterface.TAG_ORIENTATION, ExifInterface.TAG_DATETIME, ExifInterface.TAG_MAKE,
			ExifInterface.TAG_MODEL, ExifInterface.TAG_FLASH, ExifInterface.TAG_IMAGE_WIDTH,
			ExifInterface.TAG_IMAGE_LENGTH, ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LONGITUDE,
			ExifInterface.TAG_GPS_LATITUDE_REF, ExifInterface.TAG_GPS_LONGITUDE_REF, ExifInterface.TAG_GPS_ALTITUDE,
			ExifInterface.TAG_GPS_ALTITUDE_REF, ExifInterface.TAG_GPS_TIMESTAMP, ExifInterface.TAG_GPS_DATESTAMP,
			ExifInterface.TAG_WHITE_BALANCE, ExifInterface.TAG_FOCAL_LENGTH, ExifInterface.TAG_GPS_PROCESSING_METHOD);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			tags.addAll(ListUtils.listOf(ExifInterface.TAG_EXPOSURE_TIME, ExifInterface.TAG_APERTURE, ExifInterface.TAG_ISO));

		return tags;
	}

	private static SparseIntArray initializeDegreesToExifOrientation()
	{
		SparseIntArray map = new SparseIntArray();

		// From http://developer.android.com/reference/android/provider/MediaStore.Images.ImageColumns.html#ORIENTATION
		// The orientation for the image expressed as degrees. Only degrees 0, 90, 180, 270 will work.
		// We don't include a mapping for 0 -> ORIENTATION_NORMAL to avoid writing the (unnecessary) tag in that case.
		// map.put(0, ExifInterface.ORIENTATION_NORMAL);
		map.put(90, ExifInterface.ORIENTATION_ROTATE_90);
		map.put(180, ExifInterface.ORIENTATION_ROTATE_180);
		map.put(270, ExifInterface.ORIENTATION_ROTATE_270);

		return map;
	}

	public boolean copyExifInformation(Uri srcUri, File dstFile)
	{
		if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(srcUri.getScheme()))
		{
			// Copy EXIF information from the original file.
			return copyExifInformationFromFile(new File(srcUri.getPath()), dstFile);
		}
		else if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(srcUri.getScheme()))
		{
			// Copy EXIF information from the data stored in the Media Store (ORIENTATION column).
			return copyExifInformationFromMediaStore(srcUri, dstFile);
		}
		else
			return false;
	}

	private boolean copyExifInformationFromFile(File srcFile, File dstFile)
	{
		try
		{
			if (!srcFile.exists() || !srcFile.canRead())
				throw new IOException("Cannot read source file or it does not exist.");

			if (!dstFile.exists() || !dstFile.canWrite())
				throw new IOException("Cannot write destination file or it does not exist.");

			ExifInterface srcExif = new ExifInterface(srcFile.getAbsolutePath());
			ExifInterface dstExif = new ExifInterface(dstFile.getAbsolutePath());

			boolean somethingCopied = false;
			for (String tag : EXIF_TAGS)
			{
				String tagValue = srcExif.getAttribute(tag);
				if (Strings.hasValue(tagValue))
				{
					dstExif.setAttribute(tag, tagValue);
					somethingCopied = true;
				}
			}

			if (somethingCopied)
				dstExif.saveAttributes();

			return true;
		}
		catch (IOException e)
		{
			Services.Log.warning(String.format("Exception while copying EXIF information from %s to %s", srcFile.getAbsolutePath(), dstFile.getAbsolutePath()), e);
			return false;
		}
	}

	private boolean copyExifInformationFromMediaStore(Uri srcUri, File dstFile)
	{
		Context context = MyApplication.getAppContext();
		Cursor cursor = context.getContentResolver().query(srcUri, new String[] { MediaStore.Images.Media.ORIENTATION }, null, null, null);
		try
		{
			if (cursor.moveToFirst())
			{
			    int mediaStoreOrientation = cursor.getInt(0);
			    int exifOrientation = DEGREES_TO_EXIF_ORIENTATION.get(mediaStoreOrientation, -1);
			    if (exifOrientation != -1)
			    {
			    	try
			    	{
			    		ExifInterface dstExif = new ExifInterface(dstFile.getAbsolutePath());
			    		dstExif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(exifOrientation));
			    		dstExif.saveAttributes();
			    		return true;
			    	}
			    	catch (IOException e)
			    	{
						Services.Log.warning(String.format("Exception while saving EXIF orientation value (%s) to %s", exifOrientation, dstFile.getAbsolutePath()), e);
			    	}
			    }
			    else
			    	Services.Log.warning("Ignoring unknown value for media store ORIENTATION column: " + mediaStoreOrientation);
			}
		}
		finally
		{
			cursor.close();
		}

		return false;
	}
}
