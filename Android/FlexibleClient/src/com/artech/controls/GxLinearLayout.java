package com.artech.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.artech.base.metadata.theme.LayoutBoxMeasures;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.utils.BackgroundOptions;
import com.artech.utils.Cast;
import com.artech.utils.ThemeUtils;

public class GxLinearLayout extends android.widget.LinearLayout implements IGxThemeable
{
	private LayoutBoxMeasures mMargins;
	private ThemeClassDefinition mThemeClass;

	public GxLinearLayout(Context context)
	{
		super(context);
	}

	public GxLinearLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	@Override
	public void setLayoutParams(ViewGroup.LayoutParams params)
	{
		if (mMargins != null && Cast.as(MarginLayoutParams.class, params) != null)
			((MarginLayoutParams)params).setMargins(mMargins.left, mMargins.top, mMargins.right, mMargins.bottom);

		super.setLayoutParams(params);
	}

	@Override
	public void setThemeClass(ThemeClassDefinition themeClass) {

		mThemeClass = themeClass;
		applyClass(themeClass);
	}

	@Override
	public ThemeClassDefinition getThemeClass() {
		return mThemeClass;
	}

	protected void setBackgroundBorderProperties(ThemeClassDefinition themeClass)
	{
		ThemeUtils.setBackgroundBorderProperties(this, themeClass, BackgroundOptions.DEFAULT);
	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass) {
		if (themeClass == null)
			return;
		//Margins
		LayoutBoxMeasures margins = themeClass.getMargins();
		if (margins!=null)
		{

			ViewGroup.LayoutParams lp = getLayoutParams(); // The layout could not be on site yet, differ the setting to the setLayoutParams
			if (lp != null) {
				MarginLayoutParams marginParms = Cast.as(MarginLayoutParams.class, lp); // does its site support margins?
				if (marginParms != null) {
					marginParms.setMargins( margins.left, margins.top,margins.right, margins.bottom);
					setLayoutParams(lp);
				}
			}
			else {
				mMargins = margins;
			}
		}

		//Padding
		LayoutBoxMeasures padding = themeClass.getPadding();
		if (padding != null)
			setPadding(padding.left, padding.top, padding.right, padding.bottom);

		// Background and Border.
		setBackgroundBorderProperties(themeClass);
	}
}
