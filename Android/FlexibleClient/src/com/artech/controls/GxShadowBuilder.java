package com.artech.controls;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.View.DragShadowBuilder;

import com.artech.base.services.Services;

@SuppressLint("NewApi")
public class GxShadowBuilder extends DragShadowBuilder
{
	private final Point mTouchPoint;
	private final static int SHADOW_VERTICAL_OFFSET = Services.Device.dipsToPixels(10);

	public GxShadowBuilder(@NonNull View v, @Nullable Point p)
	{
		super(v);
		mTouchPoint = p;
	}

	@Override
	public void onProvideShadowMetrics(@NonNull Point shadowSize, @NonNull Point shadowTouchPoint)
	{
		super.onProvideShadowMetrics(shadowSize, shadowTouchPoint);

		if (mTouchPoint != null)
			shadowTouchPoint.set(mTouchPoint.x, mTouchPoint.y + SHADOW_VERTICAL_OFFSET);
	}
}