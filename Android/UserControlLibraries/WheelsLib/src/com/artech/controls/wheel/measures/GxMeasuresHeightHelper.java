package com.artech.controls.wheel.measures;

import org.json.JSONObject;

import com.artech.controls.wheel.R;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class GxMeasuresHeightHelper implements IGxMeasuresHelper {

	private static double mInchesToMeters = 0.0254;
	private static int mFeetToInches = 12;
	
	private String mUnit = MeasureHeight.MeasureHeightMeters;
	private String mCurrentUnit = MeasureHeight.MeasureHeightMeters;
	private double mValue = 0; //saved the value in inches or meters
	private double mCurrentValue = 0;
	
	static String MeasureHeightFeet = "Feet"; //$NON-NLS-1$
	
	private static String mPrefixMeters = "m"; //$NON-NLS-1$
	private static String mPrefixFeet = "ft"; //$NON-NLS-1$
	private static String mPrefixInches = "in"; //$NON-NLS-1$
	
	final class MeasureHeight {
		public static final String MeasureHeightMeters = "Meters"; //$NON-NLS-1$
		public static final String MeasureHeightInches = "Inches"; //$NON-NLS-1$
	}

	private double changeMetersToInches (double meters) {
		return GxMeasuresHelper.round((meters)* (1 / mInchesToMeters), 0).doubleValue();
	}

	private double changeFeetToInches (double feet) {
		return changeFeetToInches(feet, 1);
	}
	
	private double changeFeetToInches (double feet, int decimals) {
		int numeric = GxMeasuresHelper.getNumericByDouble(feet);
		int decimal = getDecimalByDouble(feet, MeasureHeight.MeasureHeightInches);
		double inches =  changeFeetToInches(numeric, decimal);
		return GxMeasuresHelper.round(inches, decimals).doubleValue();
	}

	private double changeFeetToInches(int numeric, int decimal) {
		return (numeric * mFeetToInches) + decimal;
	}

	public double changeInchesToMeters (double inches) {
		return GxMeasuresHelper.round(inches * mInchesToMeters, 2).doubleValue();
	}

	// Get the numeric value to make the conversion from feet to inches
	public int getNumericFeetByInches(double inches) {
		return (int) (inches / mFeetToInches);
	}
	
	// Get the numeric value to rest the conversion from feet to inches
	public int getRestFeetByInches(double inches) {
		return (int) (inches % mFeetToInches);
	}

	public int getDecimalByDouble(double value) {
		return getDecimalByDouble(value, mCurrentUnit);
	}

	private int getDecimalByDouble(double value, String unit) {
		String [] strValue = Services.Strings.split(String.valueOf(value), Strings.DOT);
		if (strValue.length > 1) {
			if (unit.equalsIgnoreCase(MeasureHeight.MeasureHeightMeters) && strValue[1].length()==1)
				// If the unit is meters and the value is eg: 1.7, it is actually 1.70, I return 70 and not 7.
				return Integer.valueOf(strValue[1]) * 10;
			return Integer.valueOf(strValue[1]);
		}
		return 0;
	}
	
	// Get display value in the "Action" button
	@Override
	public String getCurrentStringValue(int currentItemNumeric, int currentItemDecimal) {
		mCurrentValue = getCurreentValue(currentItemNumeric, currentItemDecimal);
		return getDisplayValue(mCurrentValue, mCurrentUnit);
	}
	
	@Override
	public String getDisplayValue(double value) {
		return getDisplayValue(value, mUnit);
	}
	
	private String getDisplayValue(double value, String unit) {
		int numeric = 0;
		int decimal = 0;
		if (unit.equalsIgnoreCase(MeasureHeight.MeasureHeightMeters)) {
			numeric = GxMeasuresHelper.getNumericByDouble(value);
			decimal = getDecimalByDouble(value, unit);
			String strDecimal = String.valueOf(decimal);
			if (decimal < 10)
				strDecimal = Strings.ZERO.concat(strDecimal); //To print eg. 1.07 m and 1.7 m
			return String.valueOf(numeric).concat(Strings.DOT).concat(strDecimal).concat(Strings.SPACE).concat(mPrefixMeters);
		} else if (unit.equalsIgnoreCase(MeasureHeight.MeasureHeightInches)) {
			numeric = getNumericFeetByInches(value);
			decimal = getRestFeetByInches(value);
			return String.valueOf(numeric).concat("'").concat(String.valueOf(decimal).concat("''").concat(Strings.SPACE).concat(mPrefixFeet)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return Strings.EMPTY;
	}

	// Get the current value, if the unit is "Inches" return the value in inches.
	private double getCurreentValue(int currentItemNumeric, int currentItemDecimal) {
		if (mCurrentUnit.equalsIgnoreCase(MeasureHeight.MeasureHeightMeters))
			if (currentItemDecimal < 10)
				// If the unit is meters and value of the decimal is less than 10 will add a 0 after the "."
				return Double.parseDouble(String.valueOf(currentItemNumeric).concat(Strings.DOT).concat(Strings.ZERO).concat(String.valueOf(currentItemDecimal)));
			else
				return Double.parseDouble(String.valueOf(currentItemNumeric).concat(Strings.DOT).concat(String.valueOf(currentItemDecimal)));
		if (mCurrentUnit.equalsIgnoreCase(MeasureHeight.MeasureHeightInches))
			return changeFeetToInches(currentItemNumeric, currentItemDecimal);
		return 0;
	}
	
	@Override
	public String getGx_Value(String valueKey, String unitKey, String convertedValueKey) {
		double value = mValue;
		JSONObject valueJSONObject = new JSONObject();
		try {
			if (mUnit.equalsIgnoreCase(MeasureHeight.MeasureHeightMeters)) {
				valueJSONObject.put(valueKey, value);
				valueJSONObject.put(unitKey, mPrefixMeters);
				valueJSONObject.put(convertedValueKey, value);
			} else if (mUnit.equalsIgnoreCase(MeasureHeight.MeasureHeightInches)) {
				valueJSONObject.put(valueKey, value);
				valueJSONObject.put(unitKey, mPrefixInches);
				valueJSONObject.put(convertedValueKey, changeInchesToMeters(value));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return valueJSONObject.toString();
	}

	@Override
	public String getTextButtonChange() {
		String toView = Strings.EMPTY;
		if (mCurrentUnit.equalsIgnoreCase(MeasureHeight.MeasureHeightMeters))
			toView = String.format(Services.Strings.getResource(R.string.GXM_ConvertTo), GxMeasuresHeightHelper.MeasureHeightFeet);
		else if (mCurrentUnit.equalsIgnoreCase(MeasureHeight.MeasureHeightInches))
			toView = String.format(Services.Strings.getResource(R.string.GXM_ConvertTo), MeasureHeight.MeasureHeightMeters);
		return toView;
	}

	@Override
	public double getValueKey(double valueKey, String valueUnitKey) {
		setValueUnitKey(valueUnitKey);
		mValue =  getCorrectValueKey(valueKey, valueUnitKey);
		return mValue;
	}
	
	private void setValueUnitKey(String valueUnitKey) {
		if (valueUnitKey.equalsIgnoreCase(mPrefixInches) || valueUnitKey.equalsIgnoreCase("inches") || valueUnitKey.equalsIgnoreCase(mPrefixFeet) || valueUnitKey.equalsIgnoreCase("feet")) { //$NON-NLS-1$ //$NON-NLS-2$
			mUnit = MeasureHeight.MeasureHeightInches;
		} else if (valueUnitKey.equalsIgnoreCase("metros") || valueUnitKey.equalsIgnoreCase(mPrefixMeters)) { //$NON-NLS-1$
			mUnit = MeasureHeight.MeasureHeightMeters;
		} else {
			//the default value Meters
			mUnit = MeasureHeight.MeasureHeightMeters;
		}
	}
	
	private double getCorrectValueKey(double valueKey, String valueUnitKey) {
		if (valueUnitKey.equalsIgnoreCase("ft") || valueUnitKey.equalsIgnoreCase("feet")) { //$NON-NLS-1$ //$NON-NLS-2$
			//change feet to inches
			valueKey = changeFeetToInches(valueKey);
		}
		return valueKey;
	}
	
	@Override
	public void changeValue(int currentItemNumeric, int currentItemDecimal) {
		mCurrentValue = getCurreentValue(currentItemNumeric, currentItemDecimal);
		if (mCurrentUnit.equalsIgnoreCase(MeasureHeight.MeasureHeightMeters)) {
			//meters to inches
			mCurrentValue = changeMetersToInches(mCurrentValue);
			mCurrentUnit = MeasureHeight.MeasureHeightInches;
		} else if (mCurrentUnit.equalsIgnoreCase(MeasureHeight.MeasureHeightInches)) {
			//inches to meters
			mCurrentValue = changeInchesToMeters(mCurrentValue);
			mCurrentUnit = MeasureHeight.MeasureHeightMeters;
		}
	}

	@Override
	public void setValueInWheelControl(GxMeasuresControl linearLayout) {
		double value = mCurrentValue;
		if (mCurrentUnit.equalsIgnoreCase(MeasureHeight.MeasureHeightMeters)) {
			linearLayout.setWheelControlViewAdapter(0, 2, 0, 99, GxMeasuresHelper.getNumericByDouble(value), getDecimalByDouble(value));
		} else if (mCurrentUnit.equalsIgnoreCase(MeasureHeight.MeasureHeightInches)) {
			linearLayout.setWheelControlViewAdapter(0, 7, 0, 11, getNumericFeetByInches(value), getRestFeetByInches(value));
		} else {
			linearLayout.setWheelControlViewAdapter(0, 2, 0, 99, GxMeasuresHelper.getNumericByDouble(value), getDecimalByDouble(value));
		}
	}

	@Override
	public void OkClickListener() {
		mValue = mCurrentValue;
		mUnit = mCurrentUnit;
	}

	@Override
	public void ActionClickListener() {
		mCurrentValue = mValue;
		mCurrentUnit = mUnit;
	}
	
}
