package com.artech.extendedcontrols.imagemap;

import java.util.Hashtable;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsoluteLayout;

import com.artech.R;
import com.artech.android.layout.GridContext;
import com.artech.base.controls.IGxControlRuntime;
import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.services.AndroidContext;
import com.artech.base.services.Services;
import com.artech.common.ImageHelper;
import com.artech.controllers.ViewData;
import com.artech.controls.IGridView;
import com.artech.controls.ImageViewDisplayImageWrapper;
import com.artech.controls.grids.GridAdapter;
import com.artech.controls.grids.GridHelper;
import com.artech.controls.grids.GridItemLayout;
import com.artech.controls.grids.GridItemViewInfo;
import com.artech.extendedcontrols.image.ImageViewTouch;
import com.artech.ui.Coordinator;
import com.fedorvlasov.lazylist.ImageLoader;
import com.nineoldandroids.view.ViewHelper;

@SuppressWarnings("deprecation")
public class GxImageMap extends AbsoluteLayout implements IGridView , IGxControlRuntime{

	private GridHelper mHelper;
	private GridAdapter mAdapter;
	private GxImageMapDefinition mDefinition;
	private Hashtable<List<String>, View> mItemsViews = new Hashtable<List<String>, View>();

	private ImageMapTouch mImageMap;

	private EntityList mEntities;
	private GxImageMapGestureListener mGestureManager;

	protected boolean mBitmapLoaded = false;
	protected boolean mDataArrived = false;

	// temporary
	private ImageLoader mLoader = null;

	//not used
	public GxImageMap(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public GxImageMap(Context context, Coordinator coor, LayoutItemDefinition def) {
		super(context);
		// get image loader
		if (context instanceof GridContext)
		{
			GridContext gridContext = (GridContext)context;
			mLoader = gridContext.getImageLoader();
		}
		setLayoutDefinition(def);
	}

	public GxImageMap(Activity activity, LayoutItemDefinition def) {
		super(activity);
		setLayoutDefinition(def);
	}

	private void setLayoutDefinition(LayoutItemDefinition layoutItemDefinition) {
		init((GridDefinition) layoutItemDefinition);
	}

	private void init(GridDefinition gridDefinition) {
		mDefinition = new GxImageMapDefinition(getContext(), gridDefinition);
		mHelper = new GridHelper(this, gridDefinition);

		mImageMap = new ImageMapTouch(getContext(), null);
		mImageMap.setMaxZoom(5f); // 5???
		mImageMap.setMinZoom(1f);

		mGestureManager = new GxImageMapGestureListener(this);
		mImageMap.addZoomListener(mGestureManager);

		GxImageMapTapListener tapListener = new GxImageMapTapListener(this);
		mImageMap.setTapListener(tapListener);

		addView(mImageMap, new AbsoluteLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 0, 0));

		ImageHelper.displayImage(ImageViewDisplayImageWrapper.to(mImageMap), mDefinition.getmImage());
		//new LoadBackgroundImage().execute();
	}

	protected Hashtable<List<String>, View> getItemsViews(){
		return mItemsViews;
	}

	protected GridHelper getHelper(){
		return mHelper;
	}

	protected ImageMapTouch getControl(){
		return mImageMap;
	}

	private void prepareAdapter() {
		if (mAdapter == null)
			mAdapter = new GridAdapter(getContext(), mHelper, mDefinition.getGrid());
	}



	@Override
	public void addListener(GridEventsListener listener) {
		mHelper.setListener(listener);
	}

	@Override
	public void update(ViewData data) {
		prepareAdapter();
		mAdapter.setData(data);
		mEntities = data.getEntities();

		mDataArrived = true;
		if (mBitmapLoaded)
			updateMap();
	}

	public void updateMap() {
		updateMap(mEntities);
	}

	private void updateMap(EntityList data) 
	{
		Services.Log.debug("GxImageMap updateMap with data");
		int id = 0;
		RectF bmpRect = mImageMap.getBitmapRect();

		float currentFactor = mImageMap.getCurrentScaleFactor();

		int rowcount = getChildCount();
		if(rowcount > 1)
			removeViews(1,rowcount-1);

		int resourceId = Services.Resources.getImageResourceId(mDefinition.getmImage());

		BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    BitmapFactory.decodeResource(getResources(), resourceId, options);

	    float densityRatio = (float)options.inTargetDensity / (float)options.inDensity;

		for (Entity entity : data) {

			float x = Float.valueOf(entity.optStringProperty(mDefinition.getHCoordinateAttribute())) * densityRatio;
			float y = Float.valueOf(entity.optStringProperty(mDefinition.getVCoordinateAttribute())) * densityRatio;
			float s = Float.valueOf(entity.optStringProperty(mDefinition.getSizeAttribute())) * densityRatio;

			float scaledUnzoomedS = getScaledDimenssion(s);
			float scaledUnzoomedX = getScaledDimenssion(x);
			float scaledUnzoomedY = getScaledDimenssion(y);

			float scaledX = (bmpRect!=null)?bmpRect.left:0 + (scaledUnzoomedX * currentFactor);
			float scaledY = (bmpRect!=null)?bmpRect.top:0 +  (scaledUnzoomedY * currentFactor);
			
			mAdapter.setBounds((int) s, (int) s);

			View previousView = null;
			//if (mItemsViews.containsKey(entity.getKey())){
			//	previousView = mItemsViews.get(entity.getKey());
			//	mHelper.discardItemView(previousView);
			//}

			GridItemViewInfo rowlayoutinfo = mHelper.getItemView(mAdapter, id, previousView, false);
			GridItemLayout rowlayout = rowlayoutinfo.getView();


			ViewHelper.setPivotX(rowlayout, 0);
			ViewHelper.setPivotY(rowlayout, 0);

			if (previousView == null){
				RectF original = new RectF(scaledUnzoomedX, scaledUnzoomedY, scaledUnzoomedS, scaledUnzoomedS);

				rowlayout.setTag(R.id.tag_imagemap_item_origin, original);
				rowlayout.setTag(R.id.tag_imagemap_item_entity, entity);
			}

			//rowlayout.setBackgroundColor(0xFFFF0000);
			rowlayout.setLayoutParams(new AbsoluteLayout.LayoutParams((int) s, (int) s, (int) scaledX, (int) scaledY));

			if (previousView == null)
				addView(rowlayout);

			scaleView(rowlayout, currentFactor);
			id++;
		}
		invalidate();
	}

	private float getScaledDimenssion(float original) {
		return original * mImageMap.getScaleRatio();
	}

	protected void scaleView(View view, float scale){

		ViewHelper.setScaleX(view, scale * mImageMap.getScaleRatio());
		ViewHelper.setScaleY(view, scale * mImageMap.getScaleRatio());

	}

	
	private class GxImageMapGestureListener implements ImageMapTouch.OnImageZoomedListener {

		GxImageMap mView;

		public GxImageMapGestureListener(GxImageMap view) {
			mView = view;
		}

		@Override
		public void zoom(float scale) {

			int count = mView.getChildCount();
			if (count == 1) // 1 is the map image
				return;

			for (int i = 1; i < count; i++) {
				View v = mView.getChildAt(i);

				mView.scaleView(v, scale);

				RectF original = (RectF) v.getTag(R.id.tag_imagemap_item_origin);
				RectF bmpRect = mImageMap.getBitmapRect();

				float newX = bmpRect.left + (original.left * scale);
				float newY = bmpRect.top + (original.top * scale);

				ViewHelper.setX(v, newX);
				ViewHelper.setY(v, newY);
			}
		}

		@Override
		public void panBy(double dx, double dy) {
			int count = mView.getChildCount();
			if (count == 1) // 1 is the map image
				return;

			for (int i = 1; i < count; i++) {
				View v = mView.getChildAt(i);

				ViewHelper.setX(v, ViewHelper.getX(v)+ (float)dx);
				ViewHelper.setY(v, ViewHelper.getY(v)+ (float)dy);
			}
		}

		boolean mFirstLoad = true;

		@Override
		public void bitmapLoaded() {

			if (mFirstLoad){
				mView.mBitmapLoaded = true;
				if (mView.mDataArrived ){
					post(new Runnable() {
                        @Override
                        public void run() {
                            mView.updateMap();
                        }
                    });
				}
			}

			mFirstLoad = false;
		}
	}

	private class GxImageMapTapListener implements ImageViewTouch.OnImageViewTouchTapListener{

		GxImageMap mView;

		public GxImageMapTapListener(GxImageMap view) {
			mView = view;
		}

		@Override
		public void onDoubleTap() {

		}



		@Override
		public void onTap(MotionEvent e) {

			int count = mView.getChildCount();
			if (count == 1) // 1 is the map image
				return;

			double minDistance = Double.MAX_VALUE;
			View candidateView = null;

			for (int i = 1; i < count; i++) {
				View v = mView.getChildAt(i);

				if (ViewHelper.getX(v) < 0 || ViewHelper.getY(v) < 0)
					continue; // not visible at the moment

				int[] screenLocation = new int[2];
				v.getLocationOnScreen(screenLocation);

				if ((e.getRawX() > screenLocation[0] && e.getRawX() < screenLocation[0] + (v.getWidth() * ViewHelper.getScaleX(v)))
					&& e.getRawY() > screenLocation[1] && e.getRawY() < screenLocation[1] + (v.getHeight() * ViewHelper.getScaleY(v)))
				{

					float centerX = screenLocation[0] + ((v.getWidth() * ViewHelper.getScaleX(v)) / 2);
					float centerY = screenLocation[1] + ((v.getHeight() * ViewHelper.getScaleY(v)) / 2);

					//PITAGORAS!
					double distance = Math.sqrt((double)(centerY - e.getRawY()) * (centerY - e.getRawY()) + ((centerX - e.getRawX()) * (centerX - e.getRawX())));

					minDistance = Math.min(minDistance, distance);
					if(minDistance == distance)
						candidateView = v;
				}
			}

			if (candidateView != null){
				Entity entity = (Entity)candidateView.getTag(R.id.tag_imagemap_item_entity);
				mView.getHelper().runDefaultAction(entity);
			}
		}
	}

	@Override
	public void setProperty(String name, Object value) {
		if (name.equalsIgnoreCase("Image")) {

			Drawable d = ImageHelper.getStaticImage(value.toString());
			if (d != null)
				mImageMap.setImageDrawable(d);
		}
	}

	@Override
	public Object getProperty(String name) {
		return null;
	}

	@Override
	public void runMethod(String name, List<Object> parameters)
	{

		final String METHOD_IMAGE = "SetBackgroundImage";

		if (name.equalsIgnoreCase(METHOD_IMAGE) && parameters.size() >= 1
				&& mLoader!=null)
		{
			String image = parameters.get(0).toString();
			if (image.startsWith("./"))
			{
				//relative path in android file system.
				String blobBasePath = AndroidContext.ApplicationContext.getFilesBlobsApplicationDirectory();
				// Local path in sdcard.
				image = blobBasePath + image.substring(1);
			}
			ImageHelper.showDataImage(mLoader, ImageViewDisplayImageWrapper.to(mImageMap), image, false);
		}

	}
}
