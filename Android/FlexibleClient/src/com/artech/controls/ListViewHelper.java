package com.artech.controls;

import android.widget.ListView;

import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.services.Services;
import com.artech.controls.grids.GridHelper;

public class ListViewHelper extends GridHelper
{
	private final ListView mListView;
    private LoadingIndicatorView mFooterView = null;

    private int mInsideOnMeasure;

    public ListViewHelper(ListView listview, GridDefinition definition)
    {
    	super(listview, definition);
    	mListView = listview;
    }

	public void showFooter(boolean showLoading, String errorMessage)
	{
		if (showLoading)
		{
			mListView.removeFooterView(mFooterView);

			mFooterView = new LoadingIndicatorView(mListView.getContext());
			mFooterView.setCircleStyle(android.R.attr.progressBarStyle);
			mListView.addFooterView(mFooterView, null, false);
		}
		else if (Services.Strings.hasValue(errorMessage))
		{
			mListView.removeFooterView(mFooterView);

			mFooterView = new LoadingIndicatorView(mListView.getContext());
			mFooterView.setText(errorMessage);
			mListView.addFooterView(mFooterView, null, false);
		}
		else
			mListView.removeFooterView(mFooterView);
	}

	public void onScroll()
	{
		if (isFooterViewVisible())
			requestMoreData();
	}

	private boolean isFooterViewVisible()
	{
	  	return (mFooterView != null && mFooterView.isShown());
	}

	public void beginOnMeasure()
	{
		mInsideOnMeasure++;
	}

	public void endOnMeasure()
	{
		mInsideOnMeasure--;
	}

	@Override
	protected boolean isMeasuring()
	{
		return (mInsideOnMeasure > 0);
	}
}
