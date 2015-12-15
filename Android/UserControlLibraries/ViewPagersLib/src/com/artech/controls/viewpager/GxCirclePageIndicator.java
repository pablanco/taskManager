package com.artech.controls.viewpager;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.artech.controls.magazineviewer.FlipperOptions;
import com.viewpagerindicator.CirclePageIndicator;

public class GxCirclePageIndicator extends CirclePageIndicator
{
    public GxCirclePageIndicator(Context context)
    {
        super(context);
    }

    public GxCirclePageIndicator(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

	public GxCirclePageIndicator(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public void setOptions(FlipperOptions options)
	{
		// Default colors.
		setFillColor(0xFFCCCCCC);   // Light gray
		setStrokeColor(0xFF888888); // Gray

		// Custom colors.
		if (options.getFooterBackgroundColor() != null)
			setBackgroundColor(options.getFooterBackgroundColor());

		if (options.getFooterSelectedColor() != null)
			setFillColor(options.getFooterSelectedColor());

		if (options.getFooterUnselectedColor() != null)
			setStrokeColor(options.getFooterUnselectedColor());

		// Footer visible?
		if (!options.isShowFooter())
			setVisibility(View.GONE);
	}
}
