package com.artech.controls.tabs;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.artech.R;
import com.artech.base.metadata.enums.Alignment;
import com.artech.base.metadata.theme.TabControlThemeClassDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.Services;
import com.artech.common.ImageHelper;
import com.artech.utils.BackgroundOptions;
import com.artech.utils.DrawableUtils;
import com.artech.utils.ThemeUtils;

public class TabUtils
{
	private final static int TAB_DRAWABLE_PADDING_DIPS = 4;

	public static void applyTabControlClass(ViewGroup container, SlidingTabLayout slidingTabs, TabControlThemeClassDefinition themeClass)
	{
		if (themeClass == null)
			return;
		
		// Reorder children, if necessary, according to "tabs at bottom" preference.
		boolean isTabStripAtBottom = (container.getChildAt(1) == slidingTabs);
		boolean shouldPutTabStripAtBottom = themeClass.getTabStripPosition() == TabControlThemeClassDefinition.TAB_STRIP_POSITION_BOTTOM;
		if (isTabStripAtBottom != shouldPutTabStripAtBottom)
		{
			View previousFirstChild = container.getChildAt(0);
			container.removeView(previousFirstChild);
			container.addView(previousFirstChild);
		}

		// Background for the whole tab control.
		ThemeUtils.setBackgroundBorderProperties(container, themeClass, BackgroundOptions.DEFAULT);

		// Background for the tab strip only.
		Integer tabStripColor = ThemeUtils.getColorId(themeClass.getTabStripColor());
		if (tabStripColor != null)
			slidingTabs.setBackgroundColor(tabStripColor);

		Integer indicatorColor = ThemeUtils.getColorId(themeClass.getIndicatorColor());
		if (indicatorColor == null)
			indicatorColor = ThemeUtils.getAndroidThemeColorId(container.getContext(), R.attr.colorAccent); // As per material design guidelines.
		if (indicatorColor != null)
			slidingTabs.setSelectedIndicatorColors(indicatorColor);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && themeClass.getTabStripElevation() != null)
			slidingTabs.setElevation(themeClass.getTabStripElevation());

		// As per Material Design guidelines
		slidingTabs.setDividerColors(Color.TRANSPARENT);
	}

	public static void applyTabItemClass(TextView tabTitleView, ThemeClassDefinition normalClass, ThemeClassDefinition selectedClass)
	{
		applyTabItemClass(tabTitleView, normalClass, selectedClass, null);
	}
	
	public static ThemeClassDefinition applyTabItemClass(TextView tabTitleView, ThemeClassDefinition normalClass, ThemeClassDefinition selectedClass, ThemeClassDefinition currentAppliedClass)
	{
		ThemeClassDefinition themeClass;
		if (normalClass != null && selectedClass != null)
			themeClass = (tabTitleView.isSelected() ? selectedClass : normalClass);
		else if (normalClass != null)
			themeClass = normalClass;
		else if (selectedClass != null)
			themeClass = selectedClass;
		else
			themeClass = null;
		
		if (themeClass != null && themeClass != currentAppliedClass)
		{
			ThemeUtils.setBackgroundBorderProperties(tabTitleView, themeClass, BackgroundOptions.DEFAULT);
			ThemeUtils.setFontProperties(tabTitleView, themeClass);
			return themeClass;
		}
		else
			return currentAppliedClass;
	}
	
	public static void setTabImage(TextView tabTitleView, String image, String selectedImage)
	{
		setTabImage(tabTitleView, image, selectedImage, Alignment.LEFT);
	}
	
	public static void setTabImage(TextView tabTitleView, String image, String selectedImage, int imageAlignment)
	{
		Drawable normalDrawable = ImageHelper.getStaticImage(image);
		Drawable selectedDrawable = ImageHelper.getStaticImage(selectedImage);

		Drawable tabImage = DrawableUtils.newStateListDrawable(normalDrawable, selectedDrawable);
		if (tabImage != null)
		{
			tabTitleView.setCompoundDrawablePadding(Services.Device.dipsToPixels(TAB_DRAWABLE_PADDING_DIPS));
			DrawableUtils.setCompoundDrawableWithIntrinsicBounds(tabTitleView, tabImage, imageAlignment);
		}
	}
}
