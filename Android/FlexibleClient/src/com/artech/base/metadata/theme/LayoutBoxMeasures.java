package com.artech.base.metadata.theme;

import java.io.Serializable;

import com.artech.base.metadata.enums.MeasureUnit;
import com.artech.base.model.PropertiesObject;
import com.artech.base.services.Services;

public class LayoutBoxMeasures implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public final int left;
	public final int top;
	public final int right;
	public final int bottom;

	public static LayoutBoxMeasures from(PropertiesObject data, String measure)
	{
		return from(data, measure, 0);
	}

	public static LayoutBoxMeasures from(PropertiesObject data, String measure, int extra)
	{
		int left = Services.Device.dipsToPixels(parseIntPixelValue(data, measure + "_left")); //$NON-NLS-1$
		int top = Services.Device.dipsToPixels(parseIntPixelValue(data, measure + "_top")); //$NON-NLS-1$
		int right = Services.Device.dipsToPixels(parseIntPixelValue(data, measure + "_right")); //$NON-NLS-1$
		int bottom = Services.Device.dipsToPixels(parseIntPixelValue(data, measure + "_bottom")); //$NON-NLS-1$

		return new LayoutBoxMeasures(left + extra, top + extra, right + extra, bottom + extra);
	}

	private LayoutBoxMeasures(int aLeft, int aTop, int aRight, int aBottom)
	{
		left = aLeft;
		top = aTop;
		right = aRight;
		bottom = aBottom;
	}

	private static int parseIntPixelValue(PropertiesObject data, String property)
	{
		String strValue = data.optStringProperty(property);
		Integer value = Services.Strings.parseMeasureValue(strValue, MeasureUnit.DIP);
		return value!=null? value: 0;
	}

	public int getTotalVertical()
	{
		return top + bottom;
	}

	public int getTotalHorizontal()
	{
		return left + right;
	}
	
	public boolean isEmpty()
	{
		return ((left==0) && (top==0) && (right==0) && (bottom==0));
	}
}
