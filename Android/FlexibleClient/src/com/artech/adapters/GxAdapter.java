package com.artech.adapters;

import android.content.Context;
import android.widget.BaseAdapter;

import com.artech.activities.IGxBaseActivity;
import com.artech.utils.Cast;
import com.fedorvlasov.lazylist.ImageLoader;

public abstract class GxAdapter extends BaseAdapter {

	private ImageLoader mImageLoader;
	protected Context mContext;
	
	public GxAdapter(Context context) {
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
}
