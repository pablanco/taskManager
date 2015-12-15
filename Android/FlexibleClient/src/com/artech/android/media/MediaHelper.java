package com.artech.android.media;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.MediaStore;

import com.artech.R;
import com.artech.android.media.utils.CameraUtils;
import com.artech.android.media.utils.FileUtils;
import com.artech.application.MyApplication;
import com.artech.base.services.Services;
import com.artech.common.StorageHelper;

public class MediaHelper {
	public final static int TAKE_PICTURE = 701;
	public final static int CAPTURE_VIDEO = 702;
	public final static int CAPTURE_AUDIO = 703;
	public final static int PICK_IMAGE = 801;
	public final static int PICK_VIDEO = 802;
	public final static int PICK_AUDIO = 803;
	private final static String IMAGE_EXTENSION = "jpg";
	private final static String VIDEO_EXTENSION = "mp4";
	private final static String AUDIO_EXTENSION = "mp3";
	
	public static File takePicture(Activity activity) {
		return takeMedia(activity, TAKE_PICTURE);
	}
	
	public static File captureVideo(Activity activity) {
		return takeMedia(activity, CAPTURE_VIDEO);
	}
	
	public static File captureAudio(Activity activity) {
		return takeMedia(activity, CAPTURE_AUDIO);
	}
	
	public static void pickImage(Activity activity) {
		pickMedia(activity, PICK_IMAGE);
	}
	
	public static void pickVideo(Activity activity) {
		pickMedia(activity, PICK_VIDEO);
	}
	
	public static void pickAudio(Activity activity) {
		pickMedia(activity, PICK_AUDIO);
	}
	
	public static Uri getTakenMediaUri(Context context, Intent result, File outputMediaFile) {
		// When MediaStore.EXTRA_OUTPUT is used, the media file is written to the uri passed and the result intent is NULL.
		// Otherwise, the media file is written to the default gallery directory and its uri is returned in the data field of the result intent.
		// In the latter case, we have to copy the media file to the app's directory.
		if (result != null && result.getData() != null) {
			Uri mediaFileUri = result.getData();
			if (!mediaFileUri.equals(Uri.fromFile(outputMediaFile))) {
				CameraUtils.copyUriToFile(context, mediaFileUri, outputMediaFile);
			}
		}
		
		// We know where it was saved.
		return  Uri.fromFile(outputMediaFile);
	}

	public static Uri getPickedMediaUri(Intent data) {
		if (data == null || data.getData() == null) {
			Services.Log.debug("Intent or its data was null.");
			return null;
		}

		Uri uri = data.getData();
		
		if (uri.getScheme() == null) {
			uri = Uri.parse("file://" + uri.toString());
		}
		
		return uri;
	}
	
	private static File takeMedia(Activity activity, int type) {
		String intentAction = null;
		File outputMediaFile = null;
		
		switch (type) {
		case TAKE_PICTURE:
			intentAction = MediaStore.ACTION_IMAGE_CAPTURE;
			outputMediaFile = StorageHelper.getNewCameraFile(IMAGE_EXTENSION);
			break;
		case CAPTURE_VIDEO:
			intentAction = MediaStore.ACTION_VIDEO_CAPTURE;
			outputMediaFile = StorageHelper.getNewCameraFile(VIDEO_EXTENSION);
			break;
		case CAPTURE_AUDIO:
			intentAction = MediaStore.Audio.Media.RECORD_SOUND_ACTION;
			outputMediaFile = StorageHelper.getNewCameraFile(AUDIO_EXTENSION);
			break;
		}
		
		if (intentAction == null || outputMediaFile == null) {
			return null;
		}
		
		Intent intent = new Intent(intentAction);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		if (CameraUtils.supportsExtraOutput(intentAction)) {
			intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(outputMediaFile));
		}
		
		startForResultIfAppsAvailable(activity, type, intent);
		
		return outputMediaFile;
	}
	
	private static void pickMedia(Activity activity, int mediaPickerType) {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		
		switch (mediaPickerType) {
			case PICK_IMAGE:
				intent.setType(FileUtils.MIME_TYPE_IMAGE);
				break;
			case PICK_VIDEO:
				intent.setType(FileUtils.MIME_TYPE_VIDEO);
				break;
			case PICK_AUDIO:
				intent.setType(FileUtils.MIME_TYPE_AUDIO);
				break;
		}
		
		startForResultIfAppsAvailable(activity, mediaPickerType, intent);
	}
	
	private static void startForResultIfAppsAvailable(Activity activity, int type, Intent intent) {
		List<ResolveInfo> appsList = activity.getApplicationContext().getPackageManager()
				.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		if (appsList.size() > 0) {
			activity.startActivityForResult(intent, type);
		} else {
			MyApplication.getInstance().showMessage(Services.Strings.getResource(R.string.GXM_NoApplicationAvailable));
		}
	}
}
