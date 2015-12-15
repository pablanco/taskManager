package com.artech.android.media.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

public class CameraUtils {
	
	public static boolean supportsExtraOutput(String intentAction) {
		if (Build.MANUFACTURER.equalsIgnoreCase("samsung") &&
				(Build.MODEL.startsWith("GT-") || Build.MODEL.startsWith("SM-"))) {
			// Samsung *Galaxy Models* contain a Camera app that does not implement EXTRA_OUTPUT properly.
			// Either doesn't support it or have a different behavior than the specified (e.g. Copies the
			// media file to both the destination path in the uri and the default gallery path).
			return false;
		}
		
		if (MediaStore.ACTION_IMAGE_CAPTURE.equals(intentAction)) {
			// Nexus One and other devices must use EXTRA_OUTPUT due to a bug with the default mechanism.
			// http://thanksmister.com/2012/03/16/android_null_data_camera_intent/
			return true;
		} else if (MediaStore.ACTION_VIDEO_CAPTURE.equals(intentAction)) {
			// Some older devices like the Nexus One for ACTION_VIDEO_CAPTURE, don't support it. Use only on >= ICS.
			// Also, make sure to use EXTRA_OUTPUT due to a bug in Android 4.3 and later if not using it.
			// https://code.google.com/p/android/issues/detail?id=57996
			return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH);
		} else { // MediaStore.Audio.Media.RECORD_SOUND_ACTION
			return true;
		}
	}

	public static void copyUriToFile(Context context, Uri inputUri, File outputFile) {
		try {
			InputStream inputStream = context.getContentResolver().openInputStream(inputUri);
			OutputStream outputStream = new FileOutputStream(outputFile);
			IOUtils.copy(inputStream, outputStream);
		} catch (FileNotFoundException e) {
		} catch (IOException e) {}
	}
}
