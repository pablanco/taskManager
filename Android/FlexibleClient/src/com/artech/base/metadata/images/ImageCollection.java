package com.artech.base.metadata.images;

import java.io.Serializable;
import java.util.TreeMap;

public class ImageCollection implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final TreeMap<String, ImageFile> mImages;

	private final String mLanguage;
	private final String mTheme;
	private final boolean mIsDefault;
	private final String mBaseDirectory;

	public ImageCollection(String language, String theme, boolean isDefault, String baseDirectory)
	{
		mImages  = new TreeMap<String, ImageFile>(String.CASE_INSENSITIVE_ORDER);

		mLanguage = language;
		mTheme = theme;
		mIsDefault = isDefault;
		mBaseDirectory = baseDirectory;
	}

	public String getLanguage() { return mLanguage; }
	public String getTheme() { return mTheme; }
	public boolean isDefault() { return mIsDefault; }
	public String getBaseDirectory() { return mBaseDirectory; }

	public void add(ImageFile file)
	{
		mImages.put(file.getName(), file);
	}

	public ImageFile get(String imageName)
	{
		return mImages.get(imageName);
	}

}
