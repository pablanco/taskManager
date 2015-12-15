package com.extensions.controls.sparkline;

import android.graphics.Color;

import com.artech.base.utils.Strings;

public class SparkLineOptions {

	private String _labelText = Strings.EMPTY;
	private int _labelColor = Color.DKGRAY;
	private boolean _showCurrentValue = true;
	private int _currentValueColor = Color.BLUE;
	private int _penColor = Color.BLACK;
	private float _penWidth = 1.0f;

	//! The text to be displayed beside the graph data.
	public String getLabelText() {
		return _labelText;
	}
	public void setLabelText(String labelText) {
		_labelText = labelText;
	}
	//! The colour of the label text (default: dark gray).
	public int getLabelColor() {
		return _labelColor;
	}
	public void setLabelColor(int labelColor) {
		_labelColor = labelColor;
	}

	//! Flag to enable display of the numerical current (last) value (default: YES).
	public boolean isShowCurrentValue() {
		return _showCurrentValue;
	}
	public void setShowCurrentValue(boolean showCurrentValue) {
		_showCurrentValue = showCurrentValue;
	}

	//! The Color used to display the numeric current value and the marker anchor.
	public int getCurrentValueColor() {
		return _currentValueColor;
	}
	public void setCurrentValueColor(int currentValueColor) {
		_currentValueColor = currentValueColor;
	}

	//! The Color used for the sparkline colour itself
	public int getPenColor() {
		return _penColor;
	}
	public void setPenColor(int penColor) {
		_penColor = penColor;
	}
	//! The float value used for the sparkline pen width
	public float getPenWidth() {
		return _penWidth;
	}
	public void setPenWidth(float penWidth) {
		_penWidth = penWidth;
	}
}
