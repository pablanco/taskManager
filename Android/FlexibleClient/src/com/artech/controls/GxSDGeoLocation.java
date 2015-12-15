package com.artech.controls;

import android.content.Context;

import com.artech.base.metadata.enums.ImageScaleType;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.utils.Strings;
import com.artech.controls.maps.Maps;
import com.artech.ui.Coordinator;

public class GxSDGeoLocation extends GxImageViewData implements IHandleSemanticDomain
{
	private final LayoutItemDefinition mDefinition;
	private String mMapType = Strings.EMPTY;
	private String mValue;
	private final Coordinator mCoordinator;

	public GxSDGeoLocation(Context context, Coordinator coordinator, LayoutItemDefinition def)
	{
		super(context, def);
		mDefinition = def;
		mCoordinator = coordinator;

		mMapType = Strings.toLowerCase(def.getControlInfo().optStringProperty("@SDGeoLocationMapType")); //$NON-NLS-1$
	}

	@Override
	public String getGx_Value()
	{
		return mValue;
	}

	@Override
	public void setGx_Value(String value)
	{
		mValue = value;
		setImageScaleType(ImageScaleType.FIT);
		loadMapImage(false);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		super.onLayout(changed, left, top, right, bottom);
		loadMapImage(true);
	}

    private void loadMapImage(final boolean isInLayoutPass)
    {
    	if (mValue == null)
    		return;
    	
        int width = getWidth();
        int height = getHeight();

        boolean isFullyWrapContent = getLayoutParams() != null &&
                getLayoutParams().height == LayoutParams.WRAP_CONTENT &&
                getLayoutParams().width == LayoutParams.WRAP_CONTENT;

        if (width == 0 && height == 0 && !isFullyWrapContent)
            return;

        if (width == 0)
        	width = ((com.artech.base.metadata.layout.CellDefinition) mDefinition.getParent()).getAbsoluteWidth();

        if (height == 0)
        	height = ((com.artech.base.metadata.layout.CellDefinition) mDefinition.getParent()).getAbsoluteHeight();

		String requestMapUri = Maps.getMapImageUrl(getContext(), mValue, width, height, mMapType);
		if (requestMapUri != null)
			super.setGx_Value(requestMapUri);
    }

	@Override
	public IGxEdit getViewControl() { return this; }

	@Override
	public IGxEdit getEditControl()
	{
		GxLocationEdit edit = new GxLocationEdit(getContext(), mCoordinator, mDefinition);
		edit.setShowMap(true);
		return edit;
	}
}
