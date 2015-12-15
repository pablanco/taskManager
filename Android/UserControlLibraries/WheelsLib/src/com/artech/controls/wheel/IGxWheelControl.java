package com.artech.controls.wheel;

public interface IGxWheelControl {

	String getDisplayInitialValue();

	void setViewAdapter(String mDisplayValue, GxWheelPicker mWheelControlNumeric,  GxWheelPicker mWheelControlDecimal);

	String getCurrentStringValue(GxWheelPicker mWheelControlNumeric, GxWheelPicker mWheelControlDecimal);

	String getGx_DisplayValue(String value);
	
}
