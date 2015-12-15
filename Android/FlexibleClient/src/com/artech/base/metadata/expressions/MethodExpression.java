package com.artech.base.metadata.expressions;

import java.util.List;

import com.artech.base.serialization.INodeObject;

class MethodExpression extends Expression implements ITargetedExpression
{
	static final String TYPE = "method";

	private final Expression mTarget;
	private final String mMethod;
	private final List<Expression> mParameters;

	private static final String METHOD_TO_STRING = "ToString";
	private static final String METHOD_TO_FORMATTED_STRING = "ToFormattedString";

	public MethodExpression(INodeObject node)
	{
		mTarget = ExpressionFactory.parse(node.getNode("target"));
		mMethod = node.getString("@methName");
		mParameters = ExpressionFactory.parseParameters(node);
	}

	@Override
	public String toString()
	{
		return String.format("%s.%s(%s)", mTarget, mMethod, mParameters);
	}

	@Override
	public Value eval(IExpressionContext context)
	{
		Value target = mTarget.eval(context);

		// Special cases that are not mapped to actual methods.
		if (target.getType() == Type.ENTITY_COLLECTION)
		{
			if (mMethod.equalsIgnoreCase("Item"))
			{
				int itemPosition = mParameters.get(0).eval(context).coerceToInt();
				return Value.newEntity(target.coerceToEntityCollection().get(itemPosition - 1));
			}
		}
		else if (METHOD_TO_STRING.equalsIgnoreCase(mMethod) && target.getType().isNumeric())
		{
			return Value.newString(ExpressionFormatHelper.toString(target));
		}
		else if (METHOD_TO_FORMATTED_STRING.equalsIgnoreCase(mMethod) && mParameters.size() == 0)
		{
			return Value.newString(ExpressionFormatHelper.toFormattedString(target));
		}

		// Generic methods
		List<Value> methodParameters = ExpressionHelper.evalExpressions(mParameters, context);
		return MethodHelper.call(target, mMethod, methodParameters);
	}

	@Override
	public Expression getTarget()
	{
		return mTarget;
	}
}
