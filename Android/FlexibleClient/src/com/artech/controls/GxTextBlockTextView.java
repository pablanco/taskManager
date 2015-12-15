package com.artech.controls;

import android.content.Context;
import android.util.AttributeSet;

import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.LayoutBoxMeasures;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.controls.utils.TextViewUtils;
import com.artech.utils.BackgroundOptions;
import com.artech.utils.ThemeUtils;

public class GxTextBlockTextView extends android.widget.TextView implements IGxThemeable, IGxLocalizable
{
	private LayoutItemDefinition mDefinition;
	private ThemeClassDefinition mThemeClass;

	public GxTextBlockTextView(Context context)
	{
		super(context);
	}

	public GxTextBlockTextView(Context context, LayoutItemDefinition definition)
	{
		super(context);
		mDefinition = definition;
	}

	public GxTextBlockTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public GxTextBlockTextView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public void setCaption(String caption) {
		TextViewUtils.setText(this, caption, mDefinition);
	}

	@Override
	public void setThemeClass(ThemeClassDefinition themeClass)
	{
		mThemeClass = themeClass;
		applyClass(themeClass);
	}

	@Override
	public ThemeClassDefinition getThemeClass() {
		return mThemeClass;
	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass)
	{
		// Set font properties
		ThemeUtils.setFontProperties(this, themeClass);

		// Set padding
		if (themeClass != null)
		{
			LayoutBoxMeasures padding = themeClass.getPadding();
			if (padding != null)
				setPadding(padding.left, padding.top, padding.right, padding.bottom);
		}

		// Set background and border properties
		ThemeUtils.setBackgroundBorderProperties(this, themeClass, BackgroundOptions.defaultFor(mDefinition));
	}

	@Override
	public void onTranslationChanged() {
		if (mDefinition != null) {
			mDefinition.getControlType();
			TextViewUtils.setText(this, mDefinition.getCaption(), mDefinition);
		}
	}
}
