package com.artech.base.metadata.expressions;

import java.math.BigDecimal;

import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;

class ConstantNumberExpression extends ConstantExpression
{
	static final String TYPE = "number";

	public ConstantNumberExpression(INodeObject node)
	{
		super(node);
	}

	@Override
	public Value eval(IExpressionContext context)
	{
		String strValue = getConstant();
		try
		{
			BigDecimal value = new BigDecimal(strValue);
			return new Value(Type.DECIMAL, value);
		}
		catch (NumberFormatException e)
		{
			Services.Log.warning(String.format("Error parsing constant number expression '%s'.", strValue));
			return new Value(Type.DECIMAL, BigDecimal.ZERO);
		}
	}
}
