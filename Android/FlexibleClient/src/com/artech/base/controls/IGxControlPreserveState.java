package com.artech.base.controls;

import java.util.Map;

public interface IGxControlPreserveState
{
	String getControlId();
	void saveState(Map<String, Object> state);
	void restoreState(Map<String, Object> state);
}
