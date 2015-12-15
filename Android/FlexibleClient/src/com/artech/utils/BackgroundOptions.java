package com.artech.utils;

import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.controls.GxTouchEvents;

public class BackgroundOptions
{
	private boolean mIsHighlighted = false;
	private boolean mUseBitmapSize = false;
	private LayoutItemDefinition mLayoutItem;

	public static final BackgroundOptions DEFAULT = new BackgroundOptions();

	public static BackgroundOptions defaultFor(LayoutItemDefinition layoutItem)
	{
		BackgroundOptions options = new BackgroundOptions();
		options.mLayoutItem = layoutItem;
		return options;
	}

	public static BackgroundOptions copy(BackgroundOptions options)
	{
		BackgroundOptions copy = new BackgroundOptions();
		copy.mIsHighlighted = options.mIsHighlighted;
		copy.mUseBitmapSize = options.mUseBitmapSize;
		copy.mLayoutItem = options.mLayoutItem;

		return copy;
	}

	public boolean getIsHighlighted() { return mIsHighlighted; }
	public BackgroundOptions setIsHighlighted(boolean value) { mIsHighlighted = value; return this; }

	public boolean getUseBitmapSize() { return mUseBitmapSize; }
	public BackgroundOptions setUseBitmapSize(boolean value) { mUseBitmapSize = value; return this; }

	public LayoutItemDefinition getLayoutItem() { return mLayoutItem; }

	/**
	 * Returns whether the associated control is "actionable" (i.e. tappable,
	 * by having TAP, LONG_TAP or DOUBLE_TAP events).
	 */
	public boolean isActionableControl()
	{
		if (mLayoutItem != null)
		{
			for (String tapEvent : GxTouchEvents.TAP_EVENTS)
			{
				if (mLayoutItem.getEventHandler(tapEvent) != null)
					return true;
			}
		}

		return false;
	}
}
