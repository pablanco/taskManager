package com.artech.base.metadata.expressions;

import com.artech.base.metadata.expressions.Expression.Type;
import com.artech.base.metadata.expressions.Expression.Value;
import com.artech.common.ExecutionContext;
import com.artech.controls.IGxControl;

public interface IExpressionContext
{
	Value getValue(String name, Type expectedType);
	IGxControl getControl(String name);
	ExecutionContext getExecutionContext();
}
