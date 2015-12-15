package com.artech.utils;

import android.animation.StateListAnimator;
import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.artech.R;
import com.artech.base.services.Services;
import com.artech.compatibility.CompatibilityHelper;

class ThemeDefaults
{
	static void resetBackground(View view)
	{
		DefaultViewStyle defaultStyle = getDefaultStyle(view, true);
		if (defaultStyle != null)
			CompatibilityHelper.setBackground(view, defaultStyle.mBackground);
	}

	static void resetTypeface(TextView tv)
	{
		DefaultViewStyle defaultStyle = getDefaultStyle(tv, true);
		if (defaultStyle != null)
			tv.setTypeface(defaultStyle.mTypeface, defaultStyle.mTypeFaceStyle);
	}

	static void resetTextSize(TextView tv)
	{
		// UNIT_PX because getTextSize() returns pixels, but setTextSize(size) expects scaled pixels.
		DefaultViewStyle style = getDefaultStyle(tv, true);
		if (style != null)
			tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, style.mTextSize);
	}

	static void resetTextColor(TextView tv)
	{
		DefaultViewStyle style = getDefaultStyle(tv, true);
		if (style != null)
			tv.setTextColor(style.mTextColor);
	}

	@SuppressLint("NewApi")
	static void resetElevation(View view)
	{
		DefaultViewStyle defaultStyle = getDefaultStyle(view, true);
		if (defaultStyle != null)
			ViewCompat.setElevation(view, defaultStyle.mElevation);
		
		// For buttons, setting elevation clears StateListAnimator. So, restore it too.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			if (view instanceof Button && defaultStyle != null && defaultStyle.mStateListAnimator != null)
				view.setStateListAnimator(defaultStyle.mStateListAnimator);
		}
	}
	
	static int getDefaultTextColor(TextView tv)
	{
		DefaultViewStyle style = getDefaultStyle(tv, true);
		return (style != null && style.mTextColor != null ? style.mTextColor.getDefaultColor() : Color.WHITE);
	}

	static Drawable getDefaultBackground(View view)
	{
		DefaultViewStyle style = getDefaultStyle(view, true);
		return (style != null ? style.mBackground : null);
	}

	private static final int DEFAULT_TAG = R.id.tag_view_default_style;

	static void beforeSetThemeProperties(View view)
	{
		// Store defaults, unless it's already been done.
		DefaultViewStyle defaultStyle = getDefaultStyle(view, false);
		if (defaultStyle == null)
		{
			defaultStyle = new DefaultViewStyle(view);
			view.setTag(DEFAULT_TAG, defaultStyle);
		}
	}

	private static DefaultViewStyle getDefaultStyle(View view, boolean shouldExist)
	{
		DefaultViewStyle defaultStyle = Cast.as(DefaultViewStyle.class, view.getTag(DEFAULT_TAG));

		if (defaultStyle == null && shouldExist)
			Services.Log.warning(String.format("Default style not available for %s (%s). Method beforeSetThemeProperties() should have been called previously but it wasn't.", view.toString(), view.getClass().getName()));

		return defaultStyle;
	}

	private static class DefaultViewStyle
	{
		private Drawable mBackground;
		private float mElevation;
		private Typeface mTypeface;
		private int mTypeFaceStyle;
		private Float mTextSize;
		private ColorStateList mTextColor;
		private StateListAnimator mStateListAnimator;

		@SuppressLint("NewApi")
		DefaultViewStyle(View view)
		{
			mBackground = view.getBackground();
			mElevation = ViewCompat.getElevation(view);
			
			if (view instanceof TextView)
			{
				TextView textView = (TextView)view;
				mTypeface = textView.getTypeface();
				mTypeFaceStyle = Typeface.NORMAL; // This is not 100% accurate, as the base style may have the fake bits on... but I didn't find a way to obtain it.
				mTextSize = textView.getTextSize();
				mTextColor = textView.getTextColors();
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			{
				// Only stores this for Button since it's the only known widget (so far) that uses it to override elevation.
				if (view instanceof Button)
					mStateListAnimator = view.getStateListAnimator();
			}
		}
	}
}
