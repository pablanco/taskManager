package com.artech.controls.magazineviewer;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

public class FlipperView extends ViewPager {

	public FlipperView(Context context) {
		super(context);
	}
	
	public FlipperView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
	}


	public FlipperAdapter getFlipperAdapter() {
		return (FlipperAdapter) getAdapter();
	}
}
