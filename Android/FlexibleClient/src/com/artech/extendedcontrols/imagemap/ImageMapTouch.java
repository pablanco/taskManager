package com.artech.extendedcontrols.imagemap;

import android.content.Context;
import android.util.AttributeSet;

import com.artech.extendedcontrols.image.ImageViewTouch;

public class ImageMapTouch extends ImageViewTouch {

	private OnImageZoomedListener mZoomListener;

	public ImageMapTouch(Context context, AttributeSet attrs) {
		super(context, attrs);


	}

	public void addZoomListener(OnImageZoomedListener listener){
		mZoomListener = listener;
	}


	@Override
	protected void panBy(double dx, double dy) {
		super.panBy(dx, dy);

		if (mZoomListener != null)
			mZoomListener.panBy(getScrollRect().left, getScrollRect().top);
	}


	@Override
	protected void onLayout( boolean changed, int left, int top, int right, int bottom ) {
		super.onLayout(changed, left, top, right, bottom);

		if (mZoomListener != null)
			mZoomListener.bitmapLoaded();
	}

	@Override
	protected void zoomTo( float scale, float centerX, float centerY ){
		super.zoomTo(scale, centerX, centerY);

		if (mZoomListener != null)
			mZoomListener.zoom(scale);
	}

	public interface OnImageZoomedListener {
		void panBy(double dx, double dy);
		void zoom(float scale);
		void bitmapLoaded();
	}
}

