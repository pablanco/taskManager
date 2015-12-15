package com.artech.base.metadata.layout;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.Services;

public class LayoutActionDefinition extends LayoutItemDefinition implements ILayoutActionDefinition
{
	private static final long serialVersionUID = 1L;

	private ActionDefinition mAction;
	private Integer mWidth;
	private Integer mHeight;

	public LayoutActionDefinition(LayoutDefinition layout, LayoutItemDefinition itemParent)
	{
		super(layout, itemParent);
	}

	@Override
	public ActionDefinition getEvent()
	{
		if (mAction == null && getLayout() != null && getLayout().getParent() != null)
		{
			String actionId = getEventName();
			mAction = getLayout().getParent().getEvent(actionId);

			if (mAction == null)
				Services.Log.warning(String.format("Action '%s', used in layout, is not present in definition of '%s'.", actionId, getLayout().getParent().getName()));
		}

		return mAction;
	}

	@Override
	public String getEventName()
	{
		return optStringProperty("@actionElement"); //$NON-NLS-1$
	}

	@Override
	public boolean isVisible()
	{
		return LayoutActionDefinitionHelper.isVisible(this);
	}

	@Override
	public boolean isEnabled()
	{
		return LayoutActionDefinitionHelper.isEnabled(this);
	}

	@Override
	public ThemeClassDefinition getThemeClass()
	{
		return LayoutActionDefinitionHelper.getThemeClass(this);
	}

	@Override
	public String getCaption()
	{
		return LayoutActionDefinitionHelper.getCaption(this);
	}

	@Override
	public String getImage()
	{
		return LayoutActionDefinitionHelper.getImage(this);
	}

	@Override
	public String getDisabledImage()
	{
		return LayoutActionDefinitionHelper.getDisabledImage(this);
	}

	@Override
	public String getHighlightedImage()
	{
		return LayoutActionDefinitionHelper.getHighlightedImage(this);
	}

	public int getImagePosition()
	{
		return LayoutActionDefinitionHelper.getImagePosition(this);
	}

	public int getWidth()
	{
		if (mWidth == null)
			mWidth = Services.Device.dipsToPixels(optIntProperty("@width")); //$NON-NLS-1$

		return mWidth;
	}

	public int getHeight()
	{
		if (mHeight == null)
			mHeight = Services.Device.dipsToPixels(optIntProperty("@height")); //$NON-NLS-1$

		return mHeight;
	}
}
