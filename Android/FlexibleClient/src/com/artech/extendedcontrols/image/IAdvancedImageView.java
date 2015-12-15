package com.artech.extendedcontrols.image;

import android.graphics.Bitmap;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;

import com.artech.base.metadata.enums.ImageScaleType;

public interface IAdvancedImageView  {

	void setMaxZoom(float max);

	void setMinZoom(float min);

	void setImageBitmap(Bitmap bmp);

	void setLayoutParams(LayoutParams params);

	void setOnLongClickListener( OnLongClickListener listener);

	void invalidate();

	void setImageScaleType(ImageScaleType scaleType);
}
