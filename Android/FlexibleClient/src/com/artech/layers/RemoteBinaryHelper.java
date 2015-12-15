package com.artech.layers;

import java.io.InputStream;
import java.net.HttpURLConnection;

import com.artech.application.MyApplication;
import com.artech.base.services.ServiceResponse;
import com.artech.common.IProgressListener;
import com.artech.common.ServiceHelper;

class RemoteBinaryHelper
{
	public static String upload(String fileExtension, String fileMimeType, InputStream data, long dataLength, IProgressListener progressListener)
	{
		String uploadUrl = MyApplication.getApp().UriMaker.MakeImagesServer();
		ServiceResponse serviceResponse = ServiceHelper.uploadInputStreamToServer(uploadUrl, data, dataLength, fileMimeType, progressListener);

		if (serviceResponse.HttpCode == HttpURLConnection.HTTP_CREATED)
			return serviceResponse.get("object_id");
		else
			return null;
	}
}
