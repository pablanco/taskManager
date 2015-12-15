package com.extensions.controls.sparkline;

import java.util.ArrayList;
import java.util.Arrays;

public class SparkLineData extends ArrayList<Float> {

	private static final long serialVersionUID = 1L;
	private Float _minimum;
	private Float _maximum;
	private boolean _wasAnalized;

	public Float getMinimum() {
		ensureAnalysis();
		return _minimum;
	}

	public Float getMaximum() {
		ensureAnalysis();
		return _maximum;
	}

	private void ensureAnalysis() {
		if (this.size() > 0 && !_wasAnalized) {
			Float [] values = this.toArray(new Float[this.size()]);
			Arrays.sort(values);
			_minimum = values[0];
			_maximum = values[values.length - 1];
			_wasAnalized = true;
		}
	}

	public Float getCurrentValue() {
		return this.get(this.size() - 1);
	}

}
