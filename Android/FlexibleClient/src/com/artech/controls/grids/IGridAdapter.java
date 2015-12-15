package com.artech.controls.grids;

import com.artech.base.model.Entity;
import com.artech.controllers.ViewData;
import com.fedorvlasov.lazylist.ImageLoader;

public interface IGridAdapter
{
	ViewData getData();
	Entity getEntity(int position);
	ImageLoader getImageLoader();
}
