package com.artech.controls;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

class GxStarView extends View
{
	private final GxRatingHelper mDrawHelper;

	public GxStarView(Context context, float size, boolean selected, boolean enabled)
	{
		super(context);
		mDrawHelper = new GxRatingHelper(size, selected, enabled, true);
	}

	@Override
    public void onDraw(Canvas canvas)
	{
		mDrawHelper.onDraw(canvas);
    }
}
