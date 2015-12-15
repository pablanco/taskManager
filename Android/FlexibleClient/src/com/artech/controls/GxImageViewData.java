package com.artech.controls;

import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.widget.ProgressBar;

import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.common.ImageHelper;
import com.fedorvlasov.lazylist.ImageLoader;

public class GxImageViewData extends GxImageViewBase implements IGxEdit
{
	private final LayoutItemDefinition mDefinition;
	private final ImageLoader mLoader;
	private String mData;
	private boolean mEditable;

	private ProgressBar mLoadingIndicator;
	private String mUri;

	public GxImageViewData(Context context, LayoutItemDefinition itemLayout)
	{
		this(context, itemLayout, ImageLoader.fromContext(context));
	}

	public GxImageViewData(Context context, LayoutItemDefinition layoutItem, ImageLoader loader)
	{
		super(context);
		mDefinition = layoutItem;
		mData = layoutItem.getDataId();

		setAutogrow(layoutItem.hasAutoGrow());
		mLoader = loader;
	}

	public void setLoading(boolean loading)
	{
		if (mLoadingIndicator == null && loading)
		{
			mLoadingIndicator = new ProgressBar(getContext(), null, android.R.attr.progressBarStyle);
			mLoadingIndicator.setIndeterminate(true);

			LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
			addView(mLoadingIndicator, lp);
		}

		if (mLoadingIndicator != null)
			mLoadingIndicator.setVisibility(loading ? VISIBLE : INVISIBLE);

		getImageView().setVisibility(loading ? INVISIBLE : VISIBLE);
	}

	@Override
	public String getGx_Value()
	{
		return mUri;
	}

	@Override
	public void setGx_Value(String value)
	{
		mUri = value;
		if (mUri != null)
		{
			// If the image is editable, we need a "visual tappable thingie". Otherwise don't display it.
			boolean needsPlaceholder = isEditable();
			ImageHelper.showDataImage(mLoader, this, value, needsPlaceholder);
		}
	}

	@Override
	public String getGx_Tag()
	{
		return mData;
	}

	@Override
	public void setGx_Tag(String data)
	{
		mData = data;
		setTag(data);
	}

	@Override
	public void setValueFromIntent(Intent data) { }

	@Override
	public IGxEdit getViewControl()
	{
		setEditable(false);
		return this;
	}

	@Override
	public IGxEdit getEditControl()
	{
		setEditable(true);
		return this;
	}

	public String getLabel()
	{
		return mDefinition.getCaption();
	}

	public String getControlType()
	{
		return mDefinition.getControlType();
	}

	@Override
	public boolean isEditable()
	{
		return mEditable;
	}

	public void setEditable(boolean value)
	{
		mEditable = value;
	}

	public int getMaximumUploadSizeMode()
	{
		return mDefinition.getMaximumUploadSizeMode();
	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass)
	{
		// For data images, we only consider scale type and custom size.
		// Background, etc is handled by the DataBoundControl parent.
		if (themeClass != null)
		{
			setPropertiesImageSizeScaleRadiusFromThemeClass(themeClass);
		}
	}
}
