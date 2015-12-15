package com.artech.controls.magazineviewer;

import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.artech.base.metadata.layout.Size;

public class FlipperAdapter extends PagerAdapter
{
	private final IFlipDataSource _dataSource;
	private final Size _size;

	public FlipperAdapter(IFlipDataSource ds, Size mSize)
	{
		_dataSource = ds;
		_size = mSize;
	}

	@Override
	public void notifyDataSetChanged()
	{
		_dataSource.resetNumberOfPages();
		super.notifyDataSetChanged();
	}

	@Override
	public int getCount()
	{
		return _dataSource.getNumberOfPages();
	}

	@Override
	public int getItemPosition(Object object)
	{
		// Return POSITION_NONE so that the view is recreated by ViewPager (after updating data and calling notifyDataSetChanged()).
		return POSITION_NONE;
	}

	@Override
	public void startUpdate(View arg0) { }

	@Override
	public Object instantiateItem(View container, int position)
	{
		View itemView = _dataSource.getViewForPage(position, _size);
		((ViewPager)container).addView(itemView, 0);
		return itemView;
	}

	@Override
	public boolean isViewFromObject(View view, Object object)
	{
	 	return view == ((View)object);
	}

	@Override
	public void destroyItem(View container, int idx, Object view)
	{
		((ViewPager)container).removeView((View) view);
		_dataSource.destroyPageView(idx, (View)view);
	}

	@Override
	public void finishUpdate(View collection) { }

	@Override
	public void restoreState(Parcelable arg0, ClassLoader arg1) { }

	@Override
	public Parcelable saveState() { return null; }
}
