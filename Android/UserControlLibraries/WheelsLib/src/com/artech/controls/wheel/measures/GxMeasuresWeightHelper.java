package com.artech.controls.wheel.measures;

import org.json.JSONObject;

import com.artech.controls.wheel.R;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class GxMeasuresWeightHelper implements IGxMeasuresHelper {

	private static double mKilogramToPound = 2.20462262;
	
	private String mUnit = MeasureWeight.MeasureWeightKilogram;
	private String mCurrentUnit = MeasureWeight.MeasureWeightKilogram;
	private double mValue = 0;
	private double mCurrentValue = 0;
	
	final class MeasureWeight {
		public static final String MeasureWeightPound = "Pounds"; //$NON-NLS-1$
		public static final String MeasureWeightKilogram = "Kilograms"; //$NON-NLS-1$
	}
	
	private static String mPrefixKilogram = "kg"; //$NON-NLS-1$
	private static String mPrefixPound = "lb"; //$NON-NLS-1$
	
	private double changeKilogramToPound (double kilogram) {
		return GxMeasuresHelper.round(kilogram * mKilogramToPound, 1).doubleValue();
	}
	
	private double changePoundToKilogram (double pound) {
		return GxMeasuresHelper.round(pound / mKilogramToPound, 1).doubleValue();
	}

	@Override
	public String getDisplayValue(double value) {
		return getDisplayValue(value, mUnit);
	}
	
	private String getDisplayValue(double value, String unit) {
		if (unit.equalsIgnoreCase(MeasureWeight.MeasureWeightKilogram))
			return String.valueOf(value).concat(Strings.SPACE).concat(mPrefixKilogram);
		else if (unit.equalsIgnoreCase(MeasureWeight.MeasureWeightPound))
			return String.valueOf(value).concat(Strings.SPACE).concat(mPrefixPound);
		return Strings.EMPTY;
	}
	
	// Get display value in the "Action" button
	@Override
	public String getCurrentStringValue(int currentItemNumeric, int currentItemDecimal) {
		mCurrentValue = getCurreentValue(currentItemNumeric, currentItemDecimal);
		return getDisplayValue(mCurrentValue, mCurrentUnit);
	}

	private double getCurreentValue(int currentItemNumeric, int currentItemDecimal) {
		return Double.parseDouble(String.valueOf(currentItemNumeric).concat(Strings.DOT).concat(String.valueOf(currentItemDecimal)));
	}
	
	@Override
	public String getGx_Value(String valueKey, String unitKey, String convertedValueKey) {
		double value = mValue;
		JSONObject valueJSONObject = new JSONObject();
		try {
			valueJSONObject.put(valueKey, value);
			valueJSONObject.put(convertedValueKey, value);
			if (mUnit.equalsIgnoreCase(MeasureWeight.MeasureWeightKilogram)) {
				valueJSONObject.put(unitKey, mPrefixKilogram);
			} else if (mUnit.equalsIgnoreCase(MeasureWeight.MeasureWeightPound)) {
				valueJSONObject.put(unitKey, mPrefixPound);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return valueJSONObject.toString();
	}

	@Override
	public String getTextButtonChange() {
		String toView = Strings.EMPTY;
		if (mCurrentUnit.equalsIgnoreCase(MeasureWeight.MeasureWeightKilogram))
			toView = String.format(Services.Strings.getResource(R.string.GXM_ConvertTo), MeasureWeight.MeasureWeightPound);
		else if (mCurrentUnit.equalsIgnoreCase(MeasureWeight.MeasureWeightPound))
			toView = String.format(Services.Strings.getResource(R.string.GXM_ConvertTo), MeasureWeight.MeasureWeightKilogram);
		return toView;
	}
	
	@Override
	public double getValueKey(double valueKey, String valueUnitKey) {
		setValueUnitKey(valueUnitKey);
		mValue =  valueKey;
		return mValue;
	}

	private void setValueUnitKey(String valueUnitKey) {
		if (valueUnitKey.equalsIgnoreCase(mPrefixPound)) {
			mUnit = MeasureWeight.MeasureWeightPound;
		} else if (valueUnitKey.equalsIgnoreCase(mPrefixKilogram)) {
			mUnit = MeasureWeight.MeasureWeightKilogram;
		} else {
			//the default value Kilograms
			mUnit = MeasureWeight.MeasureWeightKilogram;
		}
	}
	
	@Override
	public void changeValue(int currentItemNumeric, int currentItemDecimal) {
		mCurrentValue = getCurreentValue(currentItemNumeric, currentItemDecimal);
		if (mCurrentUnit.equalsIgnoreCase(MeasureWeight.MeasureWeightKilogram)) {
			//kilograms to pounds
			mCurrentValue = changeKilogramToPound(mCurrentValue);
			mCurrentUnit = MeasureWeight.MeasureWeightPound;
		} else if (mCurrentUnit.equalsIgnoreCase(MeasureWeight.MeasureWeightPound)) {
			//pounds to kilograms
			mCurrentValue = changePoundToKilogram(mCurrentValue);
			mCurrentUnit = MeasureWeight.MeasureWeightKilogram;
		}
	}

	@Override
	public void setValueInWheelControl(GxMeasuresControl linearLayout) {
		double value = mCurrentValue;
		if (mCurrentUnit.equalsIgnoreCase(MeasureWeight.MeasureWeightKilogram)) {
			linearLayout.setWheelControlViewAdapter(0, 199, 0, 9, GxMeasuresHelper.getNumericByDouble(value), GxMeasuresHelper.getDecimalByDouble(value));
		} else if (mCurrentUnit.equalsIgnoreCase(MeasureWeight.MeasureWeightPound)) {
			linearLayout.setWheelControlViewAdapter(0, 440, 0, 9, GxMeasuresHelper.getNumericByDouble(value), GxMeasuresHelper.getDecimalByDouble(value));
		} else {
			linearLayout.setWheelControlViewAdapter(0, 199, 0, 9, GxMeasuresHelper.getNumericByDouble(value), GxMeasuresHelper.getDecimalByDouble(value));
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
