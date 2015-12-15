package com.artech.controls.wheel.measures;

import org.json.JSONObject;

import com.artech.controls.wheel.R;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class GxMeasuresVolumeHelper implements IGxMeasuresHelper{

	private static int mLitersCubicToCentimeters = 1000;
	
	private String mUnit = MeasureVolume.MeasureVolumeCubicCentimeters;
	private String mCurrentUnit = MeasureVolume.MeasureVolumeCubicCentimeters;
	private double mValue = 0;
	private double mCurrentValue = 0;
		
	private static String mPrefixCubicCentimeters = "cm3"; //$NON-NLS-1$
	private static String mPrefixLiters = "lt"; //$NON-NLS-1$
	
	final class MeasureVolume {
		public static final String MeasureVolumeCubicCentimeters = "Cubic Cm"; //$NON-NLS-1$
		public static final String MeasureVolumeLiters = "Liters"; //$NON-NLS-1$
	}
	
	private double changeCubicCentimetersToLiters(double cubicCentimeters) {
		return cubicCentimeters / mLitersCubicToCentimeters;
	}
	
	private double changeLitersToCubicCentimeters(double cubicCentimeters) {
		return cubicCentimeters * mLitersCubicToCentimeters;
	}
	
	@Override
	public String getDisplayValue(double value) {
		return getDisplayValue(value, mUnit);
	}
	
	private String getDisplayValue(double value, String unit) {
		if (unit.equalsIgnoreCase(MeasureVolume.MeasureVolumeCubicCentimeters))
			return String.valueOf(GxMeasuresHelper.getNumericByDouble(value)).concat(Strings.SPACE).concat(mPrefixCubicCentimeters);
		else if (unit.equalsIgnoreCase(MeasureVolume.MeasureVolumeLiters))
			return String.valueOf(value).concat(Strings.SPACE).concat(mPrefixLiters);
		return Strings.EMPTY;
	}
	
	// Get display value in the "Action" button
	@Override
	public String getCurrentStringValue(int currentItemNumeric, int currentItemDecimal) {
		mCurrentValue = getCurreentValue(currentItemNumeric, currentItemDecimal);
		return getDisplayValue(mCurrentValue, mCurrentUnit);
	}
	
	private double getCurreentValue(int currentItemNumeric, int currentItemDecimal) {
		if (mCurrentUnit.equalsIgnoreCase(MeasureVolume.MeasureVolumeCubicCentimeters))
			return Double.parseDouble(String.valueOf(currentItemNumeric));
		else if (mCurrentUnit.equalsIgnoreCase(MeasureVolume.MeasureVolumeLiters))
			if (currentItemDecimal < 100)
				if (currentItemDecimal < 10)
					// If the unit is liters and value of the decimal is less than 10 will add a 00 after the "."
					return Double.parseDouble(String.valueOf(currentItemNumeric).concat(Strings.DOT).concat(Strings.ZERO).concat(Strings.ZERO).concat(String.valueOf(currentItemDecimal)));
				else
					//If the unit is liters and value of the decimal is less than 100 will add a 0 after the "."
					return Double.parseDouble(String.valueOf(currentItemNumeric).concat(Strings.DOT).concat(Strings.ZERO).concat(String.valueOf(currentItemDecimal)));
			else
				return Double.parseDouble(String.valueOf(currentItemNumeric).concat(Strings.DOT).concat(String.valueOf(currentItemDecimal)));
		return 0;
	}

	@Override
	public String getGx_Value(String valueKey, String unitKey, String convertedValueKey) {
		double value = mValue;
		JSONObject valueJSONObject = new JSONObject();
		try {
			valueJSONObject.put(valueKey, value);
			valueJSONObject.put(convertedValueKey, value);
			if (mUnit.equalsIgnoreCase(MeasureVolume.MeasureVolumeCubicCentimeters)) {
				valueJSONObject.put(unitKey, mPrefixCubicCentimeters);
			} else if (mUnit.equalsIgnoreCase(MeasureVolume.MeasureVolumeLiters)) {
				valueJSONObject.put(unitKey, mPrefixLiters);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return valueJSONObject.toString();
	}

	@Override
	public String getTextButtonChange() {
		String toView = Strings.EMPTY;
		if (mCurrentUnit.equalsIgnoreCase(MeasureVolume.MeasureVolumeCubicCentimeters))
			toView = String.format(Services.Strings.getResource(R.string.GXM_ConvertTo), MeasureVolume.MeasureVolumeLiters);
		else if (mCurrentUnit.equalsIgnoreCase(MeasureVolume.MeasureVolumeLiters))
			toView = String.format(Services.Strings.getResource(R.string.GXM_ConvertTo), MeasureVolume.MeasureVolumeCubicCentimeters);
		return toView;
	}

	@Override
	public double getValueKey(double valueKey, String valueUnitKey) {
		setValueUnitKey(valueUnitKey);
		mValue =  valueKey;
		return mValue;
	}

	private void setValueUnitKey(String valueUnitKey) {
		if (valueUnitKey.equalsIgnoreCase(mPrefixCubicCentimeters)) {
			mUnit = MeasureVolume.MeasureVolumeCubicCentimeters;
		} else if (valueUnitKey.equalsIgnoreCase(mPrefixLiters)) {
			mUnit = MeasureVolume.MeasureVolumeLiters;
		} else {
			//the default value Cubic Centimeters
			mUnit = MeasureVolume.MeasureVolumeCubicCentimeters;
		}
	}
	
	@Override
	public void changeValue(int currentItemNumeric, int currentItemDecimal) {
		mCurrentValue = getCurreentValue(currentItemNumeric, currentItemDecimal);
		if (mCurrentUnit.equalsIgnoreCase(MeasureVolume.MeasureVolumeCubicCentimeters)) {
			//cubic centimeters to liters
			mCurrentValue = changeCubicCentimetersToLiters(mCurrentValue);
			mCurrentUnit = MeasureVolume.MeasureVolumeLiters;
		} else if (mCurrentUnit.equalsIgnoreCase(MeasureVolume.MeasureVolumeLiters)) {
			//liters to cubic centimeters
			mCurrentValue = changeLitersToCubicCentimeters(mCurrentValue);
			mCurrentUnit = MeasureVolume.MeasureVolumeCubicCentimeters;
		}
	}

	@Override
	public void setValueInWheelControl(GxMeasuresControl linearLayout) {
		double value = mCurrentValue;
		if (mCurrentUnit.equalsIgnoreCase(MeasureVolume.MeasureVolumeCubicCentimeters)) {
			linearLayout.setWheelControlViewAdapter(0, 99999, 0, 0, GxMeasuresHelper.getNumericByDouble(value), GxMeasuresHelper.getDecimalByDouble(value));
		} else if (mCurrentUnit.equalsIgnoreCase(MeasureVolume.MeasureVolumeLiters)) {
			linearLayout.setWheelControlViewAdapter(0, 99, 0, 999, GxMeasuresHelper.getNumericByDouble(value), GxMeasuresHelper.getDecimalByDouble(value));
		} else {
			linearLayout.setWheelControlViewAdapter(0, 99999, 0, 0, GxMeasuresHelper.getNumericByDouble(value), GxMeasuresHelper.getDecimalByDouble(value));
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
