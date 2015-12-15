package com.artech.android.layout;

import android.content.Context;
import android.content.ContextWrapper;

import com.artech.base.model.Entity;
import com.artech.ui.Coordinator;
import com.fedorvlasov.lazylist.ImageLoader;

public class GridContext extends ContextWrapper
{
	private ImageLoader mImageLoader;
	private Coordinator mCoordinator;
	private Entity mSelection;
	
	public GridContext(Coordinator coordinator, Context context, ImageLoader loader) 
	{
		super(context);
		mImageLoader = loader;
		setCoordinator(coordinator);
	}
	
	public ImageLoader getImageLoader()
	{
		return mImageLoader;
	}

	public Coordinator getCoordinator() {
		return mCoordinator;
	}
	
	public Entity getSelection() {
		return mSelection;
	}
	public void setCoordinator(Coordinator coordinator) {
		this.mCoordinator = coordinator;
	}
	
	public void setSelection(Entity entity) {
		mSelection = entity;
	}
}
