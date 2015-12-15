package com.artech.controls.magazineviewer;

import java.util.ArrayList;
import java.util.LinkedList;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.AbsoluteLayout;

import com.artech.android.layout.GridContext;
import com.artech.base.metadata.layout.Size;
import com.artech.controls.GxGradientDrawable;
import com.artech.controls.grids.GridAdapter;
import com.artech.controls.grids.GridHelper;

@SuppressWarnings("deprecation")
public class PageLayoutView extends AbsoluteLayout {

	private IViewProvider _views;
	private ArrayList<Integer> _layout;
	private GridAdapter _gridAdapter;
	private GridHelper _helper;
	private int mRowsPerColumn;
	private GridContext _Context;

	public PageLayoutView(Context context, int rowsPerColumn, ArrayList<Integer> layout, IViewProvider viewProvider, Size size, GridAdapter adapter, int initialView, GridHelper mHelper)
	{
		super(context);
		_Context = (GridContext) context;
		_views = viewProvider;
		_layout = layout;
		_gridAdapter = adapter;
		_helper = mHelper;
		int headerHeight = 0;
		int columns = _layout.size();
		int availableHeight = size.getHeight();
		int wScreen = size.getWidth();
		int nextView = initialView;
		int currentX = 0;

		int cellWidth = wScreen / columns;
		mRowsPerColumn = rowsPerColumn;

		if (mRowsPerColumn > 0) {
			int currentY = headerHeight;
			int rows = mRowsPerColumn;
			for (int r = 0; r < rows; r++) {
				currentX = 0;
				for (int c = 0; c < columns; c++) {
					if (nextView >= _views.size())
						break;
					addItemView(nextView, cellWidth, availableHeight / rows, currentX, currentY);
					currentX += cellWidth;
					nextView++;
				}
				currentY += availableHeight / rows;
			}
		} else {
			for (int c = 0; c < columns; c++) {
				int currentY = headerHeight;
				int rows = _layout.get(c);
				for (int r = 0; r < rows ; r++)
				{
					if (nextView >= _views.size())
						break;

					addItemView(nextView, cellWidth, availableHeight / rows, currentX, currentY);
					currentY += availableHeight / rows;
					nextView++;
				}
				currentX += cellWidth;
			}
		}
	}

	private void addItemView(int viewId, int width, int height, int x, int y) {
		View view = _views.getView(viewId, width, height);
		GxGradientDrawable back = new GxGradientDrawable();
		back.setColor(Color.TRANSPARENT);
		//remove stroke from magazine viewer
		//back.setStroke(1, Color.LTGRAY);
		view.setBackgroundDrawable(back);
		view.setTag(viewId);
		view.setOnClickListener(onClickView);
		addView(view, new AbsoluteLayout.LayoutParams(width, height, x, y));

	}

	OnClickListener onClickView = new OnClickListener(){
		@Override
		public void onClick(View v) {
			_gridAdapter.selectIndex((Integer) v.getTag(), _Context, _helper, true);
			_gridAdapter.runDefaultAction( (Integer) v.getTag());
		}
	};

	public void setRowsPerColumn(int rowsPerColumn)
	{
		mRowsPerColumn = rowsPerColumn;
	}

	public Iterable<View> getPageItems()
	{
		// Should be kept consistent with addItemView().
		// For example, if an intermediate container view is added there, it should be ignored here.
		LinkedList<View> pageItems = new LinkedList<View>();
		for (int i = 0; i < getChildCount(); i++)
			pageItems.add(getChildAt(i));

		return pageItems;
	}
}
