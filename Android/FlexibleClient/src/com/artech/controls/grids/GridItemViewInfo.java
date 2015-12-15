package com.artech.controls.grids;

import java.util.ArrayList;
import java.util.List;

import android.view.View;
import android.widget.CheckBox;

import com.artech.R;
import com.artech.android.layout.GxLayout;
import com.artech.base.model.Entity;
import com.artech.controls.GxTextView;
import com.artech.ui.GridItemCoordinator;
import com.artech.utils.Cast;

public class GridItemViewInfo
{
	private GridItemLayout mView;
	private ArrayList<View> mBoundViews = new ArrayList<View>();
	private GridItemLayoutVersion mLayoutVersion;

	private Entity mData;
	private int mPosition;
	private GridItemCoordinator mCoordinator;
	private GxLayout mHolder;
	private GxTextView mHeaderText;
	private CheckBox mCheckbox;

	public GridItemViewInfo(GridItemLayout view, GridItemLayoutVersion version, List<View> boundViews, GxLayout holder)
	{
		mView = view;
		mLayoutVersion = version;
		mBoundViews.addAll(boundViews);
		mHolder = holder;

		mHeaderText = (GxTextView)view.findViewById(R.id.grid_item_header_text);
		mCheckbox = (CheckBox)view.findViewById(R.id.grid_item_checkbox);
	}

	public void setCoordinator(GridItemCoordinator coordinator) { mCoordinator = coordinator; }

	public GridItemLayout getView() { return mView; }
	public List<View> getBoundViews() { return mBoundViews; }
	public GridItemLayoutVersion getVersion() { return mLayoutVersion; }

	public int getPosition() { return mPosition; }
	public Entity getData() { return mData; }

	public GxLayout getHolder() { return mHolder; }
	public GxTextView getHeaderText() { return mHeaderText; }
	public CheckBox getCheckbox() { return mCheckbox; }

	static GridItemViewInfo fromView(View view)
	{
		if (view == null)
			return null;

		return Cast.as(GridItemViewInfo.class, view.getTag(R.id.tag_grid_item_info));
	}

	void assignTo(View view)
	{
		view.setTag(R.id.tag_grid_item_info, this);
	}

	static void discard(View view)
	{
		view.setTag(R.id.tag_grid_item_info, null);
	}

	void setData(View grid, int position, Entity data)
	{
		setData(position, data);
		mView.setData(this, grid, position, data);
	}

	void setData(int position, Entity data)
	{
		mData = data;
		mPosition = position;

		if (mCoordinator != null)
			mCoordinator.setData(data);

		// mView?
	}
}
