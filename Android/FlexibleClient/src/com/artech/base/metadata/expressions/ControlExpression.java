package com.artech.base.metadata.expressions;

import com.artech.base.serialization.INodeObject;

class ControlExpression extends Expression
{
	static final String TYPE = "control";

	private final String mControlName;

	public ControlExpression(INodeObject node)
	{
		mControlName = node.getString("@exprValue");
	}

	@Override
	public String toString()
	{
		return mControlName;
	}

	@Override
	public Value eval(IExpressionContext context)
	{
		return new Value(Type.CONTROL, context.getControl(mControlName));
	}
}
