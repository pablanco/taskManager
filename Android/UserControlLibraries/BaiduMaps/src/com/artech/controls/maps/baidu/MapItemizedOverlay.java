package com.artech.controls.maps.baidu;

import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;

import com.artech.base.model.Entity;
import com.artech.controls.TransparentPanel;
import com.artech.controls.grids.GridAdapter;
import com.baidu.mapapi.map.ItemizedOverlay;
import com.baidu.mapapi.map.MapView;
import com.baidu.platform.comapi.basestruct.GeoPoint;
import com.baidu.platform.comapi.map.Projection;

public class MapItemizedOverlay extends ItemizedOverlay<MapOverlayItem>
{
	private BaiduMapView mParent;
	private Entity mCurrentEntityShown;

	public MapItemizedOverlay(BaiduMapView parent, Drawable marker)
	{
		super(marker, parent);
		mParent = parent;
	}

	@Override
	public boolean onTap(GeoPoint point, MapView mapView)
	{
		if (view != null)
			mParent.removeView(callOut);

		return false;
	}

	private View view;
	private TransparentPanel callOut;
	private GridAdapter mAdapter;

	@Override
	protected boolean onTap(int index)
	{
		if (callOut == null)
			callOut = new TransparentPanel(mParent.getContext());

		if (view != null)
			mParent.removeView(callOut);

		MapOverlayItem selectedItem = (MapOverlayItem)getItem(index);

		if (selectedItem.hasDetail() && !mAdapter.isItemViewEmpty(selectedItem.getEntity()))
		{
			view = mAdapter.getView(selectedItem.getId(), null, null);

			callOut.removeAllViews();
			callOut.addView(view);
			callOut.setVisibility(View.VISIBLE);

			mCurrentEntityShown = selectedItem.getEntity();

			view.setOnClickListener(myEditClickListener);
			Point point = new Point();
			Projection projection= mParent.getProjection();
			point = projection.toPixels(selectedItem.getPoint(), point);
			point.y -= 40;
			GeoPoint ballonPoint = projection.fromPixels(point.x, point.y);
			final MapView.LayoutParams lp = new MapView.LayoutParams(MapView.LayoutParams.WRAP_CONTENT,MapView.LayoutParams.WRAP_CONTENT, ballonPoint,MapView.LayoutParams.BOTTOM_CENTER);
			view.setVisibility(View.VISIBLE);
			mParent.getController().animateTo(selectedItem.getPoint());
			mParent.addView(callOut, lp);
			mParent.setVisibility(View.VISIBLE);
			mParent.requestLayout();
		}
		return true;
	}

	private OnClickListener myEditClickListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			mParent.getHelper().runDefaultAction(mCurrentEntityShown);
		}
	};

	public void setAdapter(GridAdapter adapter)
	{
		mAdapter = adapter;
	}
}
