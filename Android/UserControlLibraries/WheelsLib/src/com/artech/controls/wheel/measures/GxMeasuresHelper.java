package com.artech.controls.wheel.measures;

import java.math.BigDecimal;

import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class GxMeasuresHelper {
	
	static final String mUnitKey = "Unit"; //$NON-NLS-1$
	static final String mValueKey = "Value"; //$NON-NLS-1$
	static final String mConvertedValueKey = "ConvertedValue"; //$NON-NLS-1$
	
	final class MeasureType {
		static final String Height = "Height"; //$NON-NLS-1$
		static final String Weight = "Weight"; //$NON-NLS-1$
		static final String Temperature = "Temperature"; //$NON-NLS-1$
		static final String Volume = "Volume"; //$NON-NLS-1$
	}
	
	public static BigDecimal round(double value, int decimals)
	{
		BigDecimal valueBigDecimal = new BigDecimal(value);
		return valueBigDecimal.setScale(decimals, BigDecimal.ROUND_HALF_UP);
	}

	public static int getNumericByDouble(double value) {
		BigDecimal valueBigDecimal = new BigDecimal(value);
		return valueBigDecimal.setScale(0, BigDecimal.ROUND_DOWN).intValue();
	}

	public static int getDecimalByDouble(double value) {
		String [] strValue = Services.Strings.split(String.valueOf(value), Strings.DOT);
		if (strValue.length > 1)
			return Integer.valueOf(strValue[1]);
		return 0;
	}

}
