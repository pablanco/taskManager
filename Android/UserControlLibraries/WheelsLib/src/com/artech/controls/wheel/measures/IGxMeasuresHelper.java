package com.artech.controls.wheel.measures;


public interface IGxMeasuresHelper {
	
	String getDisplayValue(double value);
	
	String getCurrentStringValue(int currentItemNumeric, int currentItemDecimal);
	
	String getGx_Value(String valueKey, String unitKey, String convertedValueKey);

	String getTextButtonChange();
	
	double getValueKey(double valueKey, String valueUnitKey);
	
	void changeValue(int currentItemNumeric, int currentItemDecimal);
	
	void setValueInWheelControl(GxMeasuresControl linearLayout);
	
	void OkClickListener();
	
	void ActionClickListener();
}
