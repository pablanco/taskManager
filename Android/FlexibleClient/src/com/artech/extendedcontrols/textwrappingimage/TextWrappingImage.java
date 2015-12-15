package com.artech.extendedcontrols.textwrappingimage;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.artech.R;
import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.Services;
import com.artech.controls.GxImageViewStatic;
import com.artech.controls.GxTextView;
import com.artech.controls.IGxEdit;
import com.artech.controls.IGxThemeable;
import com.artech.ui.Coordinator;
import com.artech.utils.ThemeUtils;
import com.fedorvlasov.lazylist.ImageLoader;

public class TextWrappingImage extends LinearLayout implements IGxEdit, IGxThemeable
{
	private ImageLoader mLoader;

	private String mValue = "";
	private LayoutItemDefinition mDefinition = null;
	private Coordinator mCoordinator = null;

	//Controls
	private GxImageViewStatic mImage = null;
	private GxTextView mTitle = null;
	private GxTextView mSubTitle = null;

	//control info
	private String mSubTitleAtt = null;
	private String mImageAtt = null;
	private String mImageWidth = null;
	private String mImageHeight = null;

	private ThemeClassDefinition mThemeClass;


	public TextWrappingImage(Context context) {
		super(context);
		initialize(context);
	}

	public TextWrappingImage(Context context, Coordinator coordinator, LayoutItemDefinition def)
	{
		super(context);
		initialize(context);
		setLayoutDefinition(def);
		mCoordinator = coordinator;
		mLoader = ImageLoader.fromContext(context);
	}

	private void initialize(Context context)
	{
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    if(inflater != null){
	       inflater.inflate(com.artech.R.layout.textwrappingimage, this, true);
	    }

		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1);
		//lp.setMargins(1, 1, 5 ,1);
		setLayoutParams(lp);

		mImage = (GxImageViewStatic) findViewById(R.id.textImage);
		mTitle = (GxTextView) findViewById(R.id.textTitle);
		mSubTitle = (GxTextView) findViewById(R.id.textSubTitle);
		//init();
	}

	@Override
	public String getGx_Value() {
		return mValue;
	}

	@Override
	public void setGx_Value(String value) {
		mValue = value;

		// set value of title, subtitle and image.
		if (mTitle!=null)
			mTitle.setGx_Value(value);

		//set image
		if (mImageAtt!=null)
		{
			String imageUrl = (String) mCoordinator.getValue(mImageAtt);
			if (Services.Strings.hasValue(imageUrl))
				com.artech.common.ImageHelper.showDataImage(mLoader, mImage, imageUrl);
			else
				mImage.setVisibility(View.GONE);

			//set image size
			ViewGroup.LayoutParams params = mImage.getLayoutParams();

			try
			{
				int height = Integer.parseInt( mImageHeight);
				params.height = Services.Device.dipsToPixels(height);
			}
			catch (NumberFormatException ex) { }

			try
			{
				int width = Integer.parseInt( mImageWidth);
				params.width = Services.Device.dipsToPixels(width);
			}
			catch (NumberFormatException ex)
			{}

		}

		//set subtitle
		if (mSubTitleAtt!=null)
		{
			String subTitleValue = (String) mCoordinator.getValue(mSubTitleAtt);
			if (Services.Strings.hasValue(subTitleValue))
				mSubTitle.setGx_Value(subTitleValue);
			else
				mSubTitle.setVisibility(View.GONE);
		}

	}

	@Override
	public String getGx_Tag()
	{
		return null;
	}

	@Override
	public void setGx_Tag(String tag)
	{
	}

	@Override
	public void setValueFromIntent(Intent data)
	{
	}

	@Override
	public IGxEdit getViewControl() {
		return this;
	}

	@Override
	public IGxEdit getEditControl() {
		return this;
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

	private void setLayoutDefinition(LayoutItemDefinition layoutItemDefinition)
	{
		mDefinition = layoutItemDefinition;
		if (mDefinition != null)
		{
			setControlInfo(mDefinition.getControlInfo());
			setThemeClass(mDefinition.getThemeClass());
		}

	}

	protected void setControlInfo(ControlInfo info) {
		//mFlipperOptions = new FlipperOptions();

		//mFlipperOptions.setShowFooter(info.optBooleanProperty("@SDPagedGridShowPageController")); //$NON-NLS-1$
		// Footer Color
		Integer colorId = ThemeUtils.getColorId(info.optStringProperty("@SDPagedGridPageControllerBackColor"));
		if (colorId!=null)
		{
			//mFlipperOptions.setFooterColorId(colorId);
		}
		mSubTitleAtt = info.optStringProperty("@SDTextWrappingImageControlSubTitleAttribute");
		mImageAtt = info.optStringProperty("@SDTextWrappingImageControlImageAttribute");
		Services.Log.debug("Image att" + mImageAtt);
		mImageWidth = info.optStringProperty("@SDTextWrappingImageControlImageWidth");
		mImageHeight = info.optStringProperty("@SDTextWrappingImageControlImageHeight");

	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass) {
		//set theme to title class
		if (mTitle!=null && themeClass!=null)
			mTitle.setThemeClass(themeClass);
	}

	@Override
	public boolean isEditable()
	{
		return false; // Never editable.
	}

}
