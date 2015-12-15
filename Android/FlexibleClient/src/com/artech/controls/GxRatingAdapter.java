package com.artech.controls;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;

public class GxRatingAdapter extends BaseAdapter {

	private Context mContext;
	private int mNumStars = 0;
	private int mStarsShow = 0;
	private float mSizeStars;
	private boolean mEnabledStar = true;

	public GxRatingAdapter(Context context, int numStars, float starsShow, float sizeStars) {
		mContext = context;
		mNumStars = numStars;
		mSizeStars = sizeStars;
		mStarsShow = (int) starsShow;
	}

	@Override
	public int getCount() {
		return mNumStars;
	}

	@Override
	public Object getItem(int arg0) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		boolean isSelected = (position < mStarsShow);
		GxStarView star = new GxStarView(mContext, mSizeStars, isSelected, mEnabledStar);
		star.setLayoutParams(new GridView.LayoutParams((int)mSizeStars + 1, (int)mSizeStars + 1));
		return star;
	}

	public void setRating(float rating) {
		mStarsShow = (int) rating;
	}

	public void setNumStars(int numStars) {
		mNumStars = numStars;
	}

	public void setEnabled(boolean enabled) {
		mEnabledStar = enabled;
	}
}
