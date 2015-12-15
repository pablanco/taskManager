package com.artech.base.metadata.expressions;

import com.artech.base.serialization.INodeObject;

class VariableExpression extends ValueExpression
{
	static final String TYPE = "variable";

	public VariableExpression(INodeObject node)
	{
		super(node);
	}
}
