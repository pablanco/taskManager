package com.artech.base.metadata.layout;

import com.artech.base.metadata.enums.Alignment;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;
import com.artech.base.utils.PlatformHelper;

public class TabItemDefinition extends LayoutItemDefinition
{
	private static final long serialVersionUID = 1L;

	private String mControlName;
	private String mCaption;
	private String mImage;
	private String mImageUnSelected;
	private int mImageAlignment;
	private String mSelectedClass;

	public TabItemDefinition(LayoutDefinition layout, LayoutItemDefinition parent)
	{
		super(layout, parent);

		if (!(parent instanceof TabControlDefinition))
			throw new IllegalArgumentException("Tab item cannot be created outside of a tab control."); //$NON-NLS-1$
	}

	@Override
	public String getName()
	{
		return mControlName;
	}

	@Override
	public String getCaption() { return Services.Resources.getTranslation(mCaption); }
	public String getImage() { return mImage; }
	public String getImageUnselected() { return mImageUnSelected; }
	public int getImageAlignment() { return mImageAlignment; }

	@Override
	public TabControlDefinition getParent()
	{
		return (TabControlDefinition)super.getParent();
	}

	@Override
	public void readData(INodeObject node)
	{
		super.readData(node);
		mControlName = node.optString("@itemControlName"); //$NON-NLS-1$
		mCaption = node.optString("@caption"); //$NON-NLS-1$
		mImage = MetadataLoader.getObjectName(node.optString("@image")); //$NON-NLS-1$
		mImageUnSelected = MetadataLoader.getObjectName(node.optString("@unselectedImage")); //$NON-NLS-1$
		mImageAlignment = Alignment.parseImagePosition(node.optString("@imagePosition"), Alignment.LEFT); //$NON-NLS-1$
		mSelectedClass = node.optString("@selClass");
	}

	public TableDefinition getTable()
	{
		return (TableDefinition) getChildItems().get(0);
	}
	
	public ThemeClassDefinition getSelectedClass()
	{
		return PlatformHelper.getThemeClass(mSelectedClass); 
	}
	
	public ThemeClassDefinition getUnselectedClass()
	{
		return super.getThemeClass();
	}
}
