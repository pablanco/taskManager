package com.artech.base.controls;

import java.util.List;

/**
 * Interface for user controls that support runtime properties, methods, and events.
 * @author matiash
 */
public interface IGxControlRuntime
{
	void setProperty(String name, Object value);
	Object getProperty(String name);

	void runMethod(String name, List<Object> parameters);
}
