package com.artech.android.api;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.artech.actions.ActionResult;
import com.artech.activities.ActivityHelper;
import com.artech.android.media.MediaHelper;
import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;

public class CameraAPI extends ExternalApi
{
	private static final String METHOD_TAKE_PHOTO = "TakePhoto";
	private static final String METHOD_RECORD_VIDEO = "RecordVideo";

	private File mOutputMediaFile = null;

	public CameraAPI() {
		addMethodHandler(METHOD_TAKE_PHOTO, 0, mMethodTakePhoto);
		addMethodHandler(METHOD_RECORD_VIDEO, 0, mMethodRecordVideo);
	}

	private final IMethodInvoker mMethodTakePhoto = new IMethodInvoker() {
		@Override
		public @NonNull ExternalApiResult invoke(List<Object> parameters) {
			ActivityHelper.registerActionRequestCode(MediaHelper.TAKE_PICTURE);
			mOutputMediaFile = MediaHelper.takePicture(getActivity());
			return ExternalApiResult.SUCCESS_WAIT;
		}
	};

	private final IMethodInvoker mMethodRecordVideo = new IMethodInvoker() {
		@Override
		public @NonNull ExternalApiResult invoke(List<Object> parameters) {
			ActivityHelper.registerActionRequestCode(MediaHelper.CAPTURE_VIDEO);
			mOutputMediaFile = MediaHelper.captureVideo(getActivity());
			return ExternalApiResult.SUCCESS_WAIT;
		}
	};

	@Override
	public ExternalApiResult afterActivityResult(int requestCode, int resultCode, Intent result, String method) {
		if ((METHOD_TAKE_PHOTO.equalsIgnoreCase(method) || METHOD_RECORD_VIDEO.equalsIgnoreCase(method))
				&& resultCode == Activity.RESULT_OK) {
			String mediaPath = MediaHelper.getTakenMediaUri(getActivity().getApplicationContext(), result, mOutputMediaFile).getPath();
			return new ExternalApiResult(ActionResult.SUCCESS_CONTINUE, mediaPath);
		}

		return null;
	}
}
