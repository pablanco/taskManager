package com.artech.base.metadata.expressions;

import com.artech.base.serialization.INodeObject;

abstract class ConstantExpression extends Expression
{
	private final String mConstant;

	public ConstantExpression(INodeObject node)
	{
		mConstant = node.getString("@exprValue");
	}

	@Override
	public String toString()
	{
		return mConstant;
	}

	protected String getConstant()
	{
		return mConstant;
	}
}
