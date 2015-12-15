package com.artech.utils;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.widget.TextView;

import com.artech.base.metadata.enums.Alignment;

public class DrawableUtils
{
	public static Drawable newStateListDrawable(Drawable normal, Drawable selected)
	{
		if (normal == null && selected == null)
			return null;
		
		if (normal != null && selected == null)
			return normal;

		//noinspection ConstantConditions
		if (normal == null && selected != null)
			return selected;
		
		StateListDrawable stateDrawable = new StateListDrawable();
		stateDrawable.addState(new int[] { android.R.attr.state_selected }, selected);
		stateDrawable.addState(new int[] { android.R.attr.state_pressed }, selected);
		stateDrawable.addState(new int[] { }, normal);

		return stateDrawable;
	}

	public static Drawable getTintedDrawable(Drawable drawable, int tintColor)
	{
		drawable = DrawableCompat.wrap(drawable);
		DrawableCompat.setTint(drawable, tintColor);
		return drawable;
	}

	public static void setCompoundDrawableWithIntrinsicBounds(TextView view, Drawable drawable, int position)
	{
		view.setCompoundDrawablesWithIntrinsicBounds(
			position == Alignment.LEFT ? drawable : null,
			position == Alignment.TOP ? drawable : null,
			position == Alignment.RIGHT ? drawable : null,
			position == Alignment.BOTTOM ? drawable : null);		
	}
}
