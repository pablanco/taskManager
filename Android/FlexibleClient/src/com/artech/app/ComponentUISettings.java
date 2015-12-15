package com.artech.app;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.artech.base.metadata.layout.Size;
import com.artech.fragments.BaseFragment;

/**
 * Created by matiash on 01/07/2015.
 */
public class ComponentUISettings
{
	public boolean isMain;
	public BaseFragment parent;
	public Size size;

	public ComponentUISettings(boolean isMain, BaseFragment parent, Size size)
	{
		this.isMain = isMain;
		this.parent = parent;
		this.size = size;
	}

	public static @NonNull ComponentUISettings main()
	{
		return new ComponentUISettings(true, null, null);
	}

	public static @NonNull ComponentUISettings childOf(@NonNull BaseFragment parent, @Nullable Size size)
	{
		return new ComponentUISettings(false, parent, size);
	}
}
