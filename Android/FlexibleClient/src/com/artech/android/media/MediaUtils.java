package com.artech.android.media;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import com.artech.application.MyApplication;

public class MediaUtils {
	/**
	 * Useful to check onActivityResult for MediaPicker request codes.
	 */
	public static boolean isPickMediaRequest(int requestCode) {
		return requestCode == MediaHelper.PICK_IMAGE || requestCode == MediaHelper.PICK_VIDEO || requestCode == MediaHelper.PICK_AUDIO;
	}

	/**
	 * Useful to check onActivityResult for CameraHelper request codes.
	 */
	public static boolean isTakeMediaRequest(int requestCode) {
		return requestCode == MediaHelper.TAKE_PICTURE || requestCode == MediaHelper.CAPTURE_VIDEO || requestCode == MediaHelper.CAPTURE_AUDIO;
	}
	
	/**
	 * Creates a local file in the DCIM directory storing the media content and scans it so it pops up in the Gallery.
	 * 
	 * @param uri the media's uri. Supports content:// or file:// schemes.
	 * @param fileName name for the file that will be created.
	 * @return the file path to the created media file.
	 */
	public static String addToGallery(Uri uri, String fileName) {
		File externalDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
		externalDir.mkdirs();
		File outputFile = new File(externalDir, fileName);
		
		try {
			if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
				InputStream inputStream = MyApplication.getAppContext().getContentResolver().openInputStream(uri);
				FileUtils.copyInputStreamToFile(inputStream, outputFile);
			} else { // Assume it's a local file (with or without the file:// scheme).
				File inputFile = new File(uri.getPath());
				FileUtils.copyFile(inputFile, outputFile);
			}
		} catch (IOException e) {
			return null;
		}
		
		MediaScannerConnection.scanFile(MyApplication.getAppContext(), new String [] { outputFile.getAbsolutePath() }, null, null);
		
		return outputFile.getAbsolutePath();
	}
	
	/**
	 * Attempts to retrieve the complete filename behind a content or http(s) URI.
	 * Otherwise creates a filename with current time as timestamp.
	 * @param uri a URI with content://, http:// or https:// scheme.
	 */
	public static String getFileName(Uri uri) {
		String fileName = null;
		String extension = null;
		
		if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
			Cursor cursor = MyApplication.getAppContext().getContentResolver().query(uri, null, null , null, null);
			if (cursor.getCount() == 1 && cursor.moveToFirst()) {
				int titleIndex = cursor.getColumnIndex(MediaStore.MediaColumns.TITLE);
				if (titleIndex >= 0) {
					fileName = cursor.getString(titleIndex);
				}
				int mimeTypeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE);
				if (mimeTypeIndex >= 0) {
					String mimeType = cursor.getString(mimeTypeIndex);
					extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
				}
			}
			cursor.close();
		} else if ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) {
			fileName = FilenameUtils.getBaseName(uri.toString());
			extension = FilenameUtils.getExtension(uri.toString());
		}
		
		// Put current timestamp as filename if the filename can not be obtained.
		if (fileName == null) {
			fileName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
		}
		
		return fileName + '.' + extension;
	}
	
	/**
	 * Copies the content of an Uri to a local File. 
	 * @param uri an Uri with scheme content://, http:// or https://.
	 * @param outputFile output location for the file.
	 * @param streamCopyListener notifies when the copy is done.
	 */
	public static void copyUriToFileAsync(final Uri uri, final File outputFile, final CopyUriToFileListener streamCopyListener) {
		new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground(Void... params) {
				InputStream inputStream = null;
				
				try {
					if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
						inputStream = MyApplication.getAppContext().getContentResolver().openInputStream(uri);
					} else if ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) {
						inputStream = new URL(uri.toString()).openStream();
					}
					if (inputStream != null) {
						FileUtils.copyInputStreamToFile(inputStream, outputFile);
					}
				} catch (IOException e) {
					return null;
				}
				
				return outputFile.getAbsolutePath();
			}

			@Override
			protected void onPostExecute(String filePath) {
				streamCopyListener.onFinishedCopy(filePath);
			}
		}.execute();
	}

	public interface CopyUriToFileListener {
		void onFinishedCopy(String filePath);
	}
}
