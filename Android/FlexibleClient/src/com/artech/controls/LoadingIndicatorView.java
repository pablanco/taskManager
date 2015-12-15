package com.artech.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.artech.base.services.Services;

/**
 * View for a "Loading Indicator" inside a layout. Follows the "Activity Circle" guidelines.
 * See http://developer.android.com/design/building-blocks/progress.html
 * @author matiash
 */
public class LoadingIndicatorView extends RelativeLayout
{
	private ProgressBar mCircle;
	private GxTextBlockTextView mTextView;

	public LoadingIndicatorView(Context context)
	{
		super(context);
		initialize();
	}

    public LoadingIndicatorView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize();
    }

    public LoadingIndicatorView(Context context, AttributeSet attrs)
    {
    	super(context, attrs);
    	initialize();
    }

    private void initialize()
    {
		mCircle = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleLarge);
		mCircle.setIndeterminate(true);
		mTextView = new GxTextBlockTextView(getContext());
		mTextView.setVisibility(View.GONE);

		addView(mCircle, getCenteredLayoutParams());
		addView(mTextView, getCenteredLayoutParams());
    }

	private LayoutParams getCenteredLayoutParams()
	{
		LayoutParams centered = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		centered.addRule(RelativeLayout.CENTER_IN_PARENT);

		if (!isInEditMode())
		{
			int pixelMargin = Services.Device.dipsToPixels(10);
			centered.topMargin = pixelMargin;
			centered.bottomMargin = pixelMargin;
			centered.leftMargin = pixelMargin;
			centered.rightMargin = pixelMargin;
		}

		return centered;
	}

	public void setCircleStyle(int style)
	{
		if (mCircle != null)
			removeView(mCircle);

		mCircle = new ProgressBar(getContext(), null, style);
		mCircle.setIndeterminate(true);
		addView(mCircle, getCenteredLayoutParams());
	}

	public void setText(CharSequence text)
	{
		mCircle.setVisibility(View.GONE);
		mTextView.setVisibility(View.VISIBLE);
		mTextView.setText(text);
	}
}
