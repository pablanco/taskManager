package com.artech.base.metadata.expressions;

import com.artech.base.serialization.INodeObject;

abstract class ValueExpression extends Expression
{
	private final String mName;
	private final Type mType;

	public ValueExpression(INodeObject node)
	{
		mName = node.getString("@exprValue");
		mType = ExpressionFactory.parseGxDataType(node.optString("@exprDataType"));
	}

	public String getName()
	{
		return mName;
	}

	@Override
	public String toString()
	{
		return mName;
	}

	@Override
	public Value eval(IExpressionContext context)
	{
		// Special keywords, but they arrive as values.
		if (mName.equalsIgnoreCase("nowait") || mName.equalsIgnoreCase("status") || mName.equalsIgnoreCase("keep")) //$NON-NLS-1$ //$NON-NLS-2$
			return Value.newString(mName);

		return context.getValue(mName, mType);
	}
}
