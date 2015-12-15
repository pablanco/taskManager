package com.artech.base.metadata.expressions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.artech.base.metadata.expressions.Expression.Type;
import com.artech.base.metadata.expressions.Expression.Value;
import com.genexus.DecimalUtil;

class ExpressionHelper
{
	public static List<Value> evalExpressions(List<Expression> expressions, IExpressionContext context)
	{
		ArrayList<Value> values = new ArrayList<Value>();

		for (Expression expression : expressions)
			values.add(expression.eval(context));

		return values;
	}

	/**
	 * Read as "a parameter of type <type>" can be passed (possibly with automatic conversion)
	 * a value of any of the returned types.
	 * E.g. a function that receives DECIMAL can be called with DECIMAL or INTEGER.
	 */
	static Type[] getCompatibleTypesForType(Type type)
	{
		switch (type)
		{
			case DECIMAL : return new Type[] { Type.DECIMAL, Type.INTEGER };
			case DATETIME : return new Type[] { Type.DATETIME, Type.DATE, Type.TIME };
			default : return new Type[] { type };
		}
	}

	static Class<?> typeToJavaClass(Type type)
	{
		switch (type)
		{
			case STRING : return String.class;
			case INTEGER : return int.class;
			case DECIMAL : return BigDecimal.class;
			case BOOLEAN : return boolean.class;
			case DATE : return Date.class;
			case DATETIME : return Date.class;
			case TIME : return Date.class;
			case GUID : return UUID.class;
			default : throw new IllegalArgumentException(String.format("No Java class known for type '%s'.", type));
		}
	}

	static Object valueToJavaObject(Value value, Type expectedType)
	{
		switch (expectedType)
		{
			case STRING : return value.coerceToString();
			case INTEGER : return value.coerceToInt();
			case DECIMAL : return value.coerceToNumber();
			case BOOLEAN : return value.coerceToBoolean();
			case DATE : return value.coerceToDate();
			case DATETIME : return value.coerceToDate();
			case TIME : return value.coerceToDate();
			case GUID : return value.coerceToGuid();
			default : throw new IllegalArgumentException(String.format("No Java class known for type '%s'.", value.getType()));
		}
	}

	static Value javaObjectToValue(Type type, Object obj)
	{
		switch (type)
		{
			case STRING : return Value.newString((String)obj);
			case INTEGER : return Value.newInteger(javaObjectToInteger(obj));
			case DECIMAL : return Value.newDecimal(javaObjectToDecimal(obj));
			case BOOLEAN : return Value.newBoolean((Boolean)obj);
			case DATE : return new Value(Type.DATE, obj);
			case DATETIME : return new Value(Type.DATETIME, obj);
			case TIME : return new Value(Type.TIME, obj);
			case GUID : return Value.newGuid((UUID)obj);
			default : throw new IllegalArgumentException(String.format("No Java class known for type '%s'.", type));
		}
	}

	private static long javaObjectToInteger(Object obj)
	{
		if (obj instanceof Long)
			return (Long)obj;
		else if (obj instanceof Integer)
			return (Integer)obj;
		else if (obj instanceof Short)
			return (Short)obj;
		else if (obj instanceof Byte)
			return (Byte)obj;
		else
			throw new IllegalArgumentException(String.format("Unexpected Java class for Integer type: '%s'.", obj.getClass().getName()));
	}

	private static BigDecimal javaObjectToDecimal(Object obj)
	{
		if (obj instanceof BigDecimal)
			return (BigDecimal)obj;
		if (obj instanceof Double)
			return DecimalUtil.doubleToDec((Double)obj);
		else
			throw new IllegalArgumentException(String.format("Unexpected Java class for Decimal type: '%s'.", obj.getClass().getName()));
	}
}
