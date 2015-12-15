package com.artech.android.audio;

import java.io.Serializable;
import java.util.Locale;

import com.artech.R;
import com.artech.application.MyApplication;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.StorageHelper;

public class AudioItem implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final String mUri;
	private String mTitle;
	private String mArtist;
	private String mAlbum;

	public AudioItem(String uri)
	{
		this(uri, Strings.EMPTY);
	}

	public AudioItem(String uri, String title)
	{
		// Convert "uri" to a "real uri" (add proper scheme and/or full path if missing).
		if (!uri.contains("://"))
		{
			if (StorageHelper.isLocalFile(uri))
				uri = "file://" + uri;
			else
				uri = MyApplication.getApp().UriMaker.MakeImagePath(uri);
		}

		mUri = uri;
		mTitle = title;

		if (!Services.Strings.hasValue(mTitle))
			mTitle = Services.Strings.getResource(R.string.GXM_AudioDescription);
	}

	public String getUri()
	{
		return mUri;
	}

	public String getAudioPlayerUri()
	{
		String uri = mUri;
		final String FILE_SCHEME = "file://";

		// MediaPlayer does not support the file:// uri scheme, so remove it if present.
		if (uri != null && Strings.starsWithIgnoreCase(uri, FILE_SCHEME))
			uri = uri.substring(FILE_SCHEME.length());

		return uri;
	}

	public static boolean areEqual(AudioItem item1, AudioItem item2)
	{
		return (item1 != null && item2 != null &&
				item1.getAudioPlayerUri().equalsIgnoreCase(item2.getAudioPlayerUri()));
	}

	public boolean isRemote()
	{
		return mUri.toLowerCase(Locale.US).startsWith("http:") || mUri.toLowerCase(Locale.US).startsWith("https:");
	}

	public String getTitle() { return mTitle; }
	public String getArtist() { return mArtist; }
	public String getAlbum() { return mAlbum; }
}
