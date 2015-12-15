package com.artech.controls.wheel.measures;

import org.json.JSONObject;

import com.artech.controls.wheel.R;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class GxMeasuresTemperatureHelper implements IGxMeasuresHelper {
	
	private String mUnit = MeasureTemperature.MeasureTemperatureCelsius;
	private String mCurrentUnit = MeasureTemperature.MeasureTemperatureCelsius;
	private double mValue = 0;
	private double mCurrentValue = 0;
	
	private static String mPrefixCelcius = "ºC"; //$NON-NLS-1$
	private static String mPrefixFahrenheit = "ºF"; //$NON-NLS-1$
	
	final class MeasureTemperature {
		public static final String MeasureTemperatureCelsius = "Celsius"; //$NON-NLS-1$
		public static final String MeasureTemperatureFahrenheit = "Fahrenheit"; //$NON-NLS-1$
	}
	
	private double changeCelsiusToFahrenheit (double Celcius) {
		return GxMeasuresHelper.round((Celcius * 9/5) + 32, 1).doubleValue();
	}
	
	private double changeFahrenheitToCelsius (double Fahrenheit) {
		return GxMeasuresHelper.round((Fahrenheit - 32) * 5/9, 1).doubleValue();
	}
	
	@Override
	public String getDisplayValue(double value) {
		return getDisplayValue(value, mUnit);
	}
	
	private String getDisplayValue(double value, String unit) {
		if (unit.equalsIgnoreCase(MeasureTemperature.MeasureTemperatureCelsius))
			return String.valueOf(value).concat(Strings.SPACE).concat(mPrefixCelcius);
		else if (unit.equalsIgnoreCase(MeasureTemperature.MeasureTemperatureFahrenheit))
			return String.valueOf(value).concat(Strings.SPACE).concat(mPrefixFahrenheit);
		return Strings.EMPTY;
	}

	// Get display value in the "Action" button
	@Override
	public String getCurrentStringValue(int currentItemNumeric, int currentItemDecimal) {
		mCurrentValue = getCurreentValue(currentItemNumeric, currentItemDecimal);
		return getDisplayValue(mCurrentValue, mCurrentUnit);
	}
	
	private double getCurreentValue(int currentItemNumeric, int currentItemDecimal) {
		return Double.parseDouble(String.valueOf(currentItemNumeric - 40).concat(Strings.DOT).concat(String.valueOf(currentItemDecimal)));
	}

	@Override
	public String getGx_Value(String valueKey, String unitKey, String convertedValueKey) {
		double value = mValue;
		JSONObject valueJSONObject = new JSONObject();
		try {
			valueJSONObject.put(valueKey, value);
			valueJSONObject.put(convertedValueKey, value);
			if (mUnit.equalsIgnoreCase(MeasureTemperature.MeasureTemperatureCelsius)) {
				valueJSONObject.put(unitKey, mPrefixCelcius);
			} else if (mUnit.equalsIgnoreCase(MeasureTemperature.MeasureTemperatureFahrenheit)) {
				valueJSONObject.put(unitKey, mPrefixFahrenheit);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return valueJSONObject.toString();
	}

	@Override
	public String getTextButtonChange() {
		String toView = Strings.EMPTY;
		if (mCurrentUnit.equalsIgnoreCase(MeasureTemperature.MeasureTemperatureCelsius))
			toView = String.format(Services.Strings.getResource(R.string.GXM_ConvertTo), MeasureTemperature.MeasureTemperatureFahrenheit);
		else if (mCurrentUnit.equalsIgnoreCase(MeasureTemperature.MeasureTemperatureFahrenheit))
			toView = String.format(Services.Strings.getResource(R.string.GXM_ConvertTo), MeasureTemperature.MeasureTemperatureCelsius);
		return toView;
	}

	@Override
	public double getValueKey(double valueKey, String valueUnitKey) {
		setValueUnitKey(valueUnitKey);
		mValue =  valueKey;
		return mValue;
	}

	private void setValueUnitKey(String valueUnitKey) {
		if (valueUnitKey.equalsIgnoreCase(mPrefixCelcius)) {
			mUnit = MeasureTemperature.MeasureTemperatureCelsius;
		} else if (valueUnitKey.equalsIgnoreCase(mPrefixFahrenheit)) {
			mUnit = MeasureTemperature.MeasureTemperatureFahrenheit;
		} else {
			//the default value Celsius
			mUnit = MeasureTemperature.MeasureTemperatureCelsius;
		}
	}
	
	@Override
	public void changeValue(int currentItemNumeric, int currentItemDecimal) {
		mCurrentValue = getCurreentValue(currentItemNumeric, currentItemDecimal);
		if (mCurrentUnit.equalsIgnoreCase(MeasureTemperature.MeasureTemperatureCelsius)) {
			//Celsius to Fahrenheit
			mCurrentValue = changeCelsiusToFahrenheit(mCurrentValue);
			mCurrentUnit = MeasureTemperature.MeasureTemperatureFahrenheit;
		} else if (mCurrentUnit.equalsIgnoreCase(MeasureTemperature.MeasureTemperatureFahrenheit)) {
			//Fahrenheit to Celsius
			mCurrentValue = changeFahrenheitToCelsius(mCurrentValue);
			mCurrentUnit = MeasureTemperature.MeasureTemperatureCelsius;
		}
	}

	@Override
	public void setValueInWheelControl(GxMeasuresControl linearLayout) {
		double value = mCurrentValue;
		if (mCurrentUnit.equalsIgnoreCase(MeasureTemperature.MeasureTemperatureCelsius)) {
			linearLayout.setWheelControlViewAdapter(-40, 199, 0, 9, GxMeasuresHelper.getNumericByDouble(value) + 40, GxMeasuresHelper.getDecimalByDouble(value));
		} else if (mCurrentUnit.equalsIgnoreCase(MeasureTemperature.MeasureTemperatureFahrenheit)) {
			linearLayout.setWheelControlViewAdapter(-40, 391, 0, 9, GxMeasuresHelper.getNumericByDouble(value) + 40, GxMeasuresHelper.getDecimalByDouble(value));
		} else {
			linearLayout.setWheelControlViewAdapter(-40, 199, 0, 9, GxMeasuresHelper.getNumericByDouble(value) + 40, GxMeasuresHelper.getDecimalByDouble(value));
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
