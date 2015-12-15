package com.artech.controls.magazineviewer;

import android.view.View;

import com.artech.base.metadata.layout.Size;

public interface IFlipDataSource
{
	int getNumberOfPages();
	void resetNumberOfPages();

	View getViewForPage(int page, Size size);
	void destroyPageView(int page, View pageView);
}
