package com.artech.base.metadata.expressions;

import java.util.UUID;

import com.artech.base.serialization.INodeObject;
import com.artech.utils.Cast;
import com.genexus.GXutil;

class BooleanExpression extends BinaryExpression
{
	static final String TYPE = "logic";

	private static final String OPERATOR_AND = "and";
	private static final String OPERATOR_OR = "or";
	private static final String OPERATOR_NOT = "not";
	private static final String OPERATOR_EQUAL = "=";
	private static final String OPERATOR_DIFFERENT = "<>";
	private static final String OPERATOR_LESS = "<";
	private static final String OPERATOR_LESS_OR_EQUAL = "<=";
	private static final String OPERATOR_GREATER_OR_EQUAL = ">=";
	private static final String OPERATOR_GREATER = ">";

	public BooleanExpression(INodeObject node)
	{
		super(node);
	}

	private static final Value TRUE = new Value(Type.BOOLEAN, true);
	private static final Value FALSE = new Value(Type.BOOLEAN, false);

	@Override
	public Value eval(IExpressionContext context)
	{
		String operator = getOperator();

		// Operators with logic short-circuiting.
		if (OPERATOR_AND.equalsIgnoreCase(operator))
		{
			if (!getLeft().eval(context).coerceToBoolean())
				return FALSE;
			else
				return new Value(Type.BOOLEAN, getRight().eval(context).coerceToBoolean());
		}
		else if (OPERATOR_OR.equalsIgnoreCase(operator))
		{
			if (getLeft().eval(context).coerceToBoolean())
				return TRUE;
			else
				return new Value(Type.BOOLEAN, getRight().eval(context).coerceToBoolean());
		}

		// Not operator is a special case, it only has a right side.
		if (OPERATOR_NOT.equalsIgnoreCase(operator))
		{
			boolean right = getRight().eval(context).coerceToBoolean();
			return (right ? FALSE : TRUE);
		}

		// Comparison operators.
		int compareResult = compareValues(getLeft().eval(context), getRight().eval(context));
		if (OPERATOR_EQUAL.equalsIgnoreCase(operator))
		{
			return (compareResult == 0 ? TRUE : FALSE); // (leftComparable.equals(right) ? TRUE : FALSE);
		}
		else if (OPERATOR_DIFFERENT.equalsIgnoreCase(operator))
		{
			return (compareResult != 0 ? TRUE : FALSE); // (leftComparable.equals(right) ? FALSE : TRUE);
		}
		else if (OPERATOR_LESS.equalsIgnoreCase(operator))
		{
			return (compareResult < 0 ? TRUE : FALSE);
		}
		else if (OPERATOR_LESS_OR_EQUAL.equalsIgnoreCase(operator))
		{
			return (compareResult <= 0 ? TRUE : FALSE);
		}
		else if (OPERATOR_GREATER_OR_EQUAL.equalsIgnoreCase(operator))
		{
			return (compareResult >= 0 ? TRUE : FALSE);
		}
		else if (OPERATOR_GREATER.equalsIgnoreCase(operator))
		{
			return (compareResult > 0 ? TRUE : FALSE);
		}
		else
			throw new IllegalArgumentException(String.format("Unknown boolean operator: '%s'.", operator));
	}

	private int compareValues(Expression.Value left, Expression.Value right)
	{
		Object leftValue = left.getValue();
		Object rightValue = right.getValue();

		// Special case: GUID comparison does not use its Java implementation,
		// because it doesn't match GX (or what one would expect, really).
		// See http://anuff.com/2011/04/javautiluuidcompareto-considered-harmful/
		if ((leftValue instanceof UUID) && (rightValue instanceof UUID))
			return GXutil.guidCompare((UUID)leftValue, (UUID)rightValue, 0);

		@SuppressWarnings("rawtypes")
		Comparable leftComparable = Cast.as(Comparable.class, leftValue);
		if (leftComparable == null)
			throw new IllegalArgumentException(String.format("Trying to evaluate comparison expression '%s', but left side could not be evaluated into a Comparable instance.", toString()));

		@SuppressWarnings("unchecked")
		int compareResult = leftComparable.compareTo(rightValue);
		return compareResult;
	}
}
