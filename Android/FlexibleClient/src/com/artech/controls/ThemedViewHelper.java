package com.artech.controls;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;

import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.LayoutBoxMeasures;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.utils.BackgroundOptions;
import com.artech.utils.Cast;
import com.artech.utils.ThemeUtils;

/**
 * Class used to centralize margin/padding/background code among all IGxThemeable controls.
 * Should be called on applyClass() and on setLayoutParams().
 * @author matiash
 */
public class ThemedViewHelper
{
	// Associated view and flags to determine what features to use.
	private final View mView;
	private final LayoutItemDefinition mLayoutItem;

	private boolean mSetMargins = true;
	private boolean mSetPadding = true;
	private boolean mSetBackground = true;

	// Deferred info.
	private LayoutBoxMeasures mDeferredMargins;

	public ThemedViewHelper(View view, LayoutItemDefinition layoutItem)
	{
		mView = view;
		mLayoutItem = layoutItem;
	}

	public void setThemeClass(ThemeClassDefinition themeClass)
	{
		if (themeClass == null)
			return;

		// Margins.
		if (mSetMargins)
		{
			LayoutBoxMeasures margins = themeClass.getMargins();
			if (margins != null)
			{
				 // The layout could not be on site yet; in that case differ the setting to setLayoutParams().
				ViewGroup.LayoutParams lp = mView.getLayoutParams();
				if (lp != null)
				{
					// Does its site support margins?
					MarginLayoutParams marginParams = Cast.as(MarginLayoutParams.class, lp);
					if (marginParams != null)
					{
						marginParams.setMargins(margins.left, margins.top,margins.right, margins.bottom);
						mView.setLayoutParams(lp);
					}
				}
				else
					mDeferredMargins = margins;
			}
		}

		// Padding.
		if (mSetPadding)
		{
			LayoutBoxMeasures padding = themeClass.getPadding();
			if (padding != null)
				mView.setPadding(padding.left, padding.top, padding.right, padding.bottom);
		}

		// Background and border.
		if (mSetBackground)
			ThemeUtils.setBackgroundBorderProperties(mView, themeClass, BackgroundOptions.defaultFor(mLayoutItem));
	}

	public void updateLayoutParams(ViewGroup.LayoutParams params)
	{
		// Does its site support margins and we deferred the assignment? If so, do it now.
		if (mDeferredMargins != null)
		{
			MarginLayoutParams marginParams = Cast.as(MarginLayoutParams.class, params);
			if (marginParams != null)
				marginParams.setMargins(mDeferredMargins.left, mDeferredMargins.top, mDeferredMargins.right, mDeferredMargins.bottom);
		}
	}
}
