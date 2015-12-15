package com.artech.ui;

import java.lang.ref.WeakReference;

import android.graphics.Rect;
import android.view.View;

public class Anchor
{
	private final WeakReference<View> mView;
	private final Rect mRect;

	public Anchor(View view)
	{
		mView = new WeakReference<View>(view);
		mRect = getRectOnScreen(view);
	}

	/***
	 * Return the screen area for the given view.
	 * @param v The View
	 * @return the rectangle area. Take into account that can be empty if the control is not on screen yet.
	 */
	private static Rect getRectOnScreen(View v)
	{
		try
		{
			Rect hitRect = new Rect();
			v.getHitRect(hitRect);
			int[] xyPoint = new int[2];
			v.getLocationOnScreen(xyPoint);

			Rect outRect = new Rect();
			outRect.left = xyPoint[0];
			outRect.top = xyPoint[1];
			outRect.right = outRect.left + hitRect.width();
			outRect.bottom = outRect.top + hitRect.height();
			return outRect;
		}
		catch (Exception ex)
		{
			return null;
		}
	}

	/**
	 * Returns the view that marks the anchor.
	 * Can be null if the View was removed/destroyed since this object was created.
	 */
	public View getView()
	{
		return mView.get();
	}

	/**
	 * Return the screen area for the given view.
	 */
	public Rect getRect()
	{
		return mRect;
	}
}
