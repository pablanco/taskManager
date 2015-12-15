package com.artech.base.metadata.expressions;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.expressions.Expression.Type;
import com.artech.base.metadata.expressions.Expression.Value;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.services.Services;
import com.genexus.GXutil;

/**
 * Class to implement conversions from "Expression.Value" format to and from
 * "Entity Storage" format. This should be mostly removed when entities contain
 * values of the correct type.
 * @author matiash
 *
 */
public class ExpressionValueBridge
{
	public static Value convertEntityFormatToValue(Entity entity, String name, Type type)
	{
		// Obtain and convert value.
		Object entityValue = entity.getProperty(name);
		Value value = convertEntityFormatToValue(type, entityValue);

		// Obtain picture, if available.
		DataItem definition = entity.getPropertyDefinition(name);
		if (definition != null)
			value.setDefinition(definition);

		return value;
	}

	private static Value convertEntityFormatToValue(Type type, Object value)
	{
		if (type == Type.ENTITY || type == Type.ENTITY_COLLECTION)
		{
			// Fix type, entity/collection may not be correct.
			if (type == Type.ENTITY && value instanceof EntityList)
				type = Type.ENTITY_COLLECTION;
			else if (type == Type.ENTITY_COLLECTION && value instanceof Entity)
				type = Type.ENTITY;

			return new Value(type, value); // These are passed directly and can be null.
		}

		if (value == null)
			throw new IllegalArgumentException(String.format("Value of type '%s' should not be null.", type));

		String str = value.toString();

		if (type == Type.STRING)
		{
			return Value.newString(str);
		}
		else if (type == Type.INTEGER || type == Type.DECIMAL)
		{
			BigDecimal parsedValue = Services.Strings.tryParseDecimal(str);
			if (parsedValue == null)
				throw new IllegalArgumentException(String.format("Invalid %s value: '%s'.", type, str));

			return new Value(type, parsedValue);
		}
		else if (type == Type.BOOLEAN)
		{
			Boolean parsedValue = Services.Strings.tryParseBoolean(str);
			if (parsedValue == null)
				throw new IllegalArgumentException(String.format("Invalid %s value: '%s'.", type, str));

			return Value.newBoolean(parsedValue);
		}
		else if (type == Type.DATE || type == Type.DATETIME || type == Type.TIME)
		{
			if (str.length() != 0)
			{
				Date date = Services.Strings.getDateTime(str);
				if (date == null)
					date = GXutil.nullDate();

				return new Value(type, date);
			}
			else
				return new Value(type, GXutil.nullDate());
		}
		else if (type == Type.GUID)
		{
			return new Value(Type.GUID, UUID.fromString(str));
		}
		else
		{
			throw new IllegalArgumentException(String.format("Unsupported type for value expression '%s'.", type));
		}
	}

	public static Object convertValueToEntityFormat(Value value)
	{
		switch (value.getType())
		{
			case ENTITY :
			case ENTITY_COLLECTION :
			case STRING :
				return value.getValue();

			case INTEGER :
			case DECIMAL :
				return convertNumberToEntityFormat(value, null);

			case DATE :
				return Services.Strings.getDateStringForServer(value.coerceToDate());

			case DATETIME :
			case TIME :
				return Services.Strings.getDateTimeStringForServer(value.coerceToDate());

			case BOOLEAN :
				return value.coerceToBoolean().toString();

			case GUID :
				return value.coerceToString();

			default :
				throw new IllegalArgumentException(String.format("Unexpected value (%s) for converting to entity format.", value));
		}
	}

	private static Object convertNumberToEntityFormat(Value value, DataItem dataItem)
	{
		return value.coerceToNumber().stripTrailingZeros().toPlainString();

		/* Eventually we want to do this, but we don't have the correct variable definition for most parameters.
		if (dataItem != null && dataItem.getBaseType() != null &&
			DataTypes.numeric.equalsIgnoreCase(dataItem.getBaseType().getType()) &&
			dataItem.getBaseType().getDecimals() == 0)
		{
			// Force conversion of number with decimals to integer.
			return String.valueOf(value.coerceToInt());
		}
		else
			return value.coerceToNumber().stripTrailingZeros().toPlainString();
		*/
	}
}
