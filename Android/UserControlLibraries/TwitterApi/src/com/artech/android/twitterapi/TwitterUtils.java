package com.artech.android.twitterapi;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

import twitter4j.TwitterException;

import com.artech.application.MyApplication;
import com.artech.base.services.Services;

import android.text.TextUtils;
import android.webkit.MimeTypeMap;

class TwitterUtils {

	static boolean isValidTwitterUsername(String userName) {
		return (!TextUtils.isEmpty(userName) && userName.trim().matches("\\w{1,15}"));
	}

	static boolean isValidTwitterStatusId(String statusId) {
		return (!TextUtils.isEmpty(statusId) && statusId.trim().matches("\\d{1,19}"));
	}

	// Receives a path with scheme.
	static boolean isValidImagePath(String imagePath) {
		if (TextUtils.isEmpty(imagePath)) {
			return false;
		}
	
		String fileExt;
	
		if (!imagePath.startsWith("http")) {
			File imageFile = new File(imagePath);
	
			if (!imageFile.exists()) {
				return false;
			}
	
			fileExt = FilenameUtils.getExtension(imageFile.getName());
		} else {
			fileExt = MimeTypeMap.getFileExtensionFromUrl(imagePath);
		}
	
		String fileMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExt);
	
		if (fileMimeType == null || !fileMimeType.startsWith("image/")) {
			return false;
		}
	
		return true;
	}
	
	static void showErrorMessageOnApp(TwitterException te) {
		if (te.isCausedByNetworkIssue()) {
			MyApplication.getInstance().showMessage(Services.Strings.getResource(R.string.GXM_TwitterNetworkIssue));
		} else if (te.isErrorMessageAvailable()) {
			MyApplication.getInstance().showMessage(te.getErrorMessage());
		} else {
			MyApplication.getInstance().showMessage(Services.Strings.getResource(R.string.GXM_TwitterOperationError));
		}
	}

}
