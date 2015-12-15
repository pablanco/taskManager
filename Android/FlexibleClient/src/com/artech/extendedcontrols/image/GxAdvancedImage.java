package com.artech.extendedcontrols.image;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;
import android.widget.LinearLayout;

import com.artech.application.MyApplication;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.common.StorageHelper;
import com.artech.controls.IGxEdit;
import com.artech.controls.IGxThemeable;
import com.artech.controls.ImageViewDisplayImageWrapper;
import com.fedorvlasov.lazylist.ImageLoader;

public class GxAdvancedImage extends LinearLayout implements IGxEdit , IGxThemeable
{
	private String mData;
	private String mUri;
	private String mImageIdentifier;
	private ImageLoader mLoader;
	private GxAdvancedImageDefinition mDefinition;
	private LayoutItemDefinition mdef;
	private ImageViewTouch view;
	private ThemeClassDefinition mThemeClass;

	public GxAdvancedImage(Context context, LayoutItemDefinition def) {
		super(context);

		setLayoutDefinition(def);
		mLoader = ImageLoader.fromContext(context);

		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
		view = new ImageViewTouch(getContext(),null);

		view.setLayoutParams(params);
		mDefinition = new GxAdvancedImageDefinition(MyApplication.getAppContext(),def);

		boolean enabledCopy = Boolean.valueOf(mDefinition.getmEnabledCopy());
		int maxZoom= Integer.valueOf(mDefinition.getmImageMaxZoom());
		String zoomRel = mDefinition.getmImageMaxZoomRel();
		float max = Float.valueOf(2*(maxZoom/100));

		if(zoomRel.equalsIgnoreCase("Image")){
			view.setMaxZoom(max);
			view.setMinZoom(1f);
		}else if(zoomRel.equalsIgnoreCase("Control")){
			view.setMaxZoom(max);
			view.setMinZoom(1f);
		}

		this.addView(view);

		view.setLongClickable(enabledCopy);
		view.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {

				/*new AlertDialog.Builder(v.getContext())
						.setMessage("LongClicked!")
						.setTitle("onLongClickTest")
						.setPositiveButton("OK",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.dismiss();
									}
								}).create().show();
				*/
				return false;
			}

		});



	}

	@Override
	public String getGx_Value() {
		return mUri;
	}

	private void setLayoutDefinition(LayoutItemDefinition layoutItemDefinition)
	{
		mdef = layoutItemDefinition;
	}

	@Override
	public void setGx_Value(String value)
	{
		mUri = value;

		if (mUri != null && StorageHelper.isLocalFile(mUri))
		{
			// Load the image full resolution
			com.artech.common.ImageHelper.showLocalImage(view, mUri, true);
		}
		else if (mUri != null && (mUri.startsWith("/static/Resources/") || !mUri.contains("/")))  //$NON-NLS-1$
		{
			com.artech.common.ImageHelper.showStaticImage(mLoader, ImageViewDisplayImageWrapper.to(view), getStaticName(mUri));
		}
		else
		{
			// Load the image in background, full resolution.
			new LoadBitmap().execute(mUri);
		}
	}

	private class LoadBitmap extends AsyncTask<String, Bitmap, Void>
	{
		@Override
	    protected Void doInBackground(String... params)
		{
			String imageFullPath = MyApplication.getApp().UriMaker.MakeImagePath(mUri);
			// Load the image full resolution
			Bitmap bmp = mLoader.getBitmap(imageFullPath, true);
			publishProgress(bmp);
	        return null;
	    }

	    @Override
	    protected void onProgressUpdate(Bitmap... values) {
	    	//StandardImages.stopLoading(view);
	    	view.setImageBitmap(values[0]);
	    	view.invalidate();
	    }

	}

	@Override
	public String getGx_Tag() {
		return mData;
	}

	@Override
	public void setGx_Tag(String data) {
		mData = data;
		this.setTag(data);
	}

	@Override
	public void setValueFromIntent(Intent data) {


	}

	public void setImageIdentifier(String imageIdentifier) {
		mImageIdentifier = imageIdentifier;

	}

	public String getImageIdentifier() {
		return mImageIdentifier;

	}

	@Override
	public IGxEdit getViewControl() {
		return this;
	}

	@Override
	public IGxEdit getEditControl() {
		return this;
	}

	private String getStaticName(String resource){
		int s = resource.lastIndexOf("/");
		int e = resource.indexOf(".");
		if (s==-1 || e==-1){
			return resource;
		}
		return resource.substring(s+1, e);
	}

	@Override
	public boolean isEditable()
	{
		return false; // Never editable.
	}

	@Override
	public void setThemeClass(ThemeClassDefinition themeClass)
	{
		mThemeClass = themeClass;
		applyClass(themeClass);
	}

	@Override
	public ThemeClassDefinition getThemeClass() {
		return mThemeClass;
	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass)
	{
		// Padding
		//LayoutBoxMeasures padding = themeClass.getPadding();
		//if (padding != null)
		//	setPadding(padding.left, padding.top, padding.right, padding.bottom);

		// Background and border
		//ThemeUtils.setBackgroundBorderProperties(view, themeClass, BackgroundOptions.defaultFor(mdef));

		// Content Mode, not working with zoom.
		//ImageHelper.setContentMode(view, themeClass.getImageContentMode());

		// custom content mode implementation, change start zoom size.
		view.setImageScaleType(themeClass.getImageScaleType());
	}
}