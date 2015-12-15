package com.artech.controls;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import com.artech.android.ResourceManager;
import com.artech.base.metadata.enums.ActionTypes;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.utils.Strings;
import com.artech.common.PhoneHelper;
import com.artech.common.StandardImages;

public class GxAudioView extends GxLinearLayout implements IGxEdit, IGxThemeable
{
	private final ViewGroup.LayoutParams mLayoutParams;
	private String mValue = Strings.EMPTY;

	public GxAudioView(Context context, LayoutItemDefinition definition) {
		super(context);
		mLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, definition.CellGravity);
	}

	@Override
	public String getGx_Value() {
		return mValue;
	}

	@Override
	public void setGx_Value(String value) {
		mValue = value;

		removeAllViews();
		GxImageViewStatic imageView = new GxImageViewStatic(getContext());
		if (Strings.hasValue(value)) {
			imageView.setImageResource(ResourceManager.getContentDrawableFor(getContext(), ActionTypes.ViewAudio));
			imageView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					PhoneHelper.launchDomainAction(getContext(), ActionTypes.ViewAudio, mValue);
				}
			});
		} else {
			StandardImages.showPlaceholderImage(imageView, true);
		}
		addView(imageView, mLayoutParams);
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
	public String getGx_Tag() {
		return getTag().toString();
	}

	@Override
	public void setGx_Tag(String tag) {
		setTag(tag);
	}

	@Override
	public void setValueFromIntent(Intent data) {

	}

	@Override
	public boolean isEditable() {
		return false;
	}
}
