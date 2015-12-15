package com.artech.adapters;

import android.content.Context;
import android.support.v4.view.PagerAdapter;

import com.artech.activities.IGxBaseActivity;
import com.artech.utils.Cast;
import com.fedorvlasov.lazylist.ImageLoader;

public abstract class GxPagerAdapter extends PagerAdapter
{
	private ImageLoader mImageLoader;
	private Context mContext;
	
	public GxPagerAdapter(Context context) {
		mContext = context;
	}
	
	public ImageLoader getImageLoader() {
		if (mImageLoader == null) {
			IGxBaseActivity activity = Cast.as(IGxBaseActivity.class, mContext);
			if (activity != null)
				mImageLoader = activity.getImageLoader();
			else
				mImageLoader =  new ImageLoader(mContext);
		}
		return mImageLoader;
	}
	
	public void onDestroy() {
		mImageLoader = null;
	}
	
	@Override
	public int getItemPosition(Object object)
	{
		// Return POSITION_NONE so that the view is recreated by ViewPager (after updating data and calling notifyDataSetChanged()).
		return POSITION_NONE;
	}
}
