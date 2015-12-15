package com.artech.base.metadata.enums;

import com.artech.base.metadata.Properties;

public final class Alignment
{
	// Constant values MUST match values in android.view.Gravity.
	public static final int NONE = 0;

	public static final int TOP = 48;
	public static final int CENTER_VERTICAL = 16;
	public static final int BOTTOM = 80;

	public static final int VERTICAL_MASK = TOP | CENTER_VERTICAL | BOTTOM;
	
	public static final int LEFT = 3;
	public static final int CENTER_HORIZONTAL = 1;
	public static final int RIGHT = 5;

	public static final int HORIZONTAL_MASK = LEFT | CENTER_HORIZONTAL | RIGHT;
	
	public static final int CENTER = 17;
	
	private Alignment() { } // To prevent instantiation
	
	private static final String IMAGE_POSITION_ABOVE_TEXT = "Above Text"; //$NON-NLS-1$
	private static final String IMAGE_POSITION_BELOW_TEXT = "Below Text"; //$NON-NLS-1$
	private static final String IMAGE_POSITION_BEFORE_TEXT = "Before Text"; //$NON-NLS-1$
	private static final String IMAGE_POSITION_AFTER_TEXT = "After Text"; //$NON-NLS-1$
	private static final String IMAGE_POSITION_BEHIND_TEXT = "Behind Text"; //$NON-NLS-1$
	
	public static final String ADS_POSITION_BOTTOM = "Bottom"; //$NON-NLS-1$
	public static final String ADS_POSITION_TOP = "Top"; //$NON-NLS-1$
	
	public static int parseImagePosition(String jsonValue, int platformDefault)
	{
		if (Properties.PLATFORM_DEFAULT.equalsIgnoreCase(jsonValue))
			return platformDefault;
		
		return parseImagePosition(jsonValue);
	}
	
	public static int parseImagePosition(String jsonValue)
	{
		if (IMAGE_POSITION_ABOVE_TEXT.equals(jsonValue))
			return Alignment.TOP;
		
		if (IMAGE_POSITION_BELOW_TEXT.equals(jsonValue))
			return Alignment.BOTTOM;
	
		if (IMAGE_POSITION_BEFORE_TEXT.equals(jsonValue))
			return Alignment.LEFT;

		if (IMAGE_POSITION_AFTER_TEXT.equals(jsonValue))
			return Alignment.RIGHT;
				
		if (IMAGE_POSITION_BEHIND_TEXT.equals(jsonValue))
			return Alignment.CENTER;
					
		return Alignment.NONE;
	}
}
