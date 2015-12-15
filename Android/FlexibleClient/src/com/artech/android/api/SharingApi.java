package com.artech.android.api;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;

import com.artech.base.utils.Strings;
import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;
import com.fedorvlasov.lazylist.ImageLoader;

public class SharingApi extends ExternalApi
{
	public static final String OBJECT_NAME = "SharingAPI";
	private static final String METHOD_SHARE_TEXT = "ShareText";
	private static final String METHOD_SHARE_IMAGE = "ShareImage";

	public SharingApi()
	{
		addMethodHandler(METHOD_SHARE_TEXT, 3, new IMethodInvoker()
		{
			@Override
			public @NonNull ExternalApiResult invoke(List<Object> parameters)
			{
				List<String> values = ExternalApi.toString(parameters);
				shareText(values.get(2), values.get(0), values.get(1));
				return ExternalApiResult.SUCCESS_WAIT;
			}
		});

		addMethodHandler(METHOD_SHARE_IMAGE, 4, new IMethodInvoker()
		{
			@Override
			public @NonNull ExternalApiResult invoke(List<Object> parameters)
			{
				List<String> values = ExternalApi.toString(parameters);
				shareImage(values.get(3), values.get(1), values.get(2), values.get(0));
				return ExternalApiResult.SUCCESS_WAIT;
			}
		});
	}

	private void shareText(String title, String text, String url)
	{
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_SUBJECT, title);
		intent.putExtra(Intent.EXTRA_TEXT, getSharedText(text, url));

		startShareActivity(intent);
	}

	private void shareImage(String title, String text, String url, String image)
	{
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("image/*");
		intent.putExtra(Intent.EXTRA_SUBJECT, title);
		intent.putExtra(Intent.EXTRA_TEXT, getSharedText(text, url));
		intent.putExtra(Intent.EXTRA_STREAM, getSharedImage(image));

		startShareActivity(intent);
	}

	private void startShareActivity(Intent intent)
	{
		getContext().startActivity(Intent.createChooser(intent, null));
	}

	private String getSharedText(String text, String url)
	{
		if (Strings.hasValue(text) && Strings.hasValue(url))
			return text + Strings.NEWLINE + url;
		else if (Strings.hasValue(text))
			return text;
		else if (Strings.hasValue(url))
			return url;
		else
			return Strings.EMPTY;
	}

	private Uri getSharedImage(String image)
	{
		if (Strings.hasValue(image))
		{
			// Get the image file (either from cache, or downloading it now).
			File imageFile = ImageLoader.getImage(image);
			if (imageFile != null && imageFile.exists())
			{
				try
				{
					// Copy to the place where the FileProvider will grab it.
					// Use a random name to avoid exposing the original file name.
					File providerDir = new File(getContext().getFilesDir(), "shared_files");
					String providerFilename = UUID.randomUUID().toString().replace("-", "") + "." + FilenameUtils.getExtension(imageFile.getAbsolutePath());
					File providerFile = new File(providerDir, providerFilename);
					FileUtils.copyFile(imageFile, providerFile);

					// Create a temporary uri for sharing.
					String providerAuthority = getContext().getPackageName() + ".fileprovider";
					return FileProvider.getUriForFile(getContext(), providerAuthority, providerFile);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		return null;
	}
}
