package com.artech.base.metadata.theme;

import com.artech.base.metadata.DimensionValue;

public class ThemeFormClassDefinition extends ThemeClassDefinition
{
	private static final long serialVersionUID = 1L;

	final static String CLASS_NAME = "Form"; //$NON-NLS-1$

	private TargetSize mTargetSize;

	public ThemeFormClassDefinition(ThemeDefinition theme, ThemeClassDefinition parentClass)
	{
		super(theme, parentClass);
	}

	public String getCallType()
	{
		return optStringProperty("call_type"); //$NON-NLS-1$
	}

	public String getEnterEffect()
	{
		return optStringProperty("enter_effect"); //$NON-NLS-1$
	}

	public String getExitEffect()
	{
		return optStringProperty("close_effect"); //$NON-NLS-1$
	}

	public String getTargetName()
	{
		return optStringProperty("target_name"); //$NON-NLS-1$
	}

	public TargetSize getTargetSize()
	{
		if (mTargetSize == null)
		{
			String size = optStringProperty("target_size"); //$NON-NLS-1$
			String width = optStringProperty("target_width"); //$NON-NLS-1$
			String height = optStringProperty("target_height"); //$NON-NLS-1$

			mTargetSize = new TargetSize(size, DimensionValue.parse(width), DimensionValue.parse(height));
		}

		return mTargetSize;
	}

	/**
	 * Form size (for Popup and Callout types).
	 */
	public static class TargetSize
	{
		public final String Name;
		public final DimensionValue CustomWidth;
		public final DimensionValue CustomHeight;

		public static final String SIZE_DEFAULT = "gx_default"; //$NON-NLS-1$
		public static final String SIZE_SMALL = "small"; //$NON-NLS-1$
		public static final String SIZE_MEDIUM = "medium"; //$NON-NLS-1$
		public static final String SIZE_LARGE = "large"; //$NON-NLS-1$
		public static final String SIZE_CUSTOM = "custom"; //$NON-NLS-1$

		private TargetSize(String name, DimensionValue customWidth, DimensionValue customHeight)
		{
			Name = name;
			CustomWidth = customWidth;
			CustomHeight = customHeight;
		}

		@Override
		public String toString()
		{
			return String.format("%s (%s * %s)", Name, CustomWidth, CustomHeight);
		}
	}
}
