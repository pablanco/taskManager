package com.artech.extendedcontrols.image;

import android.content.Context;

import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.controls.ControlPropertiesDefinition;

class GxAdvancedImageDefinition  extends ControlPropertiesDefinition
{
	private String mEnabledCopy;
	private String mImageMaxZoomRel;
	private String mImageMaxZoom;

	public GxAdvancedImageDefinition(Context context, LayoutItemDefinition def)
	{
		super(def);
		mEnabledCopy = def.getControlInfo().optStringProperty("@SDAdvancedImageEnableCopy");
		mImageMaxZoomRel = def.getControlInfo().optStringProperty("@SDAdvancedImageMaxZoomRel");
		mImageMaxZoom = def.getControlInfo().optStringProperty("@SDAdvancedImageMaxZoom");

	}

	public String getmEnabledCopy()
	{
		return mEnabledCopy;
	}

	public String getmImageMaxZoom()
	{
		return mImageMaxZoom;
	}

	public String getmImageMaxZoomRel()
	{
		return mImageMaxZoomRel;
	}
}