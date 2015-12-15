package com.artech.controls;

import android.content.Context;
import android.util.AttributeSet;

import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.LayoutBoxMeasures;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.utils.BackgroundOptions;
import com.artech.utils.ThemeUtils;

/**
 * Control used for standalone images (i.e. not inside DataBoundControl).
 * @author matiash
 *
 */
public class GxImageViewStatic extends GxImageViewBase
{
	private LayoutItemDefinition mDefinition;

	public GxImageViewStatic(Context context)
	{
		super(context);
	}

	public GxImageViewStatic(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public GxImageViewStatic(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public GxImageViewStatic(Context context, LayoutItemDefinition definition)
	{
		super(context);
		mDefinition = definition;
		setAutogrow(definition.hasAutoGrow());
	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass)
	{
		if (themeClass != null)
		{
			// Padding
			LayoutBoxMeasures padding = themeClass.getPadding();
			if (padding != null)
				setPadding(padding.left, padding.top, padding.right, padding.bottom);

			// Background and border
			ThemeUtils.setBackgroundBorderProperties(this, themeClass, BackgroundOptions.defaultFor(mDefinition));

			// Scale type and custom size.
			setPropertiesImageSizeScaleRadiusFromThemeClass(themeClass);
		}
	}
}
