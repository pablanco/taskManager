package com.artech.android.api;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.artech.actions.ActionExecution;
import com.artech.actions.ActionResult;
import com.artech.activities.ActivityHelper;
import com.artech.android.downloader.FileDownloader;
import com.artech.android.downloader.FileDownloader.FileDownloaderListener;
import com.artech.android.media.MediaHelper;
import com.artech.android.media.MediaUtils;
import com.artech.android.media.MediaUtils.CopyUriToFileListener;
import com.artech.base.utils.Strings;
import com.artech.common.StorageHelper;
import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;

public class PhotoLibraryAPI extends ExternalApi
{
	private static final String METHOD_SAVE_IMAGE = "Save";
	private static final String METHOD_SAVE_VIDEO = "SaveVideo";
	private static final String METHOD_CHOOSE_IMAGE = "ChooseImage";
	private static final String METHOD_CHOOSE_VIDEO = "ChooseVideo";

	private String mSelectedMediaFilePath = null;

	public PhotoLibraryAPI()
	{
		addMethodHandler(METHOD_SAVE_IMAGE, 1, mMethodSaveMedia);
		addMethodHandler(METHOD_SAVE_VIDEO, 1, mMethodSaveMedia);
		addMethodHandler(METHOD_CHOOSE_IMAGE, 0, mMethodChooseImage);
		addMethodHandler(METHOD_CHOOSE_VIDEO, 0, mMethodChooseVideo);
	}

	private final IMethodInvoker mMethodSaveMedia = new IMethodInvoker() {
		@Override
		public @NonNull ExternalApiResult invoke(List<Object> parameters) {
			String mediaPath = (String) parameters.get(0);
			if (!Strings.hasValue(mediaPath))
				return ExternalApiResult.SUCCESS_CONTINUE; // TODO: Should be failure?

			Uri mediaUri = Uri.parse(mediaPath);
			String scheme = mediaUri.getScheme();

			if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
			{
				FileDownloader.downloadMediaFile(getContext(), mSaveMediaDownloadListener, mediaUri);
				return ExternalApiResult.SUCCESS_WAIT;
			}
			else if (scheme == null || ContentResolver.SCHEME_FILE.equalsIgnoreCase(scheme))
			{
				String fileName = FilenameUtils.getName(mediaPath);
				MediaUtils.addToGallery(mediaUri, fileName);
				return ExternalApiResult.SUCCESS_CONTINUE;
			}
			else
				return ExternalApiResult.SUCCESS_CONTINUE; // TODO: Should be failure?
		}
	};

	private final IMethodInvoker mMethodChooseImage = new IMethodInvoker() {
		@Override
		public @NonNull ExternalApiResult invoke(List<Object> parameters) {
			ActivityHelper.registerActionRequestCode(MediaHelper.PICK_IMAGE);
			MediaHelper.pickImage(getActivity());
			return ExternalApiResult.SUCCESS_WAIT;
		}
	};

	private final IMethodInvoker mMethodChooseVideo = new IMethodInvoker() {
		@Override
		public @NonNull ExternalApiResult invoke(List<Object> parameters) {
			ActivityHelper.registerActionRequestCode(MediaHelper.PICK_VIDEO);
			MediaHelper.pickVideo(getActivity());
			return ExternalApiResult.SUCCESS_WAIT;
		}
	};

	@Override
	public ExternalApiResult afterActivityResult(int requestCode, int resultCode, Intent result, String method) {
		if ((METHOD_CHOOSE_IMAGE.equals(method) || METHOD_CHOOSE_VIDEO.equals(method))
				&& resultCode == Activity.RESULT_OK) {
			if (MediaUtils.isPickMediaRequest(requestCode)) {
				mSelectedMediaFilePath = null;
				Uri uri = MediaHelper.getPickedMediaUri(result);
				if (uri != null) {
					String scheme = uri.getScheme();
					if (ContentResolver.SCHEME_CONTENT.equals(scheme) ||
							"http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
						File outputFile = new File(StorageHelper.getImagesDirectory(), MediaUtils.getFileName(uri));
						MediaUtils.copyUriToFileAsync(uri, outputFile, mCopyUriToFileListener);
						return ExternalApiResult.SUCCESS_WAIT;
					} else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
						return new ExternalApiResult(ActionResult.SUCCESS_CONTINUE, uri.getPath());
					}
				}
			} else {
				return new ExternalApiResult(ActionResult.SUCCESS_CONTINUE, mSelectedMediaFilePath);
			}
		}
		return ExternalApiResult.SUCCESS_CONTINUE;
	}

	private FileDownloaderListener mSaveMediaDownloadListener = new FileDownloaderListener() {

		@Override
		public void OnFileDownloaded(Uri fileUri, String fileName) {
			MediaUtils.addToGallery(fileUri, fileName);
			ActionExecution.continueCurrent(getActivity(), false);
		}
	};

	private CopyUriToFileListener mCopyUriToFileListener = new CopyUriToFileListener() {

		@Override
		public void onFinishedCopy(String filePath) {
			mSelectedMediaFilePath = filePath;
			ActionExecution.continueCurrent(getActivity(), false);
		}
	};
}
