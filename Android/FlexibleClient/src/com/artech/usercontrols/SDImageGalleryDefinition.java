package com.artech.usercontrols;

import android.content.Context;
import android.graphics.Rect;

import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.DataTypeName;
import com.artech.base.metadata.enums.DataTypes;
import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.services.Services;
import com.artech.controls.grids.CustomGridDefinition;

public class SDImageGalleryDefinition extends CustomGridDefinition
{
	private String mBehavior;
   	private String mImageAttribute;
	private String mThumbnailAttribute;
	private String mTitleAttribute;
	private String mSubtitleAttribute;
	private Rect mThumbnailSize;
	private boolean mHasShareAction;

	public SDImageGalleryDefinition(Context context, GridDefinition grid)
	{
		super(context, grid);
	}

	@Override
	protected void init(GridDefinition grid, ControlInfo controlInfo)
	{
		mBehavior = controlInfo.optStringProperty("@SDImageGalleryGridBehavior"); //$NON-NLS-1$

		mImageAttribute = readDataExpression("@SDImageGalleryDataAtt", "@SDImageGalleryDataField"); //$NON-NLS-1$ $NON-NLS-2$
		// mThumbnailAttribute = readDataExpression(...)
		mTitleAttribute = readDataExpression("@SDImageGalleryTitleAtt", "@SDImageGalleryTitleField"); //$NON-NLS-1$ $NON-NLS-2$
		mSubtitleAttribute = readDataExpression("@SDImageGallerySubtitleAtt", "@SDImageGallerySubtitleField"); //$NON-NLS-1$ $NON-NLS-2$

		// Default image attribute, if not set, is first attribute of type image.
		if (!Services.Strings.hasValue(mImageAttribute))
			mImageAttribute = getDefaultImageAttribute(grid);

		// Remove this when the thumbnail attribute is added in definition.
		if (!Services.Strings.hasValue(mThumbnailAttribute))
			mThumbnailAttribute = mImageAttribute;

		if (mThumbnailSize == null)
		{
			int sideRight = 150;
			int sideBottom = 120;
			mThumbnailSize = new Rect(0, 0, sideRight, sideBottom);
		}

		mHasShareAction = controlInfo.optBooleanProperty("@SDImageGalleryEnableShare"); //$NON-NLS-1$
	}

	private static String getDefaultImageAttribute(GridDefinition grid)
	{
    	for (DataItem dataAtt : grid.getDataSourceItems())
    	{
    		if (dataAtt.getBaseType() != null)
    		{
    			DataTypeName attDataType = dataAtt.getDataTypeName();
    			if (attDataType != null && DataTypes.isImage(attDataType.GetDataType()))
    				return dataAtt.getName();
    		}
    	}

    	return null;
	}

	public String getBehavior() { return mBehavior; }
	public String getImageAttribute() { return mImageAttribute; }
	public String getThumbnailAttribute() { return mThumbnailAttribute; }
	public String getTitleAttribute() { return mTitleAttribute; }
	public String getSubtitleAttribute() { return mSubtitleAttribute; }
	public Rect getThumbnailSize() { return mThumbnailSize; }
	public boolean hasShareAction() { return mHasShareAction; }
}
