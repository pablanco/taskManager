package com.artech.controls.viewpager;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;

import com.artech.adapters.GxPagerAdapter;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.controllers.ViewData;
import com.artech.controls.grids.GridHelper;
import com.artech.controls.grids.GridItemViewInfo;
import com.artech.controls.grids.IGridAdapter;

class GxViewPagerAdapter extends GxPagerAdapter implements IGridAdapter
{
	private GxViewPager mViewPager;
	private GridHelper mHelper;
	private ViewData mViewData;
	private EntityList mData;

	GxViewPagerAdapter(Context context, GxViewPager viewPager, GridHelper helper)
	{
		super(context);
		mViewPager = viewPager;
		mHelper = helper;
	}

	@Override
	public ViewData getData() { return mViewData; }

	@Override
	public int getCount()
	{
		return (mData != null ? mData.size() : 0);
	}

	@Override
	public Entity getEntity(int position)
	{
		return (mData != null ? mData.get(position) : null);
	}

	public void setData(ViewData data)
	{
		mViewData = data;
		mData = data.getEntities();
		notifyDataSetChanged();
	}

	public void setCurrentItem(int position)
	{
		if (mData != null && position >= 0 && position < mData.size())
			mData.setCurrentEntity(mData.get(position));
	}
	
	@Override
	public Object instantiateItem(View collection, int position)
	{
		GridItemViewInfo itemView = mHelper.getItemView(this, position, null, false);
		View view = itemView.getView();
		//add the view to result
		((ViewPager)collection).addView(view, 0);
		view.setOnClickListener(mOnItemClickListener);
		return view;
	}

	private OnClickListener mOnItemClickListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			mViewPager.onItemClick(v);
		}
	};

	@Override
	public void destroyItem(View collection, int position, Object view)
	{
		View itemView = (View)view;
		mHelper.discardItemView(itemView);
		((ViewPager)collection).removeView(itemView);
	}

	@Override
	public boolean isViewFromObject(View view, Object object)
	{
		return view == ((View)object);
	}

	@Override
	public void restoreState(Parcelable arg0, ClassLoader arg1) { }

	@Override
	public Parcelable saveState() { return null; }

	@Override
	public void startUpdate(View view) { }

	@Override
	public void finishUpdate(View view) { }
}
