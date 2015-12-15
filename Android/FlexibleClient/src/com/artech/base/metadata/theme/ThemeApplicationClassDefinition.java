package com.artech.base.metadata.theme;

public class ThemeApplicationClassDefinition extends ThemeClassDefinition
{
	private static final long serialVersionUID = 1L;

	public final static String CLASS_NAME = "Application"; //$NON-NLS-1$

	public final static String PLACEHOLDER_IMAGE = "placeholder_image"; //$NON-NLS-1$
	public final static String PROMPT_IMAGE = "prompt_image"; //$NON-NLS-1$
	public final static String DATEPICKER_IMAGE = "datepicker_image"; //$NON-NLS-1$

	public ThemeApplicationClassDefinition(ThemeDefinition theme, ThemeClassDefinition parentClass)
	{
		super(theme, parentClass);
	}

	@Override
	public String getBackgroundColor()
	{
		return optStringProperty("background_color"); //$NON-NLS-1$
	}

	@Override
	public String getBackgroundImage()
	{
		return getImage("background_image"); //$NON-NLS-1$
	}

	public String getActionTintColor()
	{
		return optStringProperty("action_tint_color");
	}

	public String getPlaceholderImage()
	{
		return getImage("placeholder_image"); //$NON-NLS-1$
	}

	public String getPromptImage()
	{
		return getImage(PROMPT_IMAGE);
	}

	public String getDatePickerImage()
	{
		return getImage(DATEPICKER_IMAGE);
	}

	public boolean useImageLoadingIndicator()
	{
		return optBooleanProperty("image_loading_indicator"); //$NON-NLS-1$
	}
}
