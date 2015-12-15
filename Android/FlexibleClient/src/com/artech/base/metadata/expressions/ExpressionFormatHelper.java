package com.artech.base.metadata.expressions;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import com.artech.base.metadata.expressions.Expression.Type;
import com.artech.base.metadata.expressions.Expression.Value;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.genexus.GXutil;

public class ExpressionFormatHelper
{
	static String toString(Value value)
	{
		Type type = value.getType();

		// &Numeric.ToString() is implemented as a special case here.
		// For all other types, the mapped functions are used instead.
		if (type.isNumeric())
		{
			int length, decimals;
			BigDecimal number = value.coerceToNumber();

			if (value.getDefinition() != null && value.getDefinition().getBaseType() != null)
			{
				length = value.getDefinition().getBaseType().getLength();
				decimals = value.getDefinition().getBaseType().getDecimals();
			}
			else
			{
				// If we don't have a definition, use as many digits as necessary to fit the number.
				length = number.precision() + 1; // +1 for the decimal dot.
				decimals = number.scale();
			}

			return GXutil.str(number, length, decimals);
		}

		throw new IllegalArgumentException(String.format("Unexpected type for ToString in ExpressionFormatHelper (%s).",type));
	}

	static String toFormattedString(Value value)
	{
		Type type = value.getType();

		if (type == Type.STRING)
			return value.coerceToString();

		if (type.isNumeric())
			return formatNumber(value.coerceToNumber(), value.getPicture());

		if (type.isDateTime())
			return GXutilPlus.getLocalUtil().format(value.coerceToDate(), value.getPicture());

		if (type == Type.BOOLEAN)
			return GXutil.booltostr(value.coerceToBoolean());

		if (type == Type.GUID)
			return value.coerceToString();

		throw new IllegalArgumentException(String.format("Unexpected type for ToFormattedString (%s).",type));
	}

	static String format(String formatString, List<Value> parameters)
	{
		String[] params = new String[9];
		Arrays.fill(params, Strings.EMPTY);
		for (int i = 0; i < parameters.size(); i++)
			params[i] = toFormattedString(parameters.get(i));

		return GXutil.format(formatString, params[0], params[1], params[2], params[3], params[4], params[5], params[6], params[7], params[8]);
	}

	public static String formatNumber(BigDecimal value, String picture)
	{
		return GXutilPlus.getLocalUtil().format(value, picture);
	}

	static String getDefaultPicture(Value value)
	{
		if (value.getType().isNumeric())
		{
			BigDecimal number = value.coerceToNumber().stripTrailingZeros();

			int length = number.precision();
			int decimals = number.scale();
			if (decimals != 0)
			{
				// Decimal picture. Z*9.9+
				int intLength = length - decimals;
				return Services.Strings.repeat("Z", intLength - 1) + "9" + "." + Services.Strings.repeat("9", decimals);
			}
			else
			{
				// Integer picture, Z*9
				return Services.Strings.repeat("Z", length - 1) + "9";
			}
		}
		else if (value.getType() == Type.DATE)
		{
			return "99/99/99";
		}
		else if (value.getType() == Type.DATETIME)
		{
			return "99/99/99 99:99";
		}
		else if (value.getType() == Type.TIME)
		{
			return "99:99";
		}

		throw new IllegalArgumentException(String.format("Cannot generate a default picture for value '%s'.", value));
	}
}
