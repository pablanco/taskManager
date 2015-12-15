package com.artech.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

public class GxRatingView extends GridView {
	
	private Context mContext;
	private int mNumStars = 5;
	private float mStarShow = 0;
	private float mStepSize = 1;
	private boolean mIsUserSeekable = false;
	private GxRatingAdapter mAdapter;
	
	public GxRatingView(Context context) {
		super(context);
		mContext = context;
	}
	
	public GxRatingView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}
	
	public void generateGxRating(float size) {
		mAdapter = new GxRatingAdapter(mContext, mNumStars, mStarShow, size);
		setAdapter(mAdapter);
		mAdapter.notifyDataSetChanged();
	}
	
	/**
     * Whether this rating bar should only be an indicator (thus non-changeable
     * by the user).
     * 
     * @param isIndicator Whether it should be an indicator.
     */
    public void setIsIndicator(boolean isIndicator) {
        mIsUserSeekable = !isIndicator;
        setFocusable(!isIndicator);
    }
    
    /**
     * @return Whether this rating bar is only an indicator.
     */
    public boolean isIndicator() {
        return !mIsUserSeekable;
    }
    
    /**
     * Sets the number of stars to show. In order for these to be shown
     * properly, it is recommended the layout width of this widget be wrap
     * content.
     * 
     * @param numStars The number of stars.
     */
    public void setNumStars(final int numStars) {
        if (numStars <= 0) {
            return;
        }        
        mNumStars = numStars;
        setNumColumns(mNumStars);
        if (mAdapter!=null) {
    		mAdapter.setNumStars(numStars);
    		mAdapter.notifyDataSetChanged();
    	}
    }

    /**
     * Returns the number of stars shown.
     * @return The number of stars shown.
     */
    public int getNumStars() {
        return mNumStars;
    }
    
    /**
     * Sets the rating (the number of stars filled).
     * 
     * @param rating The rating to set.
     */
    public void setRating(float rating) {
    	mStarShow = rating;
    	if (mAdapter!=null) {
    		mAdapter.setRating(rating);
    		mAdapter.notifyDataSetChanged();
    	}
    }

    /**
     * Gets the current rating (number of stars filled).
     * 
     * @return The current rating.
     */
    public float getRating() {
    	return mStarShow;
    }

    /**
     * Sets the step size (granularity) of this rating bar.
     * 
     * @param stepSize The step size of this rating bar. 
     */
    public void setStepSize(float stepSize) {
        if (stepSize <= 0) {
            return;
        }
        mStepSize = stepSize;
    }

    /**
     * Gets the step size of this rating bar.
     * 
     * @return The step size.
     */
    public float getStepSize() {
        return mStepSize;
    }

    public void setMax(int max) {
        if (max <= 0) {
            return;
        }
        mNumStars = max;
    }

    @Override
    public void setEnabled(boolean enabled) {
    	super.setEnabled(enabled);
    	if (mAdapter!=null) {
    		mAdapter.setEnabled(enabled);
    		mAdapter.notifyDataSetChanged();
    	}
    }
    
}
