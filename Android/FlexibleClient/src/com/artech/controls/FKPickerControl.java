package com.artech.controls;

import android.content.Context;
import android.widget.ImageView;

import com.artech.common.StandardImages;

public class FKPickerControl extends ImageView
{
	public FKPickerControl(Context context)
	{
		super(context);
		StandardImages.setPromptImage(this);
	}
}