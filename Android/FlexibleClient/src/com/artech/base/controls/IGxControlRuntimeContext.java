package com.artech.base.controls;

import com.artech.common.ExecutionContext;

/**
 * Interface for user controls that support runtime properties, methods, and events,
 * and need an execution context for each one.
 * @author matiash
 *
 */
public interface IGxControlRuntimeContext extends IGxControlRuntime
{
	void setExecutionContext(ExecutionContext context);
}
