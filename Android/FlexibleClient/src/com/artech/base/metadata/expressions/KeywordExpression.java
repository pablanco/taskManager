package com.artech.base.metadata.expressions;

import com.artech.base.serialization.INodeObject;

class KeywordExpression extends ConstantExpression
{
	static final String TYPE = "keyword";

	public KeywordExpression(INodeObject node)
	{
		super(node);
	}

	@Override
	public Value eval(IExpressionContext context)
	{
		return Value.newString(getConstant());
	}
}
