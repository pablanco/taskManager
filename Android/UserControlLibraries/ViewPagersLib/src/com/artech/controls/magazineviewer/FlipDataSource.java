package com.artech.controls.magazineviewer;

import java.util.ArrayList;
import java.util.Hashtable;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.artech.base.metadata.layout.Size;
import com.artech.controls.grids.GridAdapter;
import com.artech.controls.grids.GridHelper;
import com.artech.utils.Cast;

/* This implements a DataSource taking a common adapter and returning the pages depending on the options */
public class FlipDataSource implements IFlipDataSource
{
	private FlipperOptions _options;
	private final GridAdapter _adapter;
	private int _numberOfPages = -1;
	private Context _context;
	private ViewProvider _viewProvider;
	private Hashtable<Integer, ArrayList<Integer>> _layouts = new Hashtable<Integer, ArrayList<Integer>>();
	private final GridHelper mHelper;

	public FlipDataSource(Context context, FlipperOptions options, GridAdapter adapter, GridHelper helper)
	{
		_options = options;
		_adapter = adapter;
		_context = context;
		_viewProvider = new ViewProvider(adapter);
		mHelper = helper;
	}

	@Override
	public int getNumberOfPages() {
		if (_numberOfPages <= 0) {
			_numberOfPages = 0;
			int totalItems = _adapter.getCount();
			if (totalItems > 0) {
				_numberOfPages = totalItems / _options.getItemsPerPage();
				if (totalItems % _options.getItemsPerPage() != 0) {
					_numberOfPages++;
				}
			}
		}
		return _numberOfPages;
	}

	@Override
	public void resetNumberOfPages()
	{
		_numberOfPages = -1;
	}

	@Override
	public View getViewForPage(int page, Size size) {
		if (page >= 0) {
			ArrayList<Integer> layout;
			if (_layouts.containsKey(page))
				layout = _layouts.get(page);
			else {
				layout = _options.getLayout();
				_layouts.put(page, layout);
			}
			int nextView = page * _options.getItemsPerPage();
			PageLayoutView view = new PageLayoutView(_context,_options.getRowsPerColumn(), layout, _viewProvider, size, _adapter, nextView, mHelper);
			view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			return view;
		}
		return null;
	}

	@Override
	public void destroyPageView(int page, View pageView)
	{
		PageLayoutView pageLayoutView = Cast.as(PageLayoutView.class, pageView);
		if (pageLayoutView != null)
		{
			for (View pageItemView : pageLayoutView.getPageItems())
				mHelper.discardItemView(pageItemView);
		}
	}
}
