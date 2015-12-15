package com.artech.base.metadata.expressions;

import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;

class ConstantBooleanExpression extends ConstantExpression
{
	static final String TYPE = "boolean";

	public ConstantBooleanExpression(INodeObject node)
	{
		super(node);
	}

	@Override
	public Value eval(IExpressionContext context)
	{
		String strValue = getConstant();
		Boolean value = Services.Strings.tryParseBoolean(strValue);
		if (value != null)
		{
			return Value.newBoolean(value);
		}
		else
		{
			Services.Log.warning(String.format("Unexpected value parsing constant boolean expression '%s'.", strValue));
			return new Value(Type.BOOLEAN, false);
		}
	}

}
