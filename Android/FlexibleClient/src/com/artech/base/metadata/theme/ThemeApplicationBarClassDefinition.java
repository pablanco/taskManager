package com.artech.base.metadata.theme;

public class ThemeApplicationBarClassDefinition extends ThemeClassDefinition
{
	private static final long serialVersionUID = 1L;

	public final static String CLASS_NAME = "ApplicationBars"; //$NON-NLS-1$

	public ThemeApplicationBarClassDefinition(ThemeDefinition theme, ThemeClassDefinition parentClass)
	{
		super(theme, parentClass);
	}

	public String getTitleImage()
	{
		return getImage("title_image"); //$NON-NLS-1$
	}

	public String getIcon()
	{
		return getImage("application_bar_icon"); //$NON-NLS-1$
	}

	public String getStatusBarColor()
	{
		return optStringProperty("status_bar_color"); //$NON-NLS-1$
	}
}
