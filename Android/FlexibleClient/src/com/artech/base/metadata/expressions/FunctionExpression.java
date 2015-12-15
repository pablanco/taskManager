package com.artech.base.metadata.expressions;

import java.math.BigDecimal;
import java.util.List;

import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;

class FunctionExpression extends Expression
{
	static final String TYPE = "function";

	private final String mFunction;
	private final List<Expression> mParameters;

	private static final String FUNCTION_IIF = "iif";
	private static final String FUNCTION_MOD = "mod";
	private static final String FUNCTION_GET_LANGUAGE = "GetLanguage";
	private static final String FUNCTION_GET_MESSAGE_TEXT = "GetMessageText";
	private static final String FUNCTION_FORMAT = "Format";
	private static final String FUNCTION_TO_FORMATTED_STRING = "ToFormattedString";

	public FunctionExpression(INodeObject node)
	{
		mFunction = node.getString("@funcName");
		mParameters = ExpressionFactory.parseParameters(node);
	}

	@Override
	public String toString()
	{
		return String.format("%s(%s)", mFunction, mParameters);
	}

	@Override
	public Value eval(IExpressionContext context)
	{
		// Special cases that are not mapped to actual functions.
		if (FUNCTION_IIF.equalsIgnoreCase(mFunction))
		{
			if (mParameters.get(0).eval(context).coerceToBoolean())
				return mParameters.get(1).eval(context);
			else
				return mParameters.get(2).eval(context);
		}
		else if (FUNCTION_MOD.equalsIgnoreCase(mFunction))
		{
			Value dividend = mParameters.get(0).eval(context);
			Value divisor = mParameters.get(1).eval(context);
			BigDecimal mod = dividend.coerceToNumber().remainder(divisor.coerceToNumber());
			return new Value(dividend.getType(), mod);
		}
		else if (FUNCTION_GET_LANGUAGE.equalsIgnoreCase(mFunction))
		{
			return new Value(Type.STRING, String.valueOf(Services.Resources.getCurrentLanguage()));
		}
		else if (FUNCTION_GET_MESSAGE_TEXT.equalsIgnoreCase(mFunction))
		{
			// This function is a bit useless...
			String messageKey = mParameters.get(0).eval(context).coerceToString();
			if (mParameters.size() == 1)
				return new Value(Type.STRING, Services.Resources.getTranslation(messageKey));
			else if (mParameters.size() == 2)
				return new Value(Type.STRING, Services.Resources.getTranslation(messageKey, mParameters.get(1).eval(context).coerceToString()));
		}
		else if (FUNCTION_FORMAT.equalsIgnoreCase(mFunction) && mParameters.size() != 0)
		{
			List<Value> formatParameters = ExpressionHelper.evalExpressions(mParameters, context);

			// First parameter is format string, rest are values to embed in it.
			String formatString = formatParameters.get(0).coerceToString();
			formatParameters.remove(0);

			return Value.newString(ExpressionFormatHelper.format(formatString, formatParameters));
		}
		else if (FUNCTION_TO_FORMATTED_STRING.equalsIgnoreCase(mFunction) && mParameters.size() == 1)
		{
			Value value = ExpressionHelper.evalExpressions(mParameters, context).get(0);
			return Value.newString(ExpressionFormatHelper.toFormattedString(value));
		}

		// Generic functions.
		List<Value> functionParameters = ExpressionHelper.evalExpressions(mParameters, context);
		return FunctionHelper.call(mFunction, functionParameters);
	}
}
